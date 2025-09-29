package com.nervesparks.axia.personality

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import java.security.SecureRandom
import android.util.Log

/**
 * Encryption Manager for NSFW Content
 * 
 * Provides strong encryption for adult content with:
 * - Hardware-backed key storage when available
 * - Separate encryption keys from main app
 * - Key rotation and management
 * - Emergency key deletion
 */
class EncryptionManager(private val context: Context) {
    
    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val NSFW_KEY_ALIAS = "axia_nsfw_encryption_key"
        private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 16
    }
    
    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }
    }
    
    /**
     * Initialize encryption system
     */
    fun initialize() {
        try {
            if (!keyStore.containsAlias(NSFW_KEY_ALIAS)) {
                generateEncryptionKey()
            }
            Log.d("EncryptionManager", "Encryption manager initialized")
        } catch (e: Exception) {
            Log.e("EncryptionManager", "Failed to initialize encryption manager", e)
            throw SecurityException("Could not initialize encryption for adult content")
        }
    }
    
    /**
     * Generate new encryption key in hardware keystore
     */
    private fun generateEncryptionKey() {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            NSFW_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(true) // Requires biometric/PIN
            .setUserAuthenticationValidityDurationSeconds(30 * 60) // 30 minutes
            .setRandomizedEncryptionRequired(true)
            .setIsStrongBoxBacked(true) // Use hardware security module if available
            .build()
        
        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
        
        Log.d("EncryptionManager", "Generated new encryption key for NSFW content")
    }
    
    /**
     * Encrypt data using hardware-backed key
     */
    fun encrypt(data: ByteArray): EncryptionResult {
        return try {
            if (!keyStore.containsAlias(NSFW_KEY_ALIAS)) {
                generateEncryptionKey()
            }
            
            val secretKey = keyStore.getKey(NSFW_KEY_ALIAS, null) as SecretKey
            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            
            val iv = cipher.iv
            val encryptedData = cipher.doFinal(data)
            
            EncryptionResult.Success(
                encryptedData = encryptedData,
                iv = iv,
                keyAlias = NSFW_KEY_ALIAS
            )
            
        } catch (e: Exception) {
            Log.e("EncryptionManager", "Failed to encrypt data", e)
            EncryptionResult.Error("Encryption failed: ${e.message}")
        }
    }
    
    /**
     * Decrypt data using hardware-backed key
     */
    fun decrypt(encryptedData: ByteArray, iv: ByteArray): DecryptionResult {
        return try {
            if (!keyStore.containsAlias(NSFW_KEY_ALIAS)) {
                return DecryptionResult.Error("Encryption key not found")
            }
            
            val secretKey = keyStore.getKey(NSFW_KEY_ALIAS, null) as SecretKey
            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            
            val spec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            
            val decryptedData = cipher.doFinal(encryptedData)
            
            DecryptionResult.Success(decryptedData)
            
        } catch (e: Exception) {
            Log.e("EncryptionManager", "Failed to decrypt data", e)
            DecryptionResult.Error("Decryption failed: ${e.message}")
        }
    }
    
    /**
     * Encrypt text data
     */
    fun encryptText(text: String): EncryptionResult {
        return encrypt(text.toByteArray(Charsets.UTF_8))
    }
    
    /**
     * Decrypt text data
     */
    fun decryptText(encryptedData: ByteArray, iv: ByteArray): String? {
        return when (val result = decrypt(encryptedData, iv)) {
            is DecryptionResult.Success -> result.data.toString(Charsets.UTF_8)
            is DecryptionResult.Error -> {
                Log.e("EncryptionManager", "Failed to decrypt text: ${result.message}")
                null
            }
        }
    }
    
    /**
     * Generate secure random data
     */
    fun generateSecureRandom(size: Int): ByteArray {
        val random = SecureRandom()
        val bytes = ByteArray(size)
        random.nextBytes(bytes)
        return bytes
    }
    
    /**
     * Generate secure random string
     */
    fun generateSecureRandomString(length: Int): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val random = SecureRandom()
        return (1..length)
            .map { chars[random.nextInt(chars.length)] }
            .joinToString("")
    }
    
    /**
     * Rotate encryption key (generate new key)
     */
    fun rotateEncryptionKey(): Boolean {
        return try {
            // Delete old key
            keyStore.deleteEntry(NSFW_KEY_ALIAS)
            
            // Generate new key
            generateEncryptionKey()
            
            Log.d("EncryptionManager", "Encryption key rotated successfully")
            true
            
        } catch (e: Exception) {
            Log.e("EncryptionManager", "Failed to rotate encryption key", e)
            false
        }
    }
    
    /**
     * Check if encryption key exists
     */
    fun hasEncryptionKey(): Boolean {
        return try {
            keyStore.containsAlias(NSFW_KEY_ALIAS)
        } catch (e: Exception) {
            Log.e("EncryptionManager", "Failed to check for encryption key", e)
            false
        }
    }
    
    /**
     * Clear all encryption keys (emergency function)
     */
    fun clearAllKeys() {
        try {
            if (keyStore.containsAlias(NSFW_KEY_ALIAS)) {
                keyStore.deleteEntry(NSFW_KEY_ALIAS)
                Log.w("EncryptionManager", "All encryption keys cleared")
            }
        } catch (e: Exception) {
            Log.e("EncryptionManager", "Failed to clear encryption keys", e)
        }
    }
    
    /**
     * Get key information (for debugging/monitoring)
     */
    fun getKeyInfo(): KeyInfo {
        return try {
            if (keyStore.containsAlias(NSFW_KEY_ALIAS)) {
                val key = keyStore.getKey(NSFW_KEY_ALIAS, null)
                KeyInfo(
                    exists = true,
                    algorithm = key.algorithm,
                    format = key.format ?: "Unknown"
                )
            } else {
                KeyInfo(exists = false)
            }
        } catch (e: Exception) {
            Log.e("EncryptionManager", "Failed to get key info", e)
            KeyInfo(exists = false, error = e.message)
        }
    }
    
    /**
     * Verify key integrity
     */
    fun verifyKeyIntegrity(): Boolean {
        return try {
            if (!hasEncryptionKey()) return false
            
            // Test encryption/decryption with sample data
            val testData = "integrity_test_${System.currentTimeMillis()}"
            val encrypted = encryptText(testData)
            
            if (encrypted is EncryptionResult.Success) {
                val decrypted = decryptText(encrypted.encryptedData, encrypted.iv)
                decrypted == testData
            } else {
                false
            }
            
        } catch (e: Exception) {
            Log.e("EncryptionManager", "Key integrity verification failed", e)
            false
        }
    }
    
    /**
     * Create backup of encryption parameters (not the key itself)
     */
    fun createKeyBackupInfo(): KeyBackupInfo {
        return KeyBackupInfo(
            keyAlias = NSFW_KEY_ALIAS,
            algorithm = "AES-256-GCM",
            createdAt = System.currentTimeMillis(),
            hardwareBacked = true // Assuming hardware keystore
        )
    }
}

// Sealed classes for encryption results

sealed class EncryptionResult {
    data class Success(
        val encryptedData: ByteArray,
        val iv: ByteArray,
        val keyAlias: String
    ) : EncryptionResult()
    
    data class Error(val message: String) : EncryptionResult()
}

sealed class DecryptionResult {
    data class Success(val data: ByteArray) : DecryptionResult()
    data class Error(val message: String) : DecryptionResult()
}

// Data classes for key management

data class KeyInfo(
    val exists: Boolean,
    val algorithm: String? = null,
    val format: String? = null,
    val error: String? = null
)

data class KeyBackupInfo(
    val keyAlias: String,
    val algorithm: String,
    val createdAt: Long,
    val hardwareBacked: Boolean
)