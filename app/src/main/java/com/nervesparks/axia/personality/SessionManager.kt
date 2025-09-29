package com.nervesparks.axia.personality

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

/**
 * Session Manager for NSFW Content Security
 * 
 * Features:
 * - Panic lock with quick gestures
 * - Decoy mode activation
 * - Session boundaries and timeouts
 * - Emergency wipe capabilities
 * - Biometric re-authentication
 * - Activity monitoring and stealth mode
 */
class SessionManager(
    private val context: Context,
    private val privacyVault: NSFWPrivacyVault,
    private val encryptionManager: EncryptionManager,
    private val networkController: NetworkPolicyController
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // Session state
    private val _sessionState = MutableStateFlow(SessionState())
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()
    
    private val _panicState = MutableStateFlow(PanicState())
    val panicState: StateFlow<PanicState> = _panicState.asStateFlow()
    
    private val _decoyModeState = MutableStateFlow(DecoyModeState())
    val decoyModeState: StateFlow<DecoyModeState> = _decoyModeState.asStateFlow()
    
    // Session management
    private var sessionStartTime = AtomicLong(0)
    private var lastActivityTime = AtomicLong(0)
    private var sessionTimeoutJob: Job? = null
    private var activityMonitorJob: Job? = null
    
    // Panic detection
    private val panicGestureDetector = PanicGestureDetector()
    private var panicTriggerCount = 0
    private var lastPanicTriggerTime = 0L
    
    // Biometric authentication
    private var biometricPrompt: BiometricPrompt? = null
    private var biometricCallback: BiometricAuthenticationCallback? = null
    
    // Decoy mode
    private val decoyConversations = generateDecoyConversations()
    private var decoyModeActive = false
    
    init {
        initializeSessionManager()
    }
    
    /**
     * Initialize session manager
     */
    private fun initializeSessionManager() {
        try {
            // Start activity monitoring
            startActivityMonitoring()
            
            // Initialize panic gesture detection
            panicGestureDetector.initialize()
            
            // Set up session timeout monitoring
            startSessionTimeoutMonitoring()
            
            Log.d("SessionManager", "Session manager initialized")
            
        } catch (e: Exception) {
            Log.e("SessionManager", "Failed to initialize session manager", e)
        }
    }
    
    /**
     * Start NSFW session with biometric authentication
     */
    suspend fun startNSFWSession(
        activity: FragmentActivity,
        callback: SessionStartCallback
    ) {
        withContext(Dispatchers.Main) {
            try {
                // Check if biometric authentication is available
                val biometricManager = BiometricManager.from(context)
                when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
                    BiometricManager.BIOMETRIC_SUCCESS -> {
                        authenticateForNSFWAccess(activity, callback)
                    }
                    BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                        callback.onError("No biometric hardware available")
                    }
                    BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                        callback.onError("Biometric hardware unavailable")
                    }
                    BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                        callback.onError("No biometric credentials enrolled")
                    }
                    else -> {
                        callback.onError("Biometric authentication not available")
                    }
                }
                
            } catch (e: Exception) {
                Log.e("SessionManager", "Failed to start NSFW session", e)
                callback.onError("Session start failed: ${e.message}")
            }
        }
    }
    
    /**
     * End NSFW session and clean up
     */
    suspend fun endNSFWSession(wipeTemporaryData: Boolean = true) {
        withContext(Dispatchers.IO) {
            try {
                val currentSession = _sessionState.value
                if (!currentSession.isNSFWActive) {
                    Log.d("SessionManager", "No active NSFW session to end")
                    return@withContext
                }
                
                Log.d("SessionManager", "Ending NSFW session")
                
                // Update session state
                _sessionState.value = currentSession.copy(
                    isNSFWActive = false,
                    sessionEndTime = System.currentTimeMillis(),
                    isEnding = true
                )
                
                // Wipe temporary data if requested
                if (wipeTemporaryData) {
                    wipeTemporaryNSFWData()
                }
                
                // Clear sensitive memory
                clearSensitiveMemory()
                
                // Cancel session timeout
                sessionTimeoutJob?.cancel()
                
                // Reset panic state
                _panicState.value = PanicState()
                
                // Exit decoy mode if active
                if (decoyModeActive) {
                    exitDecoyMode()
                }
                
                // Final state update
                _sessionState.value = SessionState()
                
                Log.d("SessionManager", "NSFW session ended successfully")
                
            } catch (e: Exception) {
                Log.e("SessionManager", "Failed to end NSFW session", e)
            }
        }
    }
    
    /**
     * Activate panic lock (emergency session termination)
     */
    suspend fun activatePanicLock(reason: PanicReason = PanicReason.USER_TRIGGERED) {
        withContext(Dispatchers.IO) {
            try {
                Log.w("SessionManager", "PANIC LOCK ACTIVATED: $reason")
                
                // Update panic state immediately
                _panicState.value = PanicState(
                    isPanicActive = true,
                    panicReason = reason,
                    panicTime = System.currentTimeMillis(),
                    isEmergencyWipeActive = true
                )
                
                // Emergency network cutoff
                networkController.emergencyNetworkCutoff("Panic lock activated")
                
                // Emergency wipe of NSFW data
                privacyVault.emergencyWipe()
                
                // Clear all encryption keys
                encryptionManager.clearAllKeys()
                
                // End NSFW session immediately
                _sessionState.value = _sessionState.value.copy(
                    isNSFWActive = false,
                    isPanicLocked = true,
                    sessionEndTime = System.currentTimeMillis(),
                    panicLockTime = System.currentTimeMillis()
                )
                
                // Activate decoy mode if not already active
                if (!decoyModeActive) {
                    activateDecoyMode()
                }
                
                // Clear app from recent apps (if possible)
                clearFromRecentApps()
                
                Log.e("SessionManager", "PANIC LOCK COMPLETE - All NSFW data wiped")
                
            } catch (e: Exception) {
                Log.e("SessionManager", "Failed to activate panic lock", e)
            }
        }
    }
    
    /**
     * Activate decoy mode (fake innocent conversation)
     */
    suspend fun activateDecoyMode() {
        withContext(Dispatchers.Main) {
            try {
                if (decoyModeActive) {
                    Log.d("SessionManager", "Decoy mode already active")
                    return@withContext
                }
                
                Log.d("SessionManager", "Activating decoy mode")
                
                decoyModeActive = true
                
                // Select random decoy conversation
                val decoyConversation = decoyConversations.random()
                
                _decoyModeState.value = DecoyModeState(
                    isDecoyActive = true,
                    decoyPersonality = "Assistant",
                    decoyConversation = decoyConversation,
                    activationTime = System.currentTimeMillis(),
                    decoyReason = if (_panicState.value.isPanicActive) "Panic activated" else "Manual activation"
                )
                
                // Switch UI to decoy mode
                switchToDecoyUI()
                
                Log.d("SessionManager", "Decoy mode activated")
                
            } catch (e: Exception) {
                Log.e("SessionManager", "Failed to activate decoy mode", e)
            }
        }
    }
    
    /**
     * Exit decoy mode and return to normal operation
     */
    suspend fun exitDecoyMode(authToken: String? = null) {
        withContext(Dispatchers.Main) {
            try {
                if (!decoyModeActive) {
                    Log.d("SessionManager", "Decoy mode not active")
                    return@withContext
                }
                
                // If panic was activated, require authentication to exit decoy mode
                if (_panicState.value.isPanicActive && authToken == null) {
                    Log.w("SessionManager", "Authentication required to exit decoy mode after panic")
                    return@withContext
                }
                
                Log.d("SessionManager", "Exiting decoy mode")
                
                decoyModeActive = false
                
                _decoyModeState.value = DecoyModeState(
                    isDecoyActive = false,
                    deactivationTime = System.currentTimeMillis()
                )
                
                // Switch back to normal UI
                switchToNormalUI()
                
                Log.d("SessionManager", "Decoy mode exited")
                
            } catch (e: Exception) {
                Log.e("SessionManager", "Failed to exit decoy mode", e)
            }
        }
    }
    
    /**
     * Update user activity timestamp
     */
    fun recordUserActivity() {
        lastActivityTime.set(System.currentTimeMillis())
        
        // Reset session timeout if NSFW session is active
        if (_sessionState.value.isNSFWActive) {
            resetSessionTimeout()
        }
    }
    
    /**
     * Check if user activity indicates potential threat
     */
    fun analyzeUserActivity(
        gestureType: String,
        gestureData: Map<String, Any>
    ) {
        scope.launch {
            try {
                // Detect panic gestures
                if (panicGestureDetector.isPanicGesture(gestureType, gestureData)) {
                    handlePanicGesture()
                }
                
                // Detect suspicious patterns
                if (detectSuspiciousActivity(gestureType, gestureData)) {
                    Log.w("SessionManager", "Suspicious activity detected")
                    // Could trigger additional security measures
                }
                
            } catch (e: Exception) {
                Log.e("SessionManager", "Error analyzing user activity", e)
            }
        }
    }
    
    /**
     * Get session statistics
     */
    fun getSessionStatistics(): SessionStatistics {
        val currentSession = _sessionState.value
        val currentTime = System.currentTimeMillis()
        
        return SessionStatistics(
            isNSFWSessionActive = currentSession.isNSFWActive,
            sessionStartTime = currentSession.sessionStartTime,
            sessionDuration = if (currentSession.isNSFWActive) 
                currentTime - currentSession.sessionStartTime else 0L,
            lastActivityTime = lastActivityTime.get(),
            timeSinceLastActivity = currentTime - lastActivityTime.get(),
            panicLockCount = panicTriggerCount,
            lastPanicTime = if (_panicState.value.isPanicActive) _panicState.value.panicTime else null,
            decoyModeActivations = if (decoyModeActive) 1 else 0,
            sessionTimeoutRemaining = calculateTimeoutRemaining()
        )
    }
    
    // Private helper methods
    
    private fun authenticateForNSFWAccess(
        activity: FragmentActivity,
        callback: SessionStartCallback
    ) {
        val executor = ContextCompat.getMainExecutor(context)
        
        biometricCallback = object : BiometricAuthenticationCallback {
            override fun onAuthenticationSucceeded() {
                scope.launch {
                    startNSFWSessionInternal(callback)
                }
            }
            
            override fun onAuthenticationFailed() {
                callback.onError("Biometric authentication failed")
            }
            
            override fun onAuthenticationError(error: String) {
                callback.onError("Authentication error: $error")
            }
        }
        
        biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                biometricCallback?.onAuthenticationSucceeded()
            }
            
            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                biometricCallback?.onAuthenticationFailed()
            }
            
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                biometricCallback?.onAuthenticationError(errString.toString())
            }
        })
        
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("NSFW Content Access")
            .setSubtitle("Authenticate to access adult content")
            .setDescription("This content requires biometric authentication")
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()
        
        biometricPrompt?.authenticate(promptInfo)
    }
    
    private suspend fun startNSFWSessionInternal(callback: SessionStartCallback) {
        try {
            val currentTime = System.currentTimeMillis()
            
            sessionStartTime.set(currentTime)
            lastActivityTime.set(currentTime)
            
            _sessionState.value = SessionState(
                isNSFWActive = true,
                sessionStartTime = currentTime,
                lastActivityTime = currentTime,
                sessionTimeout = 30 * 60 * 1000L, // 30 minutes default
                maxSessionDuration = 2 * 60 * 60 * 1000L // 2 hours max
            )
            
            // Start session timeout monitoring
            startSessionTimeout()
            
            Log.d("SessionManager", "NSFW session started successfully")
            callback.onSuccess()
            
        } catch (e: Exception) {
            Log.e("SessionManager", "Failed to start NSFW session internally", e)
            callback.onError("Internal session start failed: ${e.message}")
        }
    }
    
    private fun startActivityMonitoring() {
        activityMonitorJob = scope.launch {
            while (true) {
                try {
                    val currentTime = System.currentTimeMillis()
                    val lastActivity = lastActivityTime.get()
                    val timeSinceActivity = currentTime - lastActivity
                    
                    // Check for suspicious inactivity patterns
                    if (_sessionState.value.isNSFWActive && timeSinceActivity > 5 * 60 * 1000L) {
                        Log.w("SessionManager", "Extended inactivity detected in NSFW session")
                    }
                    
                    delay(30000) // Check every 30 seconds
                    
                } catch (e: Exception) {
                    Log.e("SessionManager", "Activity monitoring error", e)
                    delay(60000) // Wait longer on error
                }
            }
        }
    }
    
    private fun startSessionTimeoutMonitoring() {
        scope.launch {
            while (true) {
                try {
                    val currentSession = _sessionState.value
                    if (currentSession.isNSFWActive) {
                        val sessionDuration = System.currentTimeMillis() - currentSession.sessionStartTime
                        val timeSinceActivity = System.currentTimeMillis() - lastActivityTime.get()
                        
                        // Check for session timeout
                        if (timeSinceActivity > currentSession.sessionTimeout ||
                            sessionDuration > currentSession.maxSessionDuration) {
                            Log.w("SessionManager", "Session timeout - ending NSFW session")
                            endNSFWSession(wipeTemporaryData = true)
                        }
                    }
                    
                    delay(60000) // Check every minute
                    
                } catch (e: Exception) {
                    Log.e("SessionManager", "Session timeout monitoring error", e)
                    delay(60000)
                }
            }
        }
    }
    
    private fun startSessionTimeout() {
        sessionTimeoutJob?.cancel()
        sessionTimeoutJob = scope.launch {
            val timeout = _sessionState.value.sessionTimeout
            delay(timeout)
            
            if (_sessionState.value.isNSFWActive) {
                Log.d("SessionManager", "Session timeout reached")
                endNSFWSession(wipeTemporaryData = true)
            }
        }
    }
    
    private fun resetSessionTimeout() {
        startSessionTimeout()
    }
    
    private fun handlePanicGesture() {
        panicTriggerCount++
        lastPanicTriggerTime = System.currentTimeMillis()
        
        Log.w("SessionManager", "Panic gesture detected - count: $panicTriggerCount")
        
        scope.launch {
            activatePanicLock(PanicReason.PANIC_GESTURE)
        }
    }
    
    private fun detectSuspiciousActivity(gestureType: String, data: Map<String, Any>): Boolean {
        // Implement suspicious activity detection
        return false
    }
    
    private suspend fun wipeTemporaryNSFWData() {
        try {
            // Clear temporary files, cache, memory
            privacyVault.clearTemporaryData()
            
            Log.d("SessionManager", "Temporary NSFW data wiped")
            
        } catch (e: Exception) {
            Log.e("SessionManager", "Failed to wipe temporary data", e)
        }
    }
    
    private fun clearSensitiveMemory() {
        // Force garbage collection to clear sensitive data from memory
        System.gc()
        System.runFinalization()
        System.gc()
    }
    
    private fun clearFromRecentApps() {
        try {
            // Attempt to remove app from recent apps list
            // This is platform-specific and may not always work
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE)
            // Implementation would depend on Android version and permissions
            
        } catch (e: Exception) {
            Log.e("SessionManager", "Failed to clear from recent apps", e)
        }
    }
    
    private fun switchToDecoyUI() {
        // Signal UI to switch to decoy mode
        // This would be handled by the UI layer
        Log.d("SessionManager", "Switching to decoy UI")
    }
    
    private fun switchToNormalUI() {
        // Signal UI to switch back to normal mode
        Log.d("SessionManager", "Switching to normal UI")
    }
    
    private fun calculateTimeoutRemaining(): Long {
        val currentSession = _sessionState.value
        if (!currentSession.isNSFWActive) return 0L
        
        val timeSinceActivity = System.currentTimeMillis() - lastActivityTime.get()
        return maxOf(0L, currentSession.sessionTimeout - timeSinceActivity)
    }
    
    private fun generateDecoyConversations(): List<DecoyConversation> {
        return listOf(
            DecoyConversation(
                title = "Weather Discussion",
                messages = listOf(
                    "How's the weather today?",
                    "It's quite nice, sunny with a mild breeze. Perfect for outdoor activities.",
                    "That sounds lovely. Any recommendations for outdoor activities?",
                    "You could try hiking, cycling, or having a picnic in the park."
                )
            ),
            DecoyConversation(
                title = "Recipe Help",
                messages = listOf(
                    "Can you help me with a recipe for dinner?",
                    "Of course! What type of cuisine are you in the mood for?",
                    "Something Italian would be great.",
                    "How about pasta carbonara? It's delicious and not too difficult to make."
                )
            ),
            DecoyConversation(
                title = "Study Tips",
                messages = listOf(
                    "I need some study tips for my exam.",
                    "Here are some effective study strategies: active recall, spaced repetition, and the Pomodoro Technique.",
                    "Can you explain the Pomodoro Technique?",
                    "It involves studying for 25-minute focused sessions followed by 5-minute breaks."
                )
            )
        )
    }
    
    /**
     * Clean up session manager resources
     */
    fun cleanup() {
        try {
            sessionTimeoutJob?.cancel()
            activityMonitorJob?.cancel()
            scope.cancel()
            
            // Clear sensitive data
            clearSensitiveMemory()
            
            Log.d("SessionManager", "Session manager cleaned up")
            
        } catch (e: Exception) {
            Log.e("SessionManager", "Error during cleanup", e)
        }
    }
}

