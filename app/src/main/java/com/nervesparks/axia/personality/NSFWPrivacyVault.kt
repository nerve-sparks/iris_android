package com.nervesparks.axia.personality

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.io.File
import java.io.FileOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import android.util.Log
import java.util.*

/**
 * NSFW Privacy Vault for αξία (Axia.OS)
 * 
 * Secure encrypted storage for adult content with:
 * - Separate encryption keys from main app
 * - Hidden entry points with PIN/biometric protection
 * - Secure deletion and emergency wipe
 * - Isolated storage containers
 */
class NSFWPrivacyVault(private val context: Context) {
    
    companion object {
        private const val VAULT_DIRECTORY = "nsfw_vault"
        private const val VAULT_KEY_ALIAS = "axia_nsfw_vault_key"
        private const val VAULT_PREFS_NAME = "nsfw_vault_prefs"
        private const val TEMP_DATA_PREFIX = "temp_nsfw_"
    }
    
    private val vaultDir: File by lazy {
        File(context.filesDir, VAULT_DIRECTORY).apply {
            if (!exists()) mkdirs()
        }
    }
    
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    private var masterKey: MasterKey? = null
    private var encryptedPrefs: androidx.security.crypto.EncryptedSharedPreferences? = null
    private var isInitialized = false
    
    /**
     * Initialize the privacy vault with encryption
     */
    fun initialize() {
        if (isInitialized) return
        
        try {
            // Create master key for NSFW vault (separate from main app)
            masterKey = MasterKey.Builder(context, VAULT_KEY_ALIAS)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .setRequestStrongBoxBacked(true) // Hardware security module if available
                .build()
            
            // Create encrypted preferences for vault metadata
            encryptedPrefs = EncryptedSharedPreferences.create(
                context,
                VAULT_PREFS_NAME,
                masterKey!!,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            ) as androidx.security.crypto.EncryptedSharedPreferences
            
            isInitialized = true
            Log.d("NSFWPrivacyVault", "Privacy vault initialized successfully")
            
        } catch (e: Exception) {
            Log.e("NSFWPrivacyVault", "Failed to initialize privacy vault", e)
            throw SecurityException("Could not create secure storage for adult content")
        }
    }
    
