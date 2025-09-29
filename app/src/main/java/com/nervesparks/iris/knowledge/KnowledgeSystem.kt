package com.nervesparks.iris.knowledge

import android.content.Context
import android.util.Log
import androidx.compose.runtime.*
import androidx.room.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.*
import java.util.zip.ZipInputStream
import java.net.URL
import java.security.MessageDigest

/**
 * Advanced Knowledge Base System
 * Allows downloading and training with specialized datasets
 * Legal, Medical, Technical, etc.
 */

@Entity(tableName = "knowledge_datasets")
data class KnowledgeDataset(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val category: DatasetCategory,
    val downloadUrl: String,
    val fileSize: Long,
    val checksum: String,
    val version: String,
    val isDownloaded: Boolean = false,
    val isActive: Boolean = false,
    val downloadPath: String = "",
    val indexedTokens: Long = 0,
    val processingStatus: ProcessingStatus = ProcessingStatus.PENDING,
    val lastUpdated: Long = System.currentTimeMillis(),
    val weight: Float = 1.0f, // Training weight
    val metadata: String = "{}" // JSON metadata
)

@Entity(tableName = "knowledge_chunks")
data class KnowledgeChunk(
    @PrimaryKey val id: String,
    val datasetId: String,
    val content: String,
    val embedding: String, // Vector embedding
    val importance: Float,
    val chunkIndex: Int,
    val tokens: Int,
    val category: String,
    val metadata: String = "{}"
)

@Entity(tableName = "dataset_usage_stats")
data class DatasetUsageStats(
    @PrimaryKey val datasetId: String,
    val queryCount: Long,
    val successfulMatches: Long,
    val averageRelevance: Float,
    val lastUsed: Long,
    val totalTokensGenerated: Long
)

enum class DatasetCategory {
    LEGAL,
    MEDICAL,
    TECHNICAL,
    BUSINESS,
    SCIENCE,
    EDUCATION,
    CUSTOM,
    CODING,
    FINANCE,
    MARKETING
}

enum class ProcessingStatus {
    PENDING,
    DOWNLOADING,
    PROCESSING,
    INDEXING,
    COMPLETE,
    ERROR
}

@Dao
interface KnowledgeDao {
    @Query("SELECT * FROM knowledge_datasets ORDER BY isActive DESC, name ASC")
    fun getAllDatasets(): Flow<List<KnowledgeDataset>>
    
    @Query("SELECT * FROM knowledge_datasets WHERE isActive = 1")
    fun getActiveDatasets(): Flow<List<KnowledgeDataset>>
    
    @Query("SELECT * FROM knowledge_datasets WHERE category = :category")
    fun getDatasetsByCategory(category: DatasetCategory): Flow<List<KnowledgeDataset>>
    
    @Query("SELECT * FROM knowledge_chunks WHERE datasetId IN (:datasetIds) AND content LIKE '%' || :query || '%' ORDER BY importance DESC LIMIT :limit")
    fun searchKnowledge(query: String, datasetIds: List<String>, limit: Int = 20): Flow<List<KnowledgeChunk>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDataset(dataset: KnowledgeDataset)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunk(chunk: KnowledgeChunk)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunks(chunks: List<KnowledgeChunk>)
    
    @Update
    suspend fun updateDataset(dataset: KnowledgeDataset)
    
    @Query("UPDATE knowledge_datasets SET isActive = :isActive WHERE id = :datasetId")
    suspend fun setDatasetActive(datasetId: String, isActive: Boolean)
    
    @Query("DELETE FROM knowledge_datasets WHERE id = :datasetId")
    suspend fun deleteDataset(datasetId: String)
    
    @Query("DELETE FROM knowledge_chunks WHERE datasetId = :datasetId")
    suspend fun deleteChunksForDataset(datasetId: String)
}