// Data classes for session management

data class SessionState(
    val isNSFWActive: Boolean = false,
    val sessionStartTime: Long = 0L,
    val sessionEndTime: Long? = null,
    val lastActivityTime: Long = 0L,
    val sessionTimeout: Long = 30 * 60 * 1000L, // 30 minutes
    val maxSessionDuration: Long = 2 * 60 * 60 * 1000L, // 2 hours
    val isPanicLocked: Boolean = false,
    val panicLockTime: Long? = null,
    val isEnding: Boolean = false
)

data class PanicState(
    val isPanicActive: Boolean = false,
    val panicReason: PanicReason? = null,
    val panicTime: Long? = null,
    val isEmergencyWipeActive: Boolean = false,
    val panicGestureCount: Int = 0
)

data class DecoyModeState(
    val isDecoyActive: Boolean = false,
    val decoyPersonality: String? = null,
    val decoyConversation: DecoyConversation? = null,
    val activationTime: Long? = null,
    val deactivationTime: Long? = null,
    val decoyReason: String? = null
)

data class DecoyConversation(
    val title: String,
    val messages: List<String>
)

data class SessionStatistics(
    val isNSFWSessionActive: Boolean,
    val sessionStartTime: Long,
    val sessionDuration: Long,
    val lastActivityTime: Long,
    val timeSinceLastActivity: Long,
    val panicLockCount: Int,
    val lastPanicTime: Long?,
    val decoyModeActivations: Int,
    val sessionTimeoutRemaining: Long
)