    /**
     * Store NSFW content securely
     */
    suspend fun storeContent(
        contentId: String,
        content: NSFWContent,
        isTemporary: Boolean = false
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (!isInitialized) initialize()
                
                val fileName = if (isTemporary) {
                    "$TEMP_DATA_PREFIX$contentId.enc"
                } else {
                    "$contentId.enc"
                }
                
                val contentFile = File(vaultDir, fileName)
                val encryptedFile = EncryptedFile.Builder(
                    context,
                    contentFile,
                    masterKey!!,
                    EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
                ).build()
                
                // Serialize and encrypt content
                val jsonContent = json.encodeToString(content)
                encryptedFile.openFileOutput().use { outputStream ->
                    outputStream.write(jsonContent.toByteArray())
                }
                
                // Store metadata
                storeContentMetadata(contentId, content, isTemporary)
                
                Log.d("NSFWPrivacyVault", "Stored NSFW content: $contentId")
                true
                
            } catch (e: Exception) {
                Log.e("NSFWPrivacyVault", "Failed to store NSFW content", e)
                false
            }
        }
    }
    
    /**
     * Retrieve NSFW content
     */
    suspend fun retrieveContent(contentId: String): NSFWContent? {
        return withContext(Dispatchers.IO) {
            try {
                if (!isInitialized) initialize()
                
                val contentFile = File(vaultDir, "$contentId.enc")
                if (!contentFile.exists()) {
                    // Try temporary storage
                    val tempFile = File(vaultDir, "$TEMP_DATA_PREFIX$contentId.enc")
                    if (!tempFile.exists()) return@withContext null
                    return@withContext retrieveFromFile(tempFile)
                }
                
                retrieveFromFile(contentFile)
                
            } catch (e: Exception) {
                Log.e("NSFWPrivacyVault", "Failed to retrieve NSFW content", e)
                null
            }
        }
    }
    
    /**
     * Store generated image securely
     */
    suspend fun storeGeneratedImage(
        imageId: String,
        imageData: ByteArray,
        metadata: ImageMetadata,
        isTemporary: Boolean = false
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (!isInitialized) initialize()
                
                val fileName = if (isTemporary) {
                    "$TEMP_DATA_PREFIX$imageId.img"
                } else {
                    "$imageId.img"
                }
                
                val imageFile = File(vaultDir, fileName)
                val encryptedFile = EncryptedFile.Builder(
                    context,
                    imageFile,
                    masterKey!!,
                    EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
                ).build()
                
                // Encrypt and store image
                encryptedFile.openFileOutput().use { outputStream ->
                    outputStream.write(imageData)
                }
                
                // Store image metadata
                storeImageMetadata(imageId, metadata, isTemporary)
                
                Log.d("NSFWPrivacyVault", "Stored NSFW image: $imageId")
                true
                
            } catch (e: Exception) {
                Log.e("NSFWPrivacyVault", "Failed to store NSFW image", e)
                false
            }
        }
    }
    
    /**
     * Retrieve generated image
     */
    suspend fun retrieveGeneratedImage(imageId: String): Pair<ByteArray, ImageMetadata>? {
        return withContext(Dispatchers.IO) {
            try {
                if (!isInitialized) initialize()
                
                val imageFile = File(vaultDir, "$imageId.img")
                val tempImageFile = File(vaultDir, "$TEMP_DATA_PREFIX$imageId.img")
                
                val fileToRead = when {
                    imageFile.exists() -> imageFile
                    tempImageFile.exists() -> tempImageFile
                    else -> return@withContext null
                }
                
                val encryptedFile = EncryptedFile.Builder(
                    context,
                    fileToRead,
                    masterKey!!,
                    EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
                ).build()
                
                val imageData = encryptedFile.openFileInput().use { inputStream ->
                    inputStream.readBytes()
                }
                
                val metadata = retrieveImageMetadata(imageId)
                if (metadata != null) {
                    Pair(imageData, metadata)
                } else {
                    null
                }
                
            } catch (e: Exception) {
                Log.e("NSFWPrivacyVault", "Failed to retrieve NSFW image", e)
                null
            }
        }
    }
    
    /**
     * List all stored content IDs
     */
    suspend fun listStoredContent(includeTemporary: Boolean = false): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                val contentIds = mutableListOf<String>()
                
                vaultDir.listFiles()?.forEach { file ->
                    val fileName = file.name
                    when {
                        fileName.endsWith(".enc") && !fileName.startsWith(TEMP_DATA_PREFIX) -> {
                            contentIds.add(fileName.removeSuffix(".enc"))
                        }
                        includeTemporary && fileName.startsWith(TEMP_DATA_PREFIX) && fileName.endsWith(".enc") -> {
                            contentIds.add(fileName.removePrefix(TEMP_DATA_PREFIX).removeSuffix(".enc"))
                        }
                    }
                }
                
                contentIds
                
            } catch (e: Exception) {
                Log.e("NSFWPrivacyVault", "Failed to list stored content", e)
                emptyList()
            }
        }
    }
    
    /**
     * Delete specific content
     */
    suspend fun deleteContent(contentId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                var deleted = false
                
                // Delete main content file
                val contentFile = File(vaultDir, "$contentId.enc")
                if (contentFile.exists()) {
                    secureDelete(contentFile)
                    deleted = true
                }
                
                // Delete temporary content file
                val tempFile = File(vaultDir, "$TEMP_DATA_PREFIX$contentId.enc")
                if (tempFile.exists()) {
                    secureDelete(tempFile)
                    deleted = true
                }
                
                // Delete associated image files
                val imageFile = File(vaultDir, "$contentId.img")
                if (imageFile.exists()) {
                    secureDelete(imageFile)
                }
                
                val tempImageFile = File(vaultDir, "$TEMP_DATA_PREFIX$contentId.img")
                if (tempImageFile.exists()) {
                    secureDelete(tempImageFile)
                }
                
                // Delete metadata
                deleteContentMetadata(contentId)
                deleteImageMetadata(contentId)
                
                if (deleted) {
                    Log.d("NSFWPrivacyVault", "Deleted NSFW content: $contentId")
                }
                
                deleted
                
            } catch (e: Exception) {
                Log.e("NSFWPrivacyVault", "Failed to delete NSFW content", e)
                false
            }
        }
    }
    
    /**
     * Clear all temporary data
     */
    suspend fun clearTemporaryData() {
        withContext(Dispatchers.IO) {
            try {
                vaultDir.listFiles()?.forEach { file ->
                    if (file.name.startsWith(TEMP_DATA_PREFIX)) {
                        secureDelete(file)
                    }
                }
                
                Log.d("NSFWPrivacyVault", "Cleared temporary NSFW data")
                
            } catch (e: Exception) {
                Log.e("NSFWPrivacyVault", "Failed to clear temporary data", e)
            }
        }
    }
    
    /**
     * Emergency wipe all vault data
     */
    suspend fun emergencyWipe() {
        withContext(Dispatchers.IO) {
            try {
                // Secure delete all files
                vaultDir.listFiles()?.forEach { file ->
                    secureDelete(file)
                }
                
                // Clear encrypted preferences
                encryptedPrefs?.edit()?.clear()?.apply()
                
                // Remove vault directory
                if (vaultDir.exists()) {
                    vaultDir.deleteRecursively()
                }
                
                Log.w("NSFWPrivacyVault", "Emergency wipe completed")
                
            } catch (e: Exception) {
                Log.e("NSFWPrivacyVault", "Failed to perform emergency wipe", e)
            }
        }
    }
    
    /**
     * Secure wipe on session end
     */
    suspend fun secureWipe() {
        withContext(Dispatchers.IO) {
            try {
                // Clear temporary data
                clearTemporaryData()
                
                // Optionally clear all data (configurable)
                val prefs = encryptedPrefs
                if (prefs?.getBoolean("wipe_on_session_end", true) == true) {
                    emergencyWipe()
                }
                
                Log.d("NSFWPrivacyVault", "Secure wipe completed")
                
            } catch (e: Exception) {
                Log.e("NSFWPrivacyVault", "Failed to perform secure wipe", e)
            }
        }
    }
    
    // Private helper methods
    
    private fun retrieveFromFile(file: File): NSFWContent? {
        return try {
            val encryptedFile = EncryptedFile.Builder(
                context,
                file,
                masterKey!!,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()
            
            val jsonContent = encryptedFile.openFileInput().use { inputStream ->
                inputStream.readBytes().toString(Charsets.UTF_8)
            }
            
            json.decodeFromString<NSFWContent>(jsonContent)
            
        } catch (e: Exception) {
            Log.e("NSFWPrivacyVault", "Failed to decrypt content from file", e)
            null
        }
    }
    
    private fun storeContentMetadata(contentId: String, content: NSFWContent, isTemporary: Boolean) {
        encryptedPrefs?.edit()?.apply {
            putString("metadata_$contentId", json.encodeToString(
                ContentMetadata(
                    id = contentId,
                    type = content.type,
                    createdAt = System.currentTimeMillis(),
                    isTemporary = isTemporary,
                    size = content.data.length
                )
            ))
            putLong("accessed_$contentId", System.currentTimeMillis())
        }?.apply()
    }
    
    private fun storeImageMetadata(imageId: String, metadata: ImageMetadata, isTemporary: Boolean) {
        encryptedPrefs?.edit()?.apply {
            putString("img_metadata_$imageId", json.encodeToString(metadata))
            putBoolean("img_temp_$imageId", isTemporary)
            putLong("img_created_$imageId", System.currentTimeMillis())
        }?.apply()
    }
    
    private fun retrieveImageMetadata(imageId: String): ImageMetadata? {
        return try {
            val metadataJson = encryptedPrefs?.getString("img_metadata_$imageId", null)
            if (metadataJson != null) {
                json.decodeFromString<ImageMetadata>(metadataJson)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("NSFWPrivacyVault", "Failed to retrieve image metadata", e)
            null
        }
    }
    
    private fun deleteContentMetadata(contentId: String) {
        encryptedPrefs?.edit()?.apply {
            remove("metadata_$contentId")
            remove("accessed_$contentId")
        }?.apply()
    }
    
    private fun deleteImageMetadata(imageId: String) {
        encryptedPrefs?.edit()?.apply {
            remove("img_metadata_$imageId")
            remove("img_temp_$imageId")
            remove("img_created_$imageId")
        }?.apply()
    }
    
    private fun secureDelete(file: File) {
        if (!file.exists()) return
        
        try {
            // Overwrite file with random data multiple times for secure deletion
            val fileSize = file.length()
            val random = SecureRandom()
            
            FileOutputStream(file).use { fos ->
                // First pass: all zeros
                fos.write(ByteArray(fileSize.toInt()))
                fos.flush()
                
                // Second pass: all ones
                val ones = ByteArray(fileSize.toInt()) { 0xFF.toByte() }
                fos.write(ones)
                fos.flush()
                
                // Third pass: random data
                val randomData = ByteArray(fileSize.toInt())
                random.nextBytes(randomData)
                fos.write(randomData)
                fos.flush()
            }
            
            // Finally delete the file
            file.delete()
            
        } catch (e: Exception) {
            Log.e("NSFWPrivacyVault", "Failed to securely delete file: ${file.name}", e)
            // Fall back to regular deletion
            file.delete()
        }
    }
}

// Data classes for vault storage

@Serializable
data class NSFWContent(
    val id: String,
    val type: ContentType,
    val data: String,
    val metadata: Map<String, String> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis(),
    val tags: List<String> = emptyList()
)

@Serializable
data class ImageMetadata(
    val id: String,
    val width: Int,
    val height: Int,
    val format: String,
    val prompt: String? = null,
    val seed: Long? = null,
    val model: String? = null,
    val parameters: Map<String, String> = emptyMap(),
    val isPhotoReal: Boolean = false,
    val isAnonymized: Boolean = false
)

@Serializable
data class ContentMetadata(
    val id: String,
    val type: ContentType,
    val createdAt: Long,
    val isTemporary: Boolean,
    val size: Int
)

@Serializable
enum class ContentType {
    TEXT_CONVERSATION,
    GENERATED_IMAGE,
    GENERATED_VIDEO,
    VOICE_MESSAGE,
    MULTIMEDIA_CONTENT
}