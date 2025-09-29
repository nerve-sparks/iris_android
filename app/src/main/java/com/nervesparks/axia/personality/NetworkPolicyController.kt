package com.nervesparks.axia.personality

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.net.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom

/**
 * Network Policy Controller for NSFW Content
 * 
 * Features:
 * - Offline-only mode for NSFW content
 * - E2E encryption for cloud services
 * - Network isolation and monitoring
 * - VPN detection and handling
 * - Secure communication channels
 * - Emergency network cutoff
 */
class NetworkPolicyController(
    private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // Network state
    private val _networkState = MutableStateFlow(NetworkState())
    val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()
    
    private val _policyMode = MutableStateFlow(NetworkPolicyMode.ALLOW_ALL)
    val policyMode: StateFlow<NetworkPolicyMode> = _policyMode.asStateFlow()
    
    // Network monitoring
    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    
    // Policy settings
    private var currentPolicies = NetworkPolicies()
    
    // Blocked domains and IPs
    private val blockedDomains = mutableSetOf<String>()
    private val allowedDomains = mutableSetOf<String>()
    private val trustedCertificates = mutableSetOf<String>()
    
    // E2E Encryption
    private val encryptionKeys = mutableMapOf<String, SecretKeySpec>()
    
    init {
        initializeNetworkController()
    }
    
    /**
     * Initialize network policy controller
     */
    private fun initializeNetworkController() {
        try {
            connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            
            // Set up network monitoring
            setupNetworkMonitoring()
            
            // Initialize default policies
            setupDefaultPolicies()
            
            // Start policy enforcement
            startPolicyEnforcement()
            
            Log.d("NetworkPolicyController", "Network controller initialized")
            
        } catch (e: Exception) {
            Log.e("NetworkPolicyController", "Failed to initialize network controller", e)
        }
    }
    
    /**
     * Set network policy mode for NSFW content
     */
    fun setNetworkPolicyMode(mode: NetworkPolicyMode) {
        scope.launch {
            try {
                _policyMode.value = mode
                
                when (mode) {
                    NetworkPolicyMode.OFFLINE_ONLY -> {
                        enforceOfflineOnlyMode()
                    }
                    NetworkPolicyMode.E2E_ENCRYPTED_ONLY -> {
                        enforceE2EEncryptionMode()
                    }
                    NetworkPolicyMode.VPN_REQUIRED -> {
                        enforceVPNRequiredMode()
                    }
                    NetworkPolicyMode.BLOCKED -> {
                        enforceNetworkBlocked()
                    }
                    NetworkPolicyMode.ALLOW_ALL -> {
                        removeNetworkRestrictions()
                    }
                }
                
                Log.d("NetworkPolicyController", "Network policy mode changed to: $mode")
                
            } catch (e: Exception) {
                Log.e("NetworkPolicyController", "Failed to set network policy mode", e)
            }
        }
    }
    
    /**
     * Check if network request is allowed under current policy
     */
    suspend fun isNetworkRequestAllowed(
        url: String,
        contentType: ContentType = ContentType.GENERAL,
        requiresEncryption: Boolean = false
    ): NetworkRequestResult {
        return withContext(Dispatchers.IO) {
            try {
                val uri = URI(url)
                val domain = uri.host
                
                // Check current policy mode
                when (_policyMode.value) {
                    NetworkPolicyMode.OFFLINE_ONLY -> {
                        if (contentType == ContentType.NSFW_CONTENT) {
                            NetworkRequestResult.Blocked("NSFW content blocked in offline-only mode")
                        } else {
                            NetworkRequestResult.Allowed
                        }
                    }
                    
                    NetworkPolicyMode.E2E_ENCRYPTED_ONLY -> {
                        if (contentType == ContentType.NSFW_CONTENT && !requiresEncryption) {
                            NetworkRequestResult.Blocked("NSFW content requires E2E encryption")
                        } else if (!uri.scheme.equals("https", ignoreCase = true)) {
                            NetworkRequestResult.Blocked("Only HTTPS connections allowed")
                        } else {
                            validateE2EEncryption(url, contentType)
                        }
                    }
                    
                    NetworkPolicyMode.VPN_REQUIRED -> {
                        if (contentType == ContentType.NSFW_CONTENT && !isVPNActive()) {
                            NetworkRequestResult.Blocked("VPN required for NSFW content")
                        } else {
                            NetworkRequestResult.Allowed
                        }
                    }
                    
                    NetworkPolicyMode.BLOCKED -> {
                        NetworkRequestResult.Blocked("All network access blocked")
                    }
                    
                    NetworkPolicyMode.ALLOW_ALL -> {
                        // Still check domain blocking
                        if (isDomainBlocked(domain)) {
                            NetworkRequestResult.Blocked("Domain blocked: $domain")
                        } else {
                            NetworkRequestResult.Allowed
                        }
                    }
                }
                
            } catch (e: Exception) {
                Log.e("NetworkPolicyController", "Error checking network request", e)
                NetworkRequestResult.Blocked("Network policy check failed")
            }
        }
    }
    
    /**
     * Create encrypted connection for cloud services
     */
    suspend fun createEncryptedConnection(
        serviceUrl: String,
        serviceId: String
    ): EncryptedConnectionResult {
        return withContext(Dispatchers.IO) {
            try {
                // Check if connection is allowed
                val requestCheck = isNetworkRequestAllowed(
                    url = serviceUrl,
                    contentType = ContentType.NSFW_CONTENT,
                    requiresEncryption = true
                )
                
                if (requestCheck is NetworkRequestResult.Blocked) {
                    return@withContext EncryptedConnectionResult.Error(requestCheck.reason)
                }
                
                // Generate or retrieve encryption key for this service
                val encryptionKey = getOrCreateServiceKey(serviceId)
                
                // Create secure connection
                val connection = createSecureConnection(serviceUrl, encryptionKey)
                
                EncryptedConnectionResult.Success(
                    connection = connection,
                    encryptionKey = encryptionKey,
                    serviceId = serviceId
                )
                
            } catch (e: Exception) {
                Log.e("NetworkPolicyController", "Failed to create encrypted connection", e)
                EncryptedConnectionResult.Error("Connection creation failed: ${e.message}")
            }
        }
    }
    
    /**
     * Encrypt data for transmission
     */
    fun encryptForTransmission(
        data: ByteArray,
        serviceId: String
    ): EncryptionResult {
        return try {
            val key = encryptionKeys[serviceId]
                ?: return EncryptionResult.Error("No encryption key for service: $serviceId")
            
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            
            val encryptedData = cipher.doFinal(data)
            val iv = cipher.iv
            
            // Combine IV and encrypted data
            val combined = ByteArray(iv.size + encryptedData.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedData, 0, combined, iv.size, encryptedData.size)
            
            EncryptionResult.Success(combined)
            
        } catch (e: Exception) {
            Log.e("NetworkPolicyController", "Encryption failed", e)
            EncryptionResult.Error("Encryption failed: ${e.message}")
        }
    }
    
    /**
     * Decrypt received data
     */
    fun decryptFromTransmission(
        encryptedData: ByteArray,
        serviceId: String
    ): EncryptionResult {
        return try {
            val key = encryptionKeys[serviceId]
                ?: return EncryptionResult.Error("No decryption key for service: $serviceId")
            
            // Extract IV and encrypted data
            val ivSize = 12 // GCM standard IV size
            val iv = encryptedData.sliceArray(0..ivSize - 1)
            val cipherText = encryptedData.sliceArray(ivSize until encryptedData.size)
            
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val gcmSpec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)
            
            val decryptedData = cipher.doFinal(cipherText)
            EncryptionResult.Success(decryptedData)
            
        } catch (e: Exception) {
            Log.e("NetworkPolicyController", "Decryption failed", e)
            EncryptionResult.Error("Decryption failed: ${e.message}")
        }
    }
    
    /**
     * Emergency network cutoff
     */
    suspend fun emergencyNetworkCutoff(reason: String = "Emergency cutoff activated") {
        withContext(Dispatchers.IO) {
            try {
                Log.w("NetworkPolicyController", "Emergency network cutoff: $reason")
                
                // Block all network access
                setNetworkPolicyMode(NetworkPolicyMode.BLOCKED)
                
                // Clear all encryption keys
                encryptionKeys.clear()
                
                // Update network state
                _networkState.value = _networkState.value.copy(
                    isEmergencyCutoffActive = true,
                    emergencyCutoffReason = reason,
                    emergencyCutoffTime = System.currentTimeMillis()
                )
                
                // Notify about emergency cutoff
                Log.e("NetworkPolicyController", "EMERGENCY NETWORK CUTOFF ACTIVATED: $reason")
                
            } catch (e: Exception) {
                Log.e("NetworkPolicyController", "Failed to activate emergency cutoff", e)
            }
        }
    }
    
    /**
     * Restore network access after emergency cutoff
     */
    suspend fun restoreNetworkAccess(authToken: String) {
        withContext(Dispatchers.IO) {
            try {
                // Verify auth token (placeholder - implement proper verification)
                if (!verifyAuthToken(authToken)) {
                    Log.w("NetworkPolicyController", "Invalid auth token for network restoration")
                    return@withContext
                }
                
                // Restore default policy
                setNetworkPolicyMode(NetworkPolicyMode.ALLOW_ALL)
                
                // Update network state
                _networkState.value = _networkState.value.copy(
                    isEmergencyCutoffActive = false,
                    emergencyCutoffReason = null,
                    emergencyCutoffTime = null
                )
                
                Log.d("NetworkPolicyController", "Network access restored")
                
            } catch (e: Exception) {
                Log.e("NetworkPolicyController", "Failed to restore network access", e)
            }
        }
    }
    
    /**
     * Add domain to block list
     */
    fun blockDomain(domain: String) {
        blockedDomains.add(domain.lowercase())
        Log.d("NetworkPolicyController", "Blocked domain: $domain")
    }
    
    /**
     * Add domain to allow list
     */
    fun allowDomain(domain: String) {
        allowedDomains.add(domain.lowercase())
        blockedDomains.remove(domain.lowercase())
        Log.d("NetworkPolicyController", "Allowed domain: $domain")
    }
    
    /**
     * Get current network status
     */
    fun getNetworkStatus(): NetworkStatusInfo {
        val cm = connectivityManager ?: return NetworkStatusInfo()
        
        return try {
            val activeNetwork = cm.activeNetwork
            val capabilities = activeNetwork?.let { cm.getNetworkCapabilities(it) }
            
            NetworkStatusInfo(
                isConnected = activeNetwork != null,
                isWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true,
                isCellular = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true,
                isVPN = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true,
                isMetered = cm.isActiveNetworkMetered,
                connectionType = determineConnectionType(capabilities),
                bandwidth = estimateBandwidth(capabilities)
            )
        } catch (e: Exception) {
            Log.e("NetworkPolicyController", "Error getting network status", e)
            NetworkStatusInfo()
        }
    }
    
    // Private helper methods
    
    private fun setupNetworkMonitoring() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                updateNetworkState()
                Log.d("NetworkPolicyController", "Network available: $network")
            }
            
            override fun onLost(network: Network) {
                super.onLost(network)
                updateNetworkState()
                Log.d("NetworkPolicyController", "Network lost: $network")
            }
            
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                updateNetworkState()
            }
        }
        
        connectivityManager?.registerNetworkCallback(networkRequest, networkCallback!!)
    }
    
    private fun updateNetworkState() {
        val status = getNetworkStatus()
        _networkState.value = _networkState.value.copy(
            isConnected = status.isConnected,
            connectionType = status.connectionType,
            isVPNActive = status.isVPN,
            isMetered = status.isMetered,
            lastUpdate = System.currentTimeMillis()
        )
    }
    
    private fun setupDefaultPolicies() {
        // Add default blocked domains
        blockedDomains.addAll(listOf(
            "malicious-domain.com",
            "tracker.com",
            "ads.com"
        ))
        
        // Add default allowed domains
        allowedDomains.addAll(listOf(
            "api.openai.com",
            "api.anthropic.com",
            "secure-ai-service.com"
        ))
    }
    
    private fun startPolicyEnforcement() {
        scope.launch {
            while (true) {
                try {
                    enforceCurrentPolicies()
                    delay(10000) // Check every 10 seconds
                } catch (e: Exception) {
                    Log.e("NetworkPolicyController", "Policy enforcement error", e)
                    delay(30000) // Wait longer on error
                }
            }
        }
    }
    
    private suspend fun enforceCurrentPolicies() {
        val currentMode = _policyMode.value
        val networkStatus = getNetworkStatus()
        
        // Check for policy violations
        if (currentMode == NetworkPolicyMode.VPN_REQUIRED && !networkStatus.isVPN) {
            Log.w("NetworkPolicyController", "VPN required but not active")
        }
        
        if (currentMode == NetworkPolicyMode.OFFLINE_ONLY && networkStatus.isConnected) {
            Log.w("NetworkPolicyController", "Offline-only mode but network is connected")
        }
    }
    
    private fun enforceOfflineOnlyMode() {
        // Implementation to restrict network access for NSFW content
        Log.d("NetworkPolicyController", "Enforcing offline-only mode")
    }
    
    private fun enforceE2EEncryptionMode() {
        // Implementation to ensure E2E encryption
        Log.d("NetworkPolicyController", "Enforcing E2E encryption mode")
    }
    
    private fun enforceVPNRequiredMode() {
        // Implementation to require VPN
        Log.d("NetworkPolicyController", "Enforcing VPN required mode")
    }
    
    private fun enforceNetworkBlocked() {
        // Implementation to block all network access
        Log.d("NetworkPolicyController", "Enforcing network blocked mode")
    }
    
    private fun removeNetworkRestrictions() {
        // Implementation to remove restrictions
        Log.d("NetworkPolicyController", "Removing network restrictions")
    }
    
    private fun isDomainBlocked(domain: String?): Boolean {
        if (domain == null) return false
        val lowerDomain = domain.lowercase()
        return blockedDomains.contains(lowerDomain) && !allowedDomains.contains(lowerDomain)
    }
    
    private fun isVPNActive(): Boolean {
        return getNetworkStatus().isVPN
    }
    
    private suspend fun validateE2EEncryption(url: String, contentType: ContentType): NetworkRequestResult {
        return try {
            // Check if service supports E2E encryption
            val uri = URI(url)
            val serviceId = extractServiceId(uri)
            
            if (encryptionKeys.containsKey(serviceId)) {
                NetworkRequestResult.Allowed
            } else {
                NetworkRequestResult.Blocked("E2E encryption not established for service")
            }
        } catch (e: Exception) {
            NetworkRequestResult.Blocked("E2E encryption validation failed")
        }
    }
    
    private fun getOrCreateServiceKey(serviceId: String): SecretKeySpec {
        return encryptionKeys[serviceId] ?: run {
            val keyGen = KeyGenerator.getInstance("AES")
            keyGen.init(256)
            val key = SecretKeySpec(keyGen.generateKey().encoded, "AES")
            encryptionKeys[serviceId] = key
            key
        }
    }
    
    private fun createSecureConnection(url: String, key: SecretKeySpec): SecureConnection {
        // Placeholder for secure connection creation
        return SecureConnection(url, key)
    }
    
    private fun extractServiceId(uri: URI): String {
        return "${uri.host}:${uri.port}"
    }
    
    private fun verifyAuthToken(token: String): Boolean {
        // Placeholder for auth token verification
        return token.isNotEmpty()
    }
    
    private fun determineConnectionType(capabilities: NetworkCapabilities?): ConnectionType {
        return when {
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> ConnectionType.WIFI
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> ConnectionType.CELLULAR
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> ConnectionType.ETHERNET
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true -> ConnectionType.VPN
            else -> ConnectionType.UNKNOWN
        }
    }
    
    private fun estimateBandwidth(capabilities: NetworkCapabilities?): Long {
        return capabilities?.linkDownstreamBandwidthKbps?.toLong() ?: 0L
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        try {
            networkCallback?.let { connectivityManager?.unregisterNetworkCallback(it) }
            encryptionKeys.clear()
            scope.cancel()
            Log.d("NetworkPolicyController", "Network controller cleaned up")
        } catch (e: Exception) {
            Log.e("NetworkPolicyController", "Error during cleanup", e)
        }
    }
}

