package com.nervesparks.axia.class PersonalityManager(
    private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // Personality state
    private val _activePersonality = MutableStateFlow<PersonalityMode>(PersonalityMode.NOMAD)
    val activePersonality: StateFlow<PersonalityMode> = _activePersonality.asStateFlow()
    
    private val _isNSFWModeEnabled = MutableStateFlow(false)
    val isNSFWModeEnabled: StateFlow<Boolean> = _isNSFWModeEnabled.asStateFlow()
    
    private val _sessionState = MutableStateFlow<SessionState>(SessionState.SAFE_MODE)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()
    
    // Security components
    private val biometricManager = BiometricManager.from(context)
    private val encryptionManager = EncryptionManager(context)
    private val contentClassifier = ContentClassifier()
    private val privacyVault = NSFWPrivacyVault(context)
    
    // Role playing system for enhanced NSFW interactions
    private val rolePlayingSystem = RolePlayingSystem(context)android.content.Context
import android.hardware.biometrics.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import android.util.Log

/**
 * Personality Manager for αξία (Axia.OS)
 * 
 * Handles dual personality modes:
 * - Nomad: Safe-for-work core personality (default)
 * - Monique: Adult-only personality (biometric protected)
 * 
 * Features:
 * - Biometric authentication for NSFW mode
 * - Encrypted personality state storage
 * - Age verification and content filtering
 * - Secure session management
 */
class PersonalityManager(
    private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // Personality state
    private val _activePersonality = MutableStateFlow<PersonalityMode>(PersonalityMode.NOMAD)
    val activePersonality: StateFlow<PersonalityMode> = _activePersonality.asStateFlow()
    
    private val _isNSFWModeEnabled = MutableStateFlow(false)
    val isNSFWModeEnabled: StateFlow<Boolean> = _isNSFWModeEnabled.asStateFlow()
    
    private val _sessionState = MutableStateFlow<SessionState>(SessionState.SAFE_MODE)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()
    
    // Security components
    private val biometricManager = BiometricManager.from(context)
    private val encryptionManager = EncryptionManager(context)
    private val contentClassifier = ContentClassifier()
    private val privacyVault = NSFWPrivacyVault(context)
    
    // Session management
    private var sessionTimer: Job? = null
    private var panicModeEnabled = false
    
    init {
        initializePersonalities()
        setupSecurityGates()
    }
    
    /**
     * Initialize personality modes with their configurations
     */
    private fun initializePersonalities() {
        // Default to Nomad (SFW) mode
        _activePersonality.value = PersonalityMode.NOMAD
        _isNSFWModeEnabled.value = false
        _sessionState.value = SessionState.SAFE_MODE
    }
    
    /**
     * Setup security gates and content filters
     */
    private fun setupSecurityGates() {
        // Verify age compliance on startup
        verifyAgeCompliance()
        
        // Initialize content filtering
        contentClassifier.initialize()
        
        // Setup panic mode detection
        setupPanicModeDetection()
    }
    
    /**
     * Request personality switch with biometric authentication
     */
    suspend fun requestPersonalitySwitch(
        targetPersonality: PersonalityMode,
        activity: FragmentActivity
    ): PersonalitySwitchResult {
        return withContext(Dispatchers.Main) {
            when (targetPersonality) {
                PersonalityMode.NOMAD -> {
                    // Switch to Nomad (SFW) - no authentication required
                    switchToNomad()
                    PersonalitySwitchResult.Success(PersonalityMode.NOMAD)
                }
                PersonalityMode.MONIQUE -> {
                    // Switch to Monique (NSFW) - requires biometric authentication
                    authenticateForNSFWMode(activity)
                }
            }
        }
    }
    
    /**
     * Authenticate user for NSFW mode using biometrics
     */
    private suspend fun authenticateForNSFWMode(activity: FragmentActivity): PersonalitySwitchResult {
        return suspendCancellableCoroutine { continuation ->
            if (!isBiometricAuthAvailable()) {
                continuation.resume(PersonalitySwitchResult.BiometricNotAvailable)
                return@suspendCancellableCoroutine
            }
            
            if (!verifyAgeCompliance()) {
                continuation.resume(PersonalitySwitchResult.AgeVerificationFailed)
                return@suspendCancellableCoroutine
            }
            
            val biometricPrompt = BiometricPrompt(
                activity,
                ContextCompat.getMainExecutor(context),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        switchToMonique()
                        continuation.resume(PersonalitySwitchResult.Success(PersonalityMode.MONIQUE))
                    }
                    
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        continuation.resume(PersonalitySwitchResult.AuthenticationFailed(errString.toString()))
                    }
                    
                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        continuation.resume(PersonalitySwitchResult.AuthenticationFailed("Biometric authentication failed"))
                    }
                }
            )
            
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Adult Content Access")
                .setSubtitle("Verify your identity to access adult personality mode")
                .setDescription("This mode contains adult content and requires biometric authentication")
                .setNegativeButtonText("Cancel")
                .setConfirmationRequired(true)
                .build()
            
            biometricPrompt.authenticate(promptInfo)
        }
    }
    
    /**
     * Switch to Nomad (SFW) personality
     */
    private fun switchToNomad() {
        _activePersonality.value = PersonalityMode.NOMAD
        _isNSFWModeEnabled.value = false
        _sessionState.value = SessionState.SAFE_MODE
        
        // Clear any NSFW session data
        clearNSFWSessionData()
        
        Log.d("PersonalityManager", "Switched to Nomad (SFW) mode")
    }
    
    /**
     * Switch to Monique (NSFW) personality
     */
    private fun switchToMonique() {
        _activePersonality.value = PersonalityMode.MONIQUE
        _isNSFWModeEnabled.value = true
        _sessionState.value = SessionState.ADULT_MODE
        
        // Initialize NSFW privacy vault
        privacyVault.initialize()
        
        // Start session timer
        startSessionTimer()
        
        Log.d("PersonalityManager", "Switched to Monique (NSFW) mode")
    }
    
    /**
     * Classify content and auto-route to appropriate personality
     */
    suspend fun classifyAndRoute(content: String): ContentRoutingResult {
        val classification = contentClassifier.classifyContent(content)
        
        return when {
            classification.isNSFW && _activePersonality.value == PersonalityMode.NOMAD -> {
                // NSFW content detected in SFW mode
                ContentRoutingResult.RequiresPersonalitySwitch(
                    targetPersonality = PersonalityMode.MONIQUE,
                    reason = "Adult content detected",
                    confidence = classification.confidence
                )
            }
            classification.isBlocked -> {
                // Content violates safety policies
                ContentRoutingResult.Blocked(
                    reason = classification.blockReason ?: "Content violates safety policies"
                )
            }
            else -> {
                // Content is safe for current personality
                ContentRoutingResult.Allowed(classification)
            }
        }
    }

    /**
     * Process message with role-playing capabilities when in Monique mode
     */
    suspend fun processMessage(content: String, userId: String? = null): MessageProcessingResult {
        val routingResult = classifyAndRoute(content)
        
        return when (routingResult) {
            is ContentRoutingResult.Blocked -> {
                MessageProcessingResult.Blocked(routingResult.reason)
            }
            is ContentRoutingResult.RequiresPersonalitySwitch -> {
                MessageProcessingResult.RequiresAuthentication(routingResult.targetPersonality)
            }
            is ContentRoutingResult.Allowed -> {
                if (_activePersonality.value == PersonalityMode.MONIQUE && rolePlayingSystem != null) {
                    // Use role-playing system for enhanced responses
                    val response = rolePlayingSystem.processRolePlayMessage(content, userId)
                    MessageProcessingResult.Success(response, true)
                } else {
                    // Standard response processing
                    MessageProcessingResult.Success(content, false)
                }
            }
        }
    }

    /**
     * Process media message with body enhancement and role-playing
     */
    suspend fun processMediaMessage(
        mediaUri: String, 
        mediaType: String, 
        userId: String? = null
    ): MediaProcessingResult {
        if (_activePersonality.value != PersonalityMode.MONIQUE) {
            return MediaProcessingResult.RequiresAuthentication
        }

        return try {
            // Apply AI body enhancement if it's NSFW media
            val enhancedMediaUri = if (mediaType.contains("image") || mediaType.contains("video")) {
                aiBodyEnhancementTool?.enhanceImage(mediaUri) ?: mediaUri
            } else {
                mediaUri
            }

            // Process with role-playing system for contextual response
            val response = rolePlayingSystem?.handleMediaExchange(enhancedMediaUri, mediaType, userId)
                ?: "Media received and processed."

            MediaProcessingResult.Success(enhancedMediaUri, response)
        } catch (e: Exception) {
            MediaProcessingResult.Error("Failed to process media: ${e.message}")
        }
    }
    
    /**
     * Emergency panic lock
     */
    fun activatePanicLock() {
        panicModeEnabled = true
        
        // Immediately switch to safe mode
        switchToNomad()
        
        // Clear all sensitive data
        clearAllSensitiveData()
        
        // Enable decoy mode
        _sessionState.value = SessionState.DECOY_MODE
        
        Log.w("PersonalityManager", "Panic lock activated")
    }
    
    /**
     * End current session and secure wipe
     */
    suspend fun endSession() {
        withContext(Dispatchers.IO) {
            // Cancel session timer
            sessionTimer?.cancel()
            
            // Clear session data based on current mode
            if (_isNSFWModeEnabled.value) {
                privacyVault.secureWipe()
            }
            
            clearNSFWSessionData()
            
            // Reset to safe mode
            switchToNomad()
            
            Log.d("PersonalityManager", "Session ended and data wiped")
        }
    }
    
    /**
     * Get current personality configuration
     */
    fun getCurrentPersonalityConfig(): PersonalityConfig {
        return when (_activePersonality.value) {
            PersonalityMode.NOMAD -> getNomadConfig()
            PersonalityMode.MONIQUE -> getMoniqueConfig()
        }
    }
    
    // Private helper methods
    
    private fun isBiometricAuthAvailable(): Boolean {
        return biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }
    
    private fun verifyAgeCompliance(): Boolean {
        // This should integrate with proper age verification
        // For now, we'll use a placeholder check
        val preferences = context.getSharedPreferences("axia_age_verification", Context.MODE_PRIVATE)
        return preferences.getBoolean("age_verified_18_plus", false)
    }
    
    private fun startSessionTimer() {
        sessionTimer?.cancel()
        sessionTimer = scope.launch {
            delay(30 * 60 * 1000) // 30 minutes timeout
            endSession()
        }
    }
    
    private fun clearNSFWSessionData() {
        // Clear temporary NSFW data
        scope.launch {
            privacyVault.clearTemporaryData()
        }
    }
    
    private fun clearAllSensitiveData() {
        scope.launch {
            privacyVault.emergencyWipe()
            encryptionManager.clearAllKeys()
        }
    }
    
    private fun setupPanicModeDetection() {
        // This would integrate with system events for panic detection
        // For example: rapid app switching, emergency contacts, etc.
    }
    
    private fun getNomadConfig(): PersonalityConfig {
        return PersonalityConfig(
            name = "Nomad",
            description = "Helpful AI assistant for everyday tasks",
            personality = PersonalityTraits(
                helpfulness = 0.9f,
                creativity = 0.7f,
                formality = 0.6f,
                humor = 0.5f,
                empathy = 0.8f
            ),
            capabilities = listOf(
                "General assistance",
                "Information lookup",
                "Task automation",
                "Creative writing",
                "Code assistance",
                "Learning support"
            ),
            restrictions = listOf(
                "No adult content",
                "No explicit material",
                "Family-friendly responses only"
            ),
            responseStyle = ResponseStyle.HELPFUL_PROFESSIONAL,
            contentFilter = ContentFilterLevel.STRICT
        )
    }
    
    private fun getMoniqueConfig(): PersonalityConfig {
        return PersonalityConfig(
            name = "Monique",
            description = "Adult-oriented AI companion for mature audiences",
            personality = PersonalityTraits(
                helpfulness = 0.8f,
                creativity = 0.9f,
                formality = 0.3f,
                humor = 0.8f,
                empathy = 0.9f
            ),
            capabilities = listOf(
                "Adult conversation",
                "Mature content creation",
                "Relationship advice",
                "Creative adult content",
                "Private assistance",
                "Personal companionship"
            ),
            restrictions = listOf(
                "18+ content only",
                "No minors involved",
                "Consent-based interactions",
                "Legal compliance required"
            ),
            responseStyle = ResponseStyle.INTIMATE_CASUAL,
            contentFilter = ContentFilterLevel.ADULT_PERMISSIVE
        )
    }
}

