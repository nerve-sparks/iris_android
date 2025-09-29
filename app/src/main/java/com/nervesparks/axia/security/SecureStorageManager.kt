package com.nervesparks.axia.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom

/**
 * Secure Storage Manager for Axia
 * Handles encrypted storage of sensitive data like API keys
 * Uses Android's EncryptedSharedPreferences and AES-256 encryption
 */
class SecureStorageManager(private val context: Context) {
    
    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    
    private val encryptedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        "axia_secure_prefs",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    /**
     * Store API key securely
     */
    fun storeApiKey(provider: String, apiKey: String) {
        encryptedPrefs.edit()
            .putString("api_key_$provider", apiKey)
            .apply()
    }
    
    /**
     * Get API key for provider
     */
    fun getApiKey(provider: String): String? {
        return encryptedPrefs.getString("api_key_$provider", null)
    }
    
    /**
     * Remove API key
     */
    fun removeApiKey(provider: String) {
        encryptedPrefs.edit()
            .remove("api_key_$provider")
            .apply()
    }
    
    /**
     * Store configuration value
     */
    fun storeConfig(key: String, value: String) {
        encryptedPrefs.edit()
            .putString("config_$key", value)
            .apply()
    }
    
    /**
     * Get configuration value
     */
    fun getConfig(key: String): String? {
        return encryptedPrefs.getString("config_$key", null)
    }
    
    /**
     * Check if API key exists for provider
     */
    fun hasApiKey(provider: String): Boolean {
        return encryptedPrefs.contains("api_key_$provider")
    }
    
    /**
     * Get all configured providers
     */
    fun getConfiguredProviders(): List<String> {
        return encryptedPrefs.all.keys
            .filter { it.startsWith("api_key_") }
            .map { it.removePrefix("api_key_") }
    }
    
    /**
     * Clear all stored data
     */
    fun clearAll() {
        encryptedPrefs.edit().clear().apply()
    }
}