// Data classes for network policy management

data class NetworkState(
    val isConnected: Boolean = false,
    val connectionType: ConnectionType = ConnectionType.UNKNOWN,
    val isVPNActive: Boolean = false,
    val isMetered: Boolean = false,
    val isEmergencyCutoffActive: Boolean = false,
    val emergencyCutoffReason: String? = null,
    val emergencyCutoffTime: Long? = null,
    val lastUpdate: Long = System.currentTimeMillis()
)

data class NetworkPolicies(
    val allowedDomains: Set<String> = emptySet(),
    val blockedDomains: Set<String> = emptySet(),
    val requireVPNForNSFW: Boolean = false,
    val requireE2EEncryption: Boolean = false,
    val offlineOnlyForNSFW: Boolean = false,
    val maxBandwidthUsage: Long = Long.MAX_VALUE
)

data class NetworkStatusInfo(
    val isConnected: Boolean = false,
    val isWifi: Boolean = false,
    val isCellular: Boolean = false,
    val isVPN: Boolean = false,
    val isMetered: Boolean = false,
    val connectionType: ConnectionType = ConnectionType.UNKNOWN,
    val bandwidth: Long = 0L
)

sealed class NetworkRequestResult {
    object Allowed : NetworkRequestResult()
    data class Blocked(val reason: String) : NetworkRequestResult()
}

sealed class EncryptedConnectionResult {
    data class Success(
        val connection: SecureConnection,
        val encryptionKey: SecretKeySpec,
        val serviceId: String
    ) : EncryptedConnectionResult()
    
    data class Error(val message: String) : EncryptedConnectionResult()
}

sealed class EncryptionResult {
    data class Success(val data: ByteArray) : EncryptionResult()
    data class Error(val message: String) : EncryptionResult()
}

enum class NetworkPolicyMode {
    ALLOW_ALL,
    OFFLINE_ONLY,
    E2E_ENCRYPTED_ONLY,
    VPN_REQUIRED,
    BLOCKED
}

enum class ConnectionType {
    WIFI, CELLULAR, ETHERNET, VPN, UNKNOWN
}

enum class ContentType {
    GENERAL, NSFW_CONTENT, AI_GENERATION, USER_DATA
}

// Placeholder class for secure connections
data class SecureConnection(
    val url: String,
    val encryptionKey: SecretKeySpec
)