class KnowledgeBaseManager(
    private val context: Context,
    private val knowledgeDao: KnowledgeDao
) {
    companion object {
        private const val TAG = "KnowledgeBaseManager"
        private const val KNOWLEDGE_DIR = "knowledge_base"
        private const val CHUNK_SIZE = 1024 // tokens per chunk
        
        // Predefined dataset sources
        val AVAILABLE_DATASETS = listOf(
            KnowledgeDataset(
                id = "legal_procedures_2024",
                name = "Legal Procedures & Law",
                description = "Comprehensive legal procedures, case law, and legal documentation standards",
                category = DatasetCategory.LEGAL,
                downloadUrl = "https://example.com/datasets/legal_procedures.zip",
                fileSize = 250_000_000, // 250MB
                checksum = "sha256hash",
                version = "2024.1"
            ),
            KnowledgeDataset(
                id = "medical_coding_icd10",
                name = "Medical Coding ICD-10",
                description = "Medical coding standards, ICD-10 codes, and healthcare documentation",
                category = DatasetCategory.MEDICAL,
                downloadUrl = "https://example.com/datasets/medical_coding.zip",
                fileSize = 180_000_000,
                checksum = "sha256hash",
                version = "2024.2"
            ),
            KnowledgeDataset(
                id = "programming_patterns",
                name = "Programming Patterns & Best Practices",
                description = "Software engineering patterns, algorithms, and coding best practices",
                category = DatasetCategory.CODING,
                downloadUrl = "https://example.com/datasets/programming.zip",
                fileSize = 320_000_000,
                checksum = "sha256hash",
                version = "2024.1"
            ),
            KnowledgeDataset(
                id = "business_finance",
                name = "Business & Finance Knowledge",
                description = "Business strategy, financial analysis, and corporate procedures",
                category = DatasetCategory.FINANCE,
                downloadUrl = "https://example.com/datasets/business_finance.zip",
                fileSize = 200_000_000,
                checksum = "sha256hash",
                version = "2024.1"
            )
        )
    }
    
    private val knowledgeDir = File(context.filesDir, KNOWLEDGE_DIR)
    private val downloadJobs = mutableMapOf<String, Job>()
    
    init {
        knowledgeDir.mkdirs()
    }
    
    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress.asStateFlow()
    
    /**
     * Get available datasets for download
     */
    fun getAvailableDatasets(): List<KnowledgeDataset> {
        return AVAILABLE_DATASETS
    }
    
    /**
     * Download and process a dataset
     */
    suspend fun downloadDataset(dataset: KnowledgeDataset): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting download for dataset: ${dataset.name}")
                
                // Update status to downloading
                val downloadingDataset = dataset.copy(
                    processingStatus = ProcessingStatus.DOWNLOADING,
                    isDownloaded = false
                )
                knowledgeDao.insertDataset(downloadingDataset)
                
                // Start download job
                val downloadJob = launch {
                    downloadDatasetFile(dataset)
                }
                downloadJobs[dataset.id] = downloadJob
                
                downloadJob.join()
                
                // Process the downloaded file
                processDataset(dataset)
                
                Result.success("Dataset ${dataset.name} downloaded and processed successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading dataset: ${dataset.name}", e)
                
                // Update status to error
                val errorDataset = dataset.copy(
                    processingStatus = ProcessingStatus.ERROR
                )
                knowledgeDao.updateDataset(errorDataset)
                
                Result.failure(e)
            }
        }
    }
    
    private suspend fun downloadDatasetFile(dataset: KnowledgeDataset) {
        withContext(Dispatchers.IO) {
            val outputFile = File(knowledgeDir, "${dataset.id}.zip")
            
            try {
                val url = URL(dataset.downloadUrl)
                val connection = url.openConnection()
                val contentLength = connection.contentLength
                
                connection.getInputStream().use { input ->
                    outputFile.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var downloaded = 0L
                        var bytesRead: Int
                        
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloaded += bytesRead
                            
                            // Update progress
                            val progress = if (contentLength > 0) {
                                downloaded.toFloat() / contentLength
                            } else 0f
                            
                            _downloadProgress.value = _downloadProgress.value + (dataset.id to progress)
                        }
                    }
                }
                
                // Verify checksum
                val actualChecksum = calculateChecksum(outputFile)
                if (actualChecksum != dataset.checksum) {
                    Log.w(TAG, "Checksum mismatch for ${dataset.name}")
                }
                
                Log.d(TAG, "Download completed for ${dataset.name}")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to download ${dataset.name}", e)
                throw e
            }
        }
    }
    
    private suspend fun processDataset(dataset: KnowledgeDataset) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Processing dataset: ${dataset.name}")
                
                // Update status to processing
                val processingDataset = dataset.copy(
                    processingStatus = ProcessingStatus.PROCESSING,
                    downloadPath = File(knowledgeDir, "${dataset.id}.zip").absolutePath
                )
                knowledgeDao.updateDataset(processingDataset)
                
                // Extract and process files
                val zipFile = File(knowledgeDir, "${dataset.id}.zip")
                val extractDir = File(knowledgeDir, dataset.id)
                extractDir.mkdirs()
                
                // Extract ZIP
                extractZipFile(zipFile, extractDir)
                
                // Process text files and create chunks
                val chunks = processTextFiles(extractDir, dataset.id)
                
                // Insert chunks in batches
                chunks.chunked(100).forEach { batch ->
                    knowledgeDao.insertChunks(batch)
                }
                
                // Update dataset status
                val completeDataset = dataset.copy(
                    processingStatus = ProcessingStatus.COMPLETE,
                    isDownloaded = true,
                    indexedTokens = chunks.sumOf { it.tokens }.toLong(),
                    downloadPath = extractDir.absolutePath
                )
                knowledgeDao.updateDataset(completeDataset)
                
                Log.d(TAG, "Successfully processed ${chunks.size} chunks for ${dataset.name}")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error processing dataset: ${dataset.name}", e)
                throw e
            }
        }
    }
    
    private fun extractZipFile(zipFile: File, extractDir: File) {
        ZipInputStream(zipFile.inputStream().buffered()).use { zipIn ->
            var entry = zipIn.nextEntry
            
            while (entry != null) {
                val file = File(extractDir, entry.name)
                
                if (entry.isDirectory) {
                    file.mkdirs()
                } else {
                    file.parentFile?.mkdirs()
                    file.outputStream().use { output ->
                        zipIn.copyTo(output)
                    }
                }
                
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }
    }
    
    private fun processTextFiles(directory: File, datasetId: String): List<KnowledgeChunk> {
        val chunks = mutableListOf<KnowledgeChunk>()
        var chunkIndex = 0
        
        directory.walkTopDown()
            .filter { it.isFile && (it.extension in listOf("txt", "md", "json", "csv")) }
            .forEach { file ->
                try {
                    val content = file.readText(Charsets.UTF_8)
                    val fileChunks = createChunksFromText(content, datasetId, chunkIndex)
                    chunks.addAll(fileChunks)
                    chunkIndex += fileChunks.size
                } catch (e: Exception) {
                    Log.w(TAG, "Could not process file: ${file.name}", e)
                }
            }
        
        return chunks
    }
    
    private fun createChunksFromText(text: String, datasetId: String, startIndex: Int): List<KnowledgeChunk> {
        val chunks = mutableListOf<KnowledgeChunk>()
        val words = text.split(Regex("\\s+"))
        
        // Rough token estimation (1 token ≈ 0.75 words)
        val wordsPerChunk = (CHUNK_SIZE * 0.75).toInt()
        
        words.chunked(wordsPerChunk).forEachIndexed { index, wordChunk ->
            val content = wordChunk.joinToString(" ")
            val tokens = estimateTokens(content)
            
            val chunk = KnowledgeChunk(
                id = "${datasetId}_chunk_${startIndex + index}",
                datasetId = datasetId,
                content = content,
                embedding = generateSimpleEmbedding(content),
                importance = calculateImportance(content),
                chunkIndex = startIndex + index,
                tokens = tokens,
                category = extractCategory(content)
            )
            
            chunks.add(chunk)
        }
        
        return chunks
    }
    
    private fun estimateTokens(text: String): Int {
        // Simple token estimation: ~0.75 tokens per word
        return (text.split(Regex("\\s+")).size * 0.75).toInt()
    }
    
    private fun generateSimpleEmbedding(text: String): String {
        // Simplified embedding using text hash and key terms
        val keyTerms = text.lowercase()
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length > 3 }
            .distinct()
            .take(20)
        
        return keyTerms.joinToString(",")
    }
    
    private fun calculateImportance(content: String): Float {
        val importantTerms = listOf(
            "definition", "procedure", "law", "code", "regulation", "standard",
            "requirement", "specification", "guideline", "principle", "method"
        )
        
        val contentLower = content.lowercase()
        val importanceScore = importantTerms.count { term ->
            contentLower.contains(term)
        }.toFloat() / importantTerms.size
        
        return (importanceScore * 0.6f + 0.4f).coerceIn(0.1f, 1.0f)
    }
    
    private fun extractCategory(content: String): String {
        val contentLower = content.lowercase()
        
        return when {
            contentLower.contains(Regex("\\b(law|legal|court|attorney|judge)\\b")) -> "legal"
            contentLower.contains(Regex("\\b(medical|health|patient|diagnosis)\\b")) -> "medical"
            contentLower.contains(Regex("\\b(code|programming|software|algorithm)\\b")) -> "technical"
            contentLower.contains(Regex("\\b(business|finance|market|profit)\\b")) -> "business"
            else -> "general"
        }
    }
    
    private fun calculateChecksum(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Search knowledge base for relevant information
     */
    suspend fun searchKnowledge(
        query: String,
        activeDatasets: List<String>? = null,
        limit: Int = 10
    ): List<KnowledgeChunk> {
        return withContext(Dispatchers.IO) {
            val datasets = activeDatasets ?: knowledgeDao.getActiveDatasets().first().map { it.id }
            knowledgeDao.searchKnowledge(query, datasets, limit).first()
        }
    }
    
    /**
     * Get context-aware knowledge for AI responses
     */
    suspend fun getRelevantContext(query: String, maxTokens: Int = 2000): String {
        val relevantChunks = searchKnowledge(query, limit = 5)
        val context = StringBuilder()
        var tokenCount = 0
        
        for (chunk in relevantChunks) {
            if (tokenCount + chunk.tokens > maxTokens) break
            
            context.append("Knowledge from ${chunk.category}: ${chunk.content}\n\n")
            tokenCount += chunk.tokens
        }
        
        return if (context.isNotEmpty()) {
            "Relevant knowledge context:\n\n$context"
        } else ""
    }
    
    /**
     * Toggle dataset active status
     */
    suspend fun toggleDatasetActive(datasetId: String, isActive: Boolean) {
        knowledgeDao.setDatasetActive(datasetId, isActive)
    }
    
    /**
     * Delete dataset and its chunks
     */
    suspend fun deleteDataset(datasetId: String) {
        knowledgeDao.deleteChunksForDataset(datasetId)
        knowledgeDao.deleteDataset(datasetId)
        
        // Delete files
        val datasetDir = File(knowledgeDir, datasetId)
        val zipFile = File(knowledgeDir, "$datasetId.zip")
        
        datasetDir.deleteRecursively()
        zipFile.delete()
    }
    
    /**
     * Get download progress for a dataset
     */
    fun getDownloadProgress(datasetId: String): Float {
        return downloadProgress.value[datasetId] ?: 0f
    }
    
    /**
     * Cancel download
     */
    fun cancelDownload(datasetId: String) {
        downloadJobs[datasetId]?.cancel()
        downloadJobs.remove(datasetId)
        
        _downloadProgress.value = _downloadProgress.value - datasetId
    }
}