// Data classes and enums

enum class PersonalityMode {
    NOMAD,      // Safe-for-work core personality
    MONIQUE     // Adult-only personality
}

enum class SessionState {
    SAFE_MODE,      // Normal SFW operation
    ADULT_MODE,     // NSFW mode active
    DECOY_MODE      // Panic mode - show decoy interface
}

sealed class PersonalitySwitchResult {
    data class Success(val personality: PersonalityMode) : PersonalitySwitchResult()
    object BiometricNotAvailable : PersonalitySwitchResult()
    object AgeVerificationFailed : PersonalitySwitchResult()
    data class AuthenticationFailed(val reason: String) : PersonalitySwitchResult()
}

sealed class ContentRoutingResult {
    data class Allowed(val classification: ContentClassification) : ContentRoutingResult()
    data class RequiresPersonalitySwitch(
        val targetPersonality: PersonalityMode,
        val reason: String,
        val confidence: Float
    ) : ContentRoutingResult()
    data class Blocked(val reason: String) : ContentRoutingResult()
}

data class PersonalityConfig(
    val name: String,
    val description: String,
    val personality: PersonalityTraits,
    val capabilities: List<String>,
    val restrictions: List<String>,
    val responseStyle: ResponseStyle,
    val contentFilter: ContentFilterLevel
)

