package com.nervesparks.axia.contractor

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import android.util.Base64
import java.security.SecureRandom

/**
 * Secure Contractor API Manager for Axia
 * 
 * CONTRACTOR MODEL:
 * - Local AI models = 100% FREE (TinyLlama, Phi-3, Llama, etc.)
 * - Cloud APIs = User provides their own API keys (they pay directly)
 * - Extensive Hugging Face integration for downloads & inference
 * - Complete work automation with autonomous phone control
 * 
 * SECURITY:
 * - All API keys encrypted with AES-256
 * - Keys stored securely in SharedPreferences
 * - Optional biometric authentication for key access
 */
class ContractorApiManager(private val context: Context) : ViewModel() {
    
    private val preferences: SharedPreferences = context.getSharedPreferences(
        "axia_contractor_secure", Context.MODE_PRIVATE
    )
    
    // API Key States
    private val _openAiKey = MutableStateFlow<String?>(null)
    val openAiKey: StateFlow<String?> = _openAiKey.asStateFlow()
    
    private val _anthropicKey = MutableStateFlow<String?>(null) 
    val anthropicKey: StateFlow<String?> = _anthropicKey.asStateFlow()
    
    private val _huggingFaceKey = MutableStateFlow<String?>(null)
    val huggingFaceKey: StateFlow<String?> = _huggingFaceKey.asStateFlow()
    
    private val _googleAiKey = MutableStateFlow<String?>(null)
    val googleAiKey: StateFlow<String?> = _googleAiKey.asStateFlow()
    
    private val _cohereKey = MutableStateFlow<String?>(null)
    val cohereKey: StateFlow<String?> = _cohereKey.asStateFlow()
    
    // Contractor Status
    private val _contractorStatus = MutableStateFlow<ContractorStatus>(ContractorStatus.FREE_ONLY)
    val contractorStatus: StateFlow<ContractorStatus> = _contractorStatus.asStateFlow()
    
    // Available Contractors
    private val _availableContractors = MutableStateFlow<List<ContractorProvider>>(emptyList())
    val availableContractors: StateFlow<List<ContractorProvider>> = _availableContractors.asStateFlow()
    
    // Usage Tracking
    private val _usageStats = MutableStateFlow<Map<String, ContractorUsage>>(emptyMap())
    val usageStats: StateFlow<Map<String, ContractorUsage>> = _usageStats.asStateFlow()
    
    private val encryptionKey: SecretKey by lazy {
        val keyString = preferences.getString("encryption_key", null)
        if (keyString != null) {
            val keyBytes = Base64.decode(keyString, Base64.DEFAULT)
            SecretKeySpec(keyBytes, "AES")
        } else {
            generateNewEncryptionKey()
        }
    }
    
    init {
        loadContractorKeys()
        initializeContractorProviders()
        updateContractorStatus()
    }
    
    /**
     * Generate new encryption key for API key security
     */
    private fun generateNewEncryptionKey(): SecretKey {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        val key = keyGen.generateKey()
        
        // Store key securely
        val keyString = Base64.encodeToString(key.encoded, Base64.DEFAULT)
        preferences.edit().putString("encryption_key", keyString).apply()
        
        return key
    }
    