enum class PanicReason {
    USER_TRIGGERED,
    PANIC_GESTURE,
    SUSPICIOUS_ACTIVITY,
    TIMEOUT,
    EXTERNAL_THREAT
}

interface SessionStartCallback {
    fun onSuccess()
    fun onError(message: String)
}

interface BiometricAuthenticationCallback {
    fun onAuthenticationSucceeded()
    fun onAuthenticationFailed()
    fun onAuthenticationError(error: String)
}

// Placeholder class for panic gesture detection
class PanicGestureDetector {
    fun initialize() {
        // Initialize gesture detection
    }
    
    fun isPanicGesture(gestureType: String, data: Map<String, Any>): Boolean {
        // Implement panic gesture detection logic
        // Examples: rapid screen taps, specific swipe patterns, device shaking
        return when (gestureType) {
            "rapid_tap" -> {
                val tapCount = data["count"] as? Int ?: 0
                val timeWindow = data["timeWindow"] as? Long ?: 0L
                tapCount >= 5 && timeWindow <= 2000L // 5 taps in 2 seconds
            }
            "triple_tap_volume" -> true // Triple tap on volume buttons
            "shake_device" -> {
                val intensity = data["intensity"] as? Float ?: 0f
                intensity > 0.8f // Strong shake
            }
            else -> false
        }
    }
}