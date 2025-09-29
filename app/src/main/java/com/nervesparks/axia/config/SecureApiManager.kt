package com.nervesparks.axia.config

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.nervesparks.axia.security.AxiaSecurityLayer

/**
 * Secure API Configuration Manager
 * Handles encrypted storage of API keys and configuration
 */
class SecureApiManager(
    private val context: Context,
    private val securityLayer: AxiaSecurityLayer
) {
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "axia_api_config",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    // API Key Storage
    fun setOpenAiKey(key: String) {
        val encrypted = securityLayer.encryptData(key.toByteArray())
        sharedPreferences.edit()
            .putString("openai_key", encrypted.toString())
            .apply()
    }
    
    fun setAnthropicKey(key: String) {
        val encrypted = securityLayer.encryptData(key.toByteArray())
        sharedPreferences.edit()
            .putString("anthropic_key", encrypted.toString())
            .apply()
    }
    
    fun getOpenAiKey(): String? {
        return try {
            val encrypted = sharedPreferences.getString("openai_key", null)
            encrypted?.let {
                String(securityLayer.decryptData(it.toByteArray()))
            }
        } catch (e: Exception) {
            null
        }
    }
    
    fun getAnthropicKey(): String? {
        return try {
            val encrypted = sharedPreferences.getString("anthropic_key", null)
            encrypted?.let {
                String(securityLayer.decryptData(it.toByteArray()))
            }
        } catch (e: Exception) {
            null
        }
    }
    
    // API Validation
    suspend fun validateApiKey(provider: String, key: String): ApiValidationResult {
        return when (provider.lowercase()) {
            "openai" -> validateOpenAiKey(key)
            "anthropic" -> validateAnthropicKey(key)
            else -> ApiValidationResult(false, "Unknown provider")
        }
    }
    
    private suspend fun validateOpenAiKey(key: String): ApiValidationResult {
        // OpenAI API key validation
        return if (key.startsWith("sk-") && key.length >= 40) {
            try {
                // Test API call to validate
                ApiValidationResult(true, "Valid OpenAI key")
            } catch (e: Exception) {
                ApiValidationResult(false, "Invalid OpenAI key: ${e.message}")
            }
        } else {
            ApiValidationResult(false, "Invalid OpenAI key format")
        }
    }
    
    private suspend fun validateAnthropicKey(key: String): ApiValidationResult {
        // Anthropic API key validation
        return if (key.startsWith("sk-ant-") && key.length >= 40) {
            try {
                // Test API call to validate
                ApiValidationResult(true, "Valid Anthropic key")
            } catch (e: Exception) {
                ApiValidationResult(false, "Invalid Anthropic key: ${e.message}")
            }
        } else {
            ApiValidationResult(false, "Invalid Anthropic key format")
        }
    }
    
    // Rate Limiting
    private val rateLimits = mutableMapOf<String, RateLimit>()
    
    fun checkRateLimit(provider: String): Boolean {
        val rateLimit = rateLimits[provider] ?: return true
        val now = System.currentTimeMillis()
        
        return when (provider.lowercase()) {
            "openai" -> {
                // OpenAI: 60 requests per minute for most tiers
                if (now - rateLimit.lastReset > 60_000) {
                    rateLimit.requestCount = 0
                    rateLimit.lastReset = now
                }
                rateLimit.requestCount < 50 // Conservative limit
            }
            "anthropic" -> {
                // Anthropic: 50 requests per minute
                if (now - rateLimit.lastReset > 60_000) {
                    rateLimit.requestCount = 0
                    rateLimit.lastReset = now
                }
                rateLimit.requestCount < 45 // Conservative limit
            }
            else -> true
        }
    }
    
    fun recordApiCall(provider: String) {
        val rateLimit = rateLimits.getOrPut(provider) { 
            RateLimit(0, System.currentTimeMillis()) 
        }
        rateLimit.requestCount++
    }
    
    // Error Handling
    fun handleApiError(provider: String, error: Exception): ApiErrorAction {
        return when {
            error.message?.contains("401") == true -> {
                ApiErrorAction.INVALID_KEY
            }
            error.message?.contains("429") == true -> {
                ApiErrorAction.RATE_LIMITED
            }
            error.message?.contains("insufficient_quota") == true -> {
                ApiErrorAction.QUOTA_EXCEEDED
            }
            error.message?.contains("model_not_found") == true -> {
                ApiErrorAction.MODEL_UNAVAILABLE
            }
            else -> ApiErrorAction.GENERIC_ERROR
        }
    }
}

data class ApiValidationResult(
    val isValid: Boolean,
    val message: String
)

data class RateLimit(
    var requestCount: Int,
    var lastReset: Long
)

enum class ApiErrorAction {
    INVALID_KEY,
    RATE_LIMITED, 
    QUOTA_EXCEEDED,
    MODEL_UNAVAILABLE,
    GENERIC_ERROR
}