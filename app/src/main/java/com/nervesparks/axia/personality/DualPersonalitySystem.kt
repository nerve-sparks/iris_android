package com.nervesparks.axia.personality

import android.content.Context
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import android.util.Log
import com.nervesparks.axia.optimization.IntelligentHardwareOptimizer

/**
 * Dual Personality System Integration
 * 
 * This class integrates all security components for the Nomad/Monique
 * dual personality system with comprehensive NSFW protection.
 * 
 * Security Features:
 * 1. Biometric authentication for NSFW access
 * 2. Content classification and routing  
 * 3. Encrypted storage with secure deletion
 * 4. Photoreal media generation with local-first approach
 * 5. Network policy enforcement with E2E encryption
 * 6. Panic lock with emergency wipe
 * 7. Decoy mode for stealth operation
 * 8. Hardware-backed encryption
 * 9. Session management with timeouts
 * 10. Offline-only NSFW mode option
 */
class DualPersonalitySystem(
    private val context: Context,
    private val hardwareOptimizer: IntelligentHardwareOptimizer
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // Core components
    private val encryptionManager = EncryptionManager(context)
    private val privacyVault = NSFWPrivacyVault(context, encryptionManager)
    private val contentClassifier = ContentClassifier()
    private val networkController = NetworkPolicyController(context)
    private val mediaGenerator = PhotorealMediaGenerator(context, hardwareOptimizer, privacyVault)
    private val sessionManager = SessionManager(context, privacyVault, encryptionManager, networkController)
    private val personalityManager = PersonalityManager(
        context, contentClassifier, privacyVault, encryptionManager
    )
    
    // System state
    private val _systemState = MutableStateFlow(SystemState())
    val systemState: StateFlow<SystemState> = _systemState.asStateFlow()
    
    private val _currentPersonality = MutableStateFlow(PersonalityMode.NOMAD)
    val currentPersonality: StateFlow<PersonalityMode> = _currentPersonality.asStateFlow()
    
    init {
        initializeDualPersonalitySystem()
    }
    
    /**
     * Initialize the dual personality system
     */
    private fun initializeDualPersonalitySystem() {
        scope.launch {
            try {
                Log.d("DualPersonalitySystem", "Initializing dual personality system...")
                
                // Initialize all components
                val initResults = awaitAll(
                    async { encryptionManager.initialize() },
                    async { privacyVault.initialize() },
                    async { networkController.setNetworkPolicyMode(NetworkPolicyMode.ALLOW_ALL) }
                )
                
                // Check initialization results
                val allInitialized = initResults.all { it }
                
                if (allInitialized) {
                    _systemState.value = _systemState.value.copy(
                        isInitialized = true,
                        initializationTime = System.currentTimeMillis()
                    )
                    
                    Log.d("DualPersonalitySystem", "Dual personality system initialized successfully")
                    
                    // Start monitoring system health
                    startSystemHealthMonitoring()
                    
                } else {
                    Log.e("DualPersonalitySystem", "Failed to initialize some components")
                    _systemState.value = _systemState.value.copy(
                        hasError = true,
                        errorMessage = "Component initialization failed"
                    )
                }
                
            } catch (e: Exception) {
                Log.e("DualPersonalitySystem", "Failed to initialize dual personality system", e)
                _systemState.value = _systemState.value.copy(
                    hasError = true,
                    errorMessage = "System initialization failed: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Request to switch to Monique (NSFW) personality
     * Requires biometric authentication and activates all security measures
     */
    suspend fun switchToMoniquePersonality(
        activity: FragmentActivity,
        callback: PersonalitySwitchCallback
    ) {
        try {
            Log.d("DualPersonalitySystem", "Requesting switch to Monique personality")
            
            // Ensure system is initialized
            if (!_systemState.value.isInitialized) {
                callback.onError("System not initialized")
                return
            }
            
            // Start NSFW session with biometric authentication
            sessionManager.startNSFWSession(activity, object : SessionStartCallback {
                override fun onSuccess() {
                    scope.launch {
                        try {
                            // Activate NSFW security measures
                            activateNSFWSecurityMode()
                            
                            // Switch personality
                            _currentPersonality.value = PersonalityMode.MONIQUE
                            
                            // Update system state
                            _systemState.value = _systemState.value.copy(
                                isMoniqueActive = true,
                                moniqueActivationTime = System.currentTimeMillis(),
                                securityLevel = SecurityLevel.MAXIMUM
                            )
                            
                            Log.d("DualPersonalitySystem", "Successfully switched to Monique personality")
                            callback.onSuccess(PersonalityMode.MONIQUE)
                            
                        } catch (e: Exception) {
                            Log.e("DualPersonalitySystem", "Failed to complete personality switch", e)
                            callback.onError("Personality switch failed: ${e.message}")
                        }
                    }
                }
                
                override fun onError(message: String) {
                    Log.e("DualPersonalitySystem", "NSFW session start failed: $message")
                    callback.onError("Authentication failed: $message")
                }
            })
            
        } catch (e: Exception) {
            Log.e("DualPersonalitySystem", "Failed to switch to Monique personality", e)
            callback.onError("Personality switch failed: ${e.message}")
        }
    }
    
    /**
     * Switch back to Nomad (SFW) personality
     */
    suspend fun switchToNomadPersonality(wipeTemporaryData: Boolean = true) {
        try {
            Log.d("DualPersonalitySystem", "Switching to Nomad personality")
            
            // End NSFW session
            sessionManager.endNSFWSession(wipeTemporaryData)
            
            // Deactivate NSFW security measures
            deactivateNSFWSecurityMode()
            
            // Switch personality
            _currentPersonality.value = PersonalityMode.NOMAD
            
            // Update system state
            _systemState.value = _systemState.value.copy(
                isMoniqueActive = false,
                nomadActivationTime = System.currentTimeMillis(),
                securityLevel = SecurityLevel.STANDARD
            )
            
            Log.d("DualPersonalitySystem", "Successfully switched to Nomad personality")
            
        } catch (e: Exception) {
            Log.e("DualPersonalitySystem", "Failed to switch to Nomad personality", e)
        }
    }
    
    /**
     * Process user message through the dual personality system
     */
    suspend fun processMessage(
        message: String,
        messageType: MessageType = MessageType.TEXT
    ): PersonalityResponse {
        return try {
            // Route through personality manager
            personalityManager.processMessage(message, messageType)
            
        } catch (e: Exception) {
            Log.e("DualPersonalitySystem", "Failed to process message", e)
            PersonalityResponse.Error("Message processing failed: ${e.message}")
        }
    }
    
    /**
     * Generate NSFW media (images/videos) - only available in Monique mode
     */
    suspend fun generateNSFWMedia(request: MediaGenerationRequest): MediaGenerationResponse {
        return try {
            if (_currentPersonality.value != PersonalityMode.MONIQUE) {
                return MediaGenerationResponse.Error("NSFW media generation only available in Monique mode")
            }
            
            val result = when (request) {
                is ImageGenerationRequest -> {
                    mediaGenerator.generateImage(request)
                }
                is VideoGenerationRequest -> {
                    mediaGenerator.generateVideo(request)
                }
            }
            
            when (result) {
                is GenerationResult.Success -> {
                    MediaGenerationResponse.Success(
                        mediaId = result.mediaId,
                        mediaType = request.getMediaType(),
                        metadata = result.metadata
                    )
                }
                is GenerationResult.Error -> {
                    MediaGenerationResponse.Error(result.message)
                }
            }
            
        } catch (e: Exception) {
            Log.e("DualPersonalitySystem", "Failed to generate NSFW media", e)
            MediaGenerationResponse.Error("Media generation failed: ${e.message}")
        }
    }
    
    /**
     * Activate panic lock (emergency shutdown)
     */
    suspend fun activatePanicLock(reason: String = "User activated") {
        try {
            Log.w("DualPersonalitySystem", "ACTIVATING PANIC LOCK: $reason")
            
            // Activate panic lock through session manager
            sessionManager.activatePanicLock(PanicReason.USER_TRIGGERED)
            
            // Force switch to Nomad personality
            _currentPersonality.value = PersonalityMode.NOMAD
            
            // Update system state
            _systemState.value = _systemState.value.copy(
                isPanicLocked = true,
                panicLockTime = System.currentTimeMillis(),
                panicLockReason = reason,
                isMoniqueActive = false,
                securityLevel = SecurityLevel.LOCKDOWN
            )
            
            Log.e("DualPersonalitySystem", "PANIC LOCK ACTIVATED - All NSFW data secured")
            
        } catch (e: Exception) {
            Log.e("DualPersonalitySystem", "Failed to activate panic lock", e)
        }
    }
    
    /**
     * Get comprehensive system status
     */
    fun getSystemStatus(): SystemStatus {
        val sessionStats = sessionManager.getSessionStatistics()
        val networkState = networkController.networkState.value
        val panicState = sessionManager.panicState.value
        
        return SystemStatus(
            isSystemInitialized = _systemState.value.isInitialized,
            currentPersonality = _currentPersonality.value,
            isMoniqueSessionActive = _systemState.value.isMoniqueActive,
            securityLevel = _systemState.value.securityLevel,
            sessionDuration = sessionStats.sessionDuration,
            networkPolicyMode = networkController.policyMode.value,
            isNetworkConnected = networkState.isConnected,
            isPanicLocked = panicState.isPanicActive,
            isDecoyModeActive = sessionManager.decoyModeState.value.isDecoyActive,
            mediaGenerationAvailable = _currentPersonality.value == PersonalityMode.MONIQUE,
            systemHealthScore = calculateSystemHealthScore()
        )
    }
    
    /**
     * Configure security settings
     */
    suspend fun configureSecuritySettings(settings: SecuritySettings) {
        try {
            // Update network policies
            if (settings.offlineOnlyForNSFW) {
                networkController.setNetworkPolicyMode(NetworkPolicyMode.OFFLINE_ONLY)
            } else if (settings.requireE2EEncryption) {
                networkController.setNetworkPolicyMode(NetworkPolicyMode.E2E_ENCRYPTED_ONLY)
            }
            
            // Update media generation settings
            mediaGenerator.updateRenderSettings(RenderSettings(
                photoRealistic = settings.enablePhotorealGeneration,
                anonymizeFaces = settings.anonymizeFacesInMedia,
                preferLocal = settings.preferLocalGeneration,
                allowCloud = settings.allowCloudGeneration
            ))
            
            Log.d("DualPersonalitySystem", "Security settings updated")
            
        } catch (e: Exception) {
            Log.e("DualPersonalitySystem", "Failed to update security settings", e)
        }
    }
    
    // Private helper methods
    
    private suspend fun activateNSFWSecurityMode() {
        // Set maximum security network policies
        networkController.setNetworkPolicyMode(NetworkPolicyMode.E2E_ENCRYPTED_ONLY)
        
        // Initialize NSFW content classification
        contentClassifier.setNSFWModeActive(true)
        
        Log.d("DualPersonalitySystem", "NSFW security mode activated")
    }
    
    private suspend fun deactivateNSFWSecurityMode() {
        // Restore standard network policies
        networkController.setNetworkPolicyMode(NetworkPolicyMode.ALLOW_ALL)
        
        // Deactivate NSFW content classification
        contentClassifier.setNSFWModeActive(false)
        
        Log.d("DualPersonalitySystem", "NSFW security mode deactivated")
    }
    
    private fun startSystemHealthMonitoring() {
        scope.launch {
            while (true) {
                try {
                    val healthScore = calculateSystemHealthScore()
                    
                    _systemState.value = _systemState.value.copy(
                        systemHealthScore = healthScore,
                        lastHealthCheck = System.currentTimeMillis()
                    )
                    
                    // Check for critical health issues
                    if (healthScore < 0.5f) {
                        Log.w("DualPersonalitySystem", "System health critical: $healthScore")
                    }
                    
                    delay(30000) // Check every 30 seconds
                    
                } catch (e: Exception) {
                    Log.e("DualPersonalitySystem", "System health monitoring error", e)
                    delay(60000)
                }
            }
        }
    }
    
    private fun calculateSystemHealthScore(): Float {
        var score = 1.0f
        
        // Check component health
        if (!_systemState.value.isInitialized) score -= 0.3f
        if (_systemState.value.hasError) score -= 0.2f
        if (_systemState.value.isPanicLocked) score -= 0.1f
        
        // Check network health
        val networkState = networkController.networkState.value
        if (networkState.isEmergencyCutoffActive) score -= 0.2f
        
        return maxOf(0.0f, score)
    }
    
    /**
     * Clean up all system resources
     */
    fun cleanup() {
        try {
            sessionManager.cleanup()
            networkController.cleanup()
            privacyVault.cleanup()
            encryptionManager.cleanup()
            mediaGenerator.cleanup()
            scope.cancel()
            
            Log.d("DualPersonalitySystem", "Dual personality system cleaned up")
            
        } catch (e: Exception) {
            Log.e("DualPersonalitySystem", "Error during system cleanup", e)
        }
    }
}

// Data classes for system integration

data class SystemState(
    val isInitialized: Boolean = false,
    val initializationTime: Long? = null,
    val isMoniqueActive: Boolean = false,
    val moniqueActivationTime: Long? = null,
    val nomadActivationTime: Long? = null,
    val securityLevel: SecurityLevel = SecurityLevel.STANDARD,
    val isPanicLocked: Boolean = false,
    val panicLockTime: Long? = null,
    val panicLockReason: String? = null,
    val systemHealthScore: Float = 1.0f,
    val lastHealthCheck: Long = 0L,
    val hasError: Boolean = false,
    val errorMessage: String? = null
)

data class SystemStatus(
    val isSystemInitialized: Boolean,
    val currentPersonality: PersonalityMode,
    val isMoniqueSessionActive: Boolean,
    val securityLevel: SecurityLevel,
    val sessionDuration: Long,
    val networkPolicyMode: NetworkPolicyMode,
    val isNetworkConnected: Boolean,
    val isPanicLocked: Boolean,
    val isDecoyModeActive: Boolean,
    val mediaGenerationAvailable: Boolean,
    val systemHealthScore: Float
)

data class SecuritySettings(
    val offlineOnlyForNSFW: Boolean = false,
    val requireE2EEncryption: Boolean = true,
    val enablePhotorealGeneration: Boolean = false,
    val anonymizeFacesInMedia: Boolean = true,
    val preferLocalGeneration: Boolean = true,
    val allowCloudGeneration: Boolean = false,
    val panicGesturesEnabled: Boolean = true,
    val decoyModeEnabled: Boolean = true,
    val sessionTimeoutMinutes: Int = 30,
    val maxSessionHours: Int = 2
)

sealed class MediaGenerationRequest {
    abstract fun getMediaType(): MediaType
}

data class ImageGenerationRequest(
    val prompt: String,
    val width: Int = 512,
    val height: Int = 512,
    val style: String? = null
) : MediaGenerationRequest() {
    override fun getMediaType(): MediaType = MediaType.IMAGE
}

data class VideoGenerationRequest(
    val prompt: String,
    val duration: Int = 5,
    val fps: Int = 24
) : MediaGenerationRequest() {
    override fun getMediaType(): MediaType = MediaType.VIDEO
}

sealed class MediaGenerationResponse {
    data class Success(
        val mediaId: String,
        val mediaType: MediaType,
        val metadata: GeneratedMediaMetadata
    ) : MediaGenerationResponse()
    
    data class Error(val message: String) : MediaGenerationResponse()
}

sealed class PersonalityResponse {
    data class Success(
        val response: String,
        val personality: PersonalityMode,
        val contentRating: ContentRating
    ) : PersonalityResponse()
    
    data class Blocked(val reason: String) : PersonalityResponse()
    data class Error(val message: String) : PersonalityResponse()
}

enum class SecurityLevel {
    STANDARD, HIGH, MAXIMUM, LOCKDOWN
}

enum class MessageType {
    TEXT, VOICE, IMAGE, VIDEO
}

enum class ContentRating {
    SFW, NSFW, BLOCKED
}

interface PersonalitySwitchCallback {
    fun onSuccess(personality: PersonalityMode)
    fun onError(message: String)
}