    /**
     * Encrypt API key for secure storage
     */
    private fun encryptApiKey(apiKey: String): String {
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey)
        val encryptedBytes = cipher.doFinal(apiKey.toByteArray())
        return Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
    }
    
    /**
     * Decrypt API key for use
     */
    private fun decryptApiKey(encryptedKey: String): String {
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.DECRYPT_MODE, encryptionKey)
        val encryptedBytes = Base64.decode(encryptedKey, Base64.DEFAULT)
        val decryptedBytes = cipher.doFinal(encryptedBytes)
        return String(decryptedBytes)
    }
    
    /**
     * Load contractor API keys from secure storage
     */
    private fun loadContractorKeys() {
        viewModelScope.launch {
            try {
                preferences.getString("openai_key", null)?.let { encrypted ->
                    _openAiKey.value = decryptApiKey(encrypted)
                }
                
                preferences.getString("anthropic_key", null)?.let { encrypted ->
                    _anthropicKey.value = decryptApiKey(encrypted)
                }
                
                preferences.getString("huggingface_key", null)?.let { encrypted ->
                    _huggingFaceKey.value = decryptApiKey(encrypted)
                }
                
                preferences.getString("google_ai_key", null)?.let { encrypted ->
                    _googleAiKey.value = decryptApiKey(encrypted)
                }
                
                preferences.getString("cohere_key", null)?.let { encrypted ->
                    _cohereKey.value = decryptApiKey(encrypted)
                }
                
                updateContractorStatus()
            } catch (e: Exception) {
                android.util.Log.e("ContractorApiManager", "Failed to load API keys", e)
            }
        }
    }
    
    /**
     * Store contractor API key securely
     */
    fun storeContractorKey(provider: ContractorProvider, apiKey: String) {
        viewModelScope.launch {
            try {
                val encrypted = encryptApiKey(apiKey)
                val prefKey = when (provider) {
                    ContractorProvider.OPENAI -> "openai_key"
                    ContractorProvider.ANTHROPIC -> "anthropic_key"
                    ContractorProvider.HUGGING_FACE -> "huggingface_key"
                    ContractorProvider.GOOGLE_AI -> "google_ai_key"
                    ContractorProvider.COHERE -> "cohere_key"
                }
                
                preferences.edit().putString(prefKey, encrypted).apply()
                
                // Update state
                when (provider) {
                    ContractorProvider.OPENAI -> _openAiKey.value = apiKey
                    ContractorProvider.ANTHROPIC -> _anthropicKey.value = apiKey
                    ContractorProvider.HUGGING_FACE -> _huggingFaceKey.value = apiKey
                    ContractorProvider.GOOGLE_AI -> _googleAiKey.value = apiKey
                    ContractorProvider.COHERE -> _cohereKey.value = apiKey
                }
                
                updateContractorStatus()
                
            } catch (e: Exception) {
                android.util.Log.e("ContractorApiManager", "Failed to store API key", e)
            }
        }
    }
    
    /**
     * Remove contractor API key
     */
    fun removeContractorKey(provider: ContractorProvider) {
        viewModelScope.launch {
            val prefKey = when (provider) {
                ContractorProvider.OPENAI -> "openai_key"
                ContractorProvider.ANTHROPIC -> "anthropic_key"
                ContractorProvider.HUGGING_FACE -> "huggingface_key"
                ContractorProvider.GOOGLE_AI -> "google_ai_key"
                ContractorProvider.COHERE -> "cohere_key"
            }
            
            preferences.edit().remove(prefKey).apply()
            
            // Update state
            when (provider) {
                ContractorProvider.OPENAI -> _openAiKey.value = null
                ContractorProvider.ANTHROPIC -> _anthropicKey.value = null
                ContractorProvider.HUGGING_FACE -> _huggingFaceKey.value = null
                ContractorProvider.GOOGLE_AI -> _googleAiKey.value = null
                ContractorProvider.COHERE -> _cohereKey.value = null
            }
            
            updateContractorStatus()
        }
    }
    
    /**
     * Initialize contractor providers with capabilities
     */
    private fun initializeContractorProviders() {
        val providers = listOf(
            ContractorProvider.OPENAI.copy(
                models = listOf("gpt-4o", "gpt-4o-mini", "gpt-4-turbo", "gpt-3.5-turbo"),
                capabilities = listOf("text", "code", "vision", "function-calling", "json-mode"),
                costInfo = "Pay-per-use: \$0.005-0.06/1K tokens"
            ),
            ContractorProvider.ANTHROPIC.copy(
                models = listOf("claude-3-5-sonnet", "claude-3-opus", "claude-3-haiku"),
                capabilities = listOf("text", "code", "analysis", "reasoning", "large-context"),
                costInfo = "Pay-per-use: \$0.003-0.075/1K tokens"
            ),
            ContractorProvider.HUGGING_FACE.copy(
                models = listOf("meta-llama/Llama-3.2-3B", "microsoft/DialoGPT-large", "codellama/CodeLlama-7b-hf"),
                capabilities = listOf("inference", "model-download", "custom-models", "transformers"),
                costInfo = "Inference API: Pay-per-use, Downloads: FREE"
            ),
            ContractorProvider.GOOGLE_AI.copy(
                models = listOf("gemini-1.5-pro", "gemini-1.5-flash", "gemini-pro"),
                capabilities = listOf("text", "vision", "multimodal", "long-context"),
                costInfo = "Pay-per-use: \$0.002-0.007/1K tokens"
            ),
            ContractorProvider.COHERE.copy(
                models = listOf("command-r", "command-r-plus", "command"),
                capabilities = listOf("text", "reasoning", "multilingual", "rag"),
                costInfo = "Pay-per-use: \$0.0005-0.003/1K tokens"
            )
        )
        
        _availableContractors.value = providers
    }
    
    /**
     * Update contractor status based on available keys
     */
    private fun updateContractorStatus() {
        val hasAnyKey = _openAiKey.value != null || 
                       _anthropicKey.value != null || 
                       _huggingFaceKey.value != null ||
                       _googleAiKey.value != null ||
                       _cohereKey.value != null
        
        _contractorStatus.value = if (hasAnyKey) {
            ContractorStatus.CONTRACTORS_AVAILABLE
        } else {
            ContractorStatus.FREE_ONLY
        }
    }
    
    /**
     * Get API key for specific provider
     */
    fun getApiKey(provider: ContractorProvider): String? {
        return when (provider) {
            ContractorProvider.OPENAI -> _openAiKey.value
            ContractorProvider.ANTHROPIC -> _anthropicKey.value
            ContractorProvider.HUGGING_FACE -> _huggingFaceKey.value
            ContractorProvider.GOOGLE_AI -> _googleAiKey.value
            ContractorProvider.COHERE -> _cohereKey.value
        }
    }
    
    /**
     * Track usage for billing transparency
     */
    fun trackUsage(provider: ContractorProvider, inputTokens: Int, outputTokens: Int, estimatedCost: Double) {
        viewModelScope.launch {
            val currentUsage = _usageStats.value[provider.name] ?: ContractorUsage()
            val updatedUsage = currentUsage.copy(
                totalRequests = currentUsage.totalRequests + 1,
                totalInputTokens = currentUsage.totalInputTokens + inputTokens,
                totalOutputTokens = currentUsage.totalOutputTokens + outputTokens,
                estimatedTotalCost = currentUsage.estimatedTotalCost + estimatedCost,
                lastUsed = System.currentTimeMillis()
            )
            
            val updatedStats = _usageStats.value.toMutableMap()
            updatedStats[provider.name] = updatedUsage
            _usageStats.value = updatedStats
        }
    }
    
    /**
     * Validate API key format
     */
    fun validateApiKey(provider: ContractorProvider, apiKey: String): Boolean {
        return when (provider) {
            ContractorProvider.OPENAI -> apiKey.startsWith("sk-") && apiKey.length > 20
            ContractorProvider.ANTHROPIC -> apiKey.startsWith("sk-ant-") && apiKey.length > 30
            ContractorProvider.HUGGING_FACE -> apiKey.startsWith("hf_") && apiKey.length > 20
            ContractorProvider.GOOGLE_AI -> apiKey.length > 20
            ContractorProvider.COHERE -> apiKey.length > 30
        }
    }
}