data class PersonalityTraits(
    val helpfulness: Float,      // 0.0 - 1.0
    val creativity: Float,       // 0.0 - 1.0
    val formality: Float,        // 0.0 - 1.0
    val humor: Float,           // 0.0 - 1.0
    val empathy: Float          // 0.0 - 1.0
)

enum class ResponseStyle {
    HELPFUL_PROFESSIONAL,
    CASUAL_FRIENDLY,
    INTIMATE_CASUAL,
    FORMAL_BUSINESS
}

enum class ContentFilterLevel {
    STRICT,             // Family-friendly only
    MODERATE,           // Some mature themes allowed
    ADULT_PERMISSIVE    // Adult content allowed
}

data class ContentClassification(
    val isNSFW: Boolean,
    val isSafe: Boolean,
    val isBlocked: Boolean,
    val confidence: Float,
    val categories: List<String>,
    val blockReason: String? = null
)

/**
 * Result of message processing with role-playing capabilities
 */
sealed class MessageProcessingResult {
    data class Success(val response: String, val usedRolePlay: Boolean) : MessageProcessingResult()
    data class Blocked(val reason: String) : MessageProcessingResult()
    data class RequiresAuthentication(val targetPersonality: PersonalityMode) : MessageProcessingResult()
    data class Error(val message: String) : MessageProcessingResult()
}

/**
 * Result of media processing with enhancement and role-playing
 */
sealed class MediaProcessingResult {
    data class Success(val enhancedMediaUri: String, val response: String) : MediaProcessingResult()
    object RequiresAuthentication : MediaProcessingResult()
    data class Error(val message: String) : MediaProcessingResult()
}