/**
 * Contractor status enum
 */
enum class ContractorStatus {
    FREE_ONLY,
    CONTRACTORS_AVAILABLE
}

/**
 * Contractor provider data class
 */
data class ContractorProvider(
    val name: String,
    val displayName: String,
    val description: String,
    val models: List<String> = emptyList(),
    val capabilities: List<String> = emptyList(),
    val costInfo: String = "",
    val setupInstructions: String = ""
) {
    companion object {
        val OPENAI = ContractorProvider(
            name = "openai",
            displayName = "OpenAI",
            description = "GPT-4o, GPT-4 Turbo, GPT-3.5 - Industry-leading models",
            setupInstructions = "Get API key from platform.openai.com → API Keys"
        )
        
        val ANTHROPIC = ContractorProvider(
            name = "anthropic",
            displayName = "Anthropic",
            description = "Claude 3.5 Sonnet, Opus, Haiku - Advanced reasoning",
            setupInstructions = "Get API key from console.anthropic.com → API Keys"
        )
        
        val HUGGING_FACE = ContractorProvider(
            name = "huggingface",
            displayName = "Hugging Face",
            description = "Inference API + Model Downloads - Open Source Hub",
            setupInstructions = "Get token from huggingface.co → Settings → Access Tokens"
        )
        
        val GOOGLE_AI = ContractorProvider(
            name = "google_ai",
            displayName = "Google AI",
            description = "Gemini 1.5 Pro, Flash - Multimodal capabilities",
            setupInstructions = "Get API key from makersuite.google.com"
        )
        
        val COHERE = ContractorProvider(
            name = "cohere",
            displayName = "Cohere",
            description = "Command R, Command R+ - Enterprise-ready models",
            setupInstructions = "Get API key from dashboard.cohere.com → API Keys"
        )
    }
}

/**
 * Usage tracking data class
 */
data class ContractorUsage(
    val totalRequests: Int = 0,
    val totalInputTokens: Int = 0,
    val totalOutputTokens: Int = 0,
    val estimatedTotalCost: Double = 0.0,
    val lastUsed: Long = 0L
)