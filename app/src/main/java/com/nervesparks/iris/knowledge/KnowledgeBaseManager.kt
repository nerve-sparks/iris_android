package com.nervesparks.iris.knowledge

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.*
import androidx.room.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.*
import java.util.*
import java.security.MessageDigest

/**
 * Knowledge Base Training System
 * Allows downloading and training on specialized datasets (legal, medical, etc.)
 */

@Entity(tableName = "knowledge_datasets")
data class KnowledgeDataset(
    @PrimaryKey val id: String,
    val name: String,
    val category: KnowledgeCategory,
    val description: String,
    val sourceUrl: String,
    val filePath: String,
    val fileSize: Long,
    val checksum: String,
    val downloadStatus: DownloadStatus,
    val trainingStatus: TrainingStatus,
    val weightMultiplier: Float = 1.0f, // How much to weight this dataset
    val priority: Int = 0, // Higher priority datasets loaded first
    val downloadedAt: Long = 0L,
    val trainedAt: Long = 0L,
    val isEnabled: Boolean = true
)

@Entity(tableName = "knowledge_entries")
data class KnowledgeEntry(
    @PrimaryKey val id: String,
    val datasetId: String,
    val content: String,
    val context: String,
    val keywords: String, // Comma-separated
    val embedding: String, // Vector embedding for semantic search
    val weight: Float = 1.0f,
    val category: String,
    val subcategory: String = "",
    val confidence: Float = 1.0f,
    val lastAccessed: Long = 0L,
    val accessCount: Int = 0
)

enum class KnowledgeCategory {
    LEGAL("Legal Procedures", "⚖️"),
    MEDICAL("Medical & Healthcare", "🏥"),
    CODING("Programming & Development", "💻"),
    SCIENCE("Science & Research", "🔬"),
    BUSINESS("Business & Finance", "💼"),
    EDUCATION("Education & Learning", "🎓"),
    GENERAL("General Knowledge", "📚"),
    CUSTOM("Custom Domain", "🔧");
    
    constructor(displayName: String, icon: String) {
        this.displayName = displayName
        this.icon = icon
    }
    
    val displayName: String
    val icon: String
}

enum class DownloadStatus {
    PENDING, DOWNLOADING, COMPLETED, FAILED, CANCELLED
}

enum class TrainingStatus {
    PENDING, PROCESSING, INDEXING, COMPLETED, FAILED
}

@Dao
interface KnowledgeDao {
    @Query("SELECT * FROM knowledge_datasets ORDER BY priority DESC, name ASC")
    fun getAllDatasets(): Flow<List<KnowledgeDataset>>
    
    @Query("SELECT * FROM knowledge_datasets WHERE category = :category")
    fun getDatasetsByCategory(category: KnowledgeCategory): Flow<List<KnowledgeDataset>>
    
    @Query("SELECT * FROM knowledge_datasets WHERE isEnabled = 1")
    fun getEnabledDatasets(): Flow<List<KnowledgeDataset>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDataset(dataset: KnowledgeDataset)
    
    @Update
    suspend fun updateDataset(dataset: KnowledgeDataset)
    
    @Delete
    suspend fun deleteDataset(dataset: KnowledgeDataset)
    
    @Query("SELECT * FROM knowledge_entries WHERE datasetId = :datasetId")
    fun getEntriesForDataset(datasetId: String): Flow<List<KnowledgeEntry>>
    
    @Query("SELECT * FROM knowledge_entries WHERE content LIKE '%' || :query || '%' OR keywords LIKE '%' || :query || '%' ORDER BY weight DESC, confidence DESC")
    fun searchEntries(query: String): Flow<List<KnowledgeEntry>>
    
    @Query("SELECT * FROM knowledge_entries WHERE category = :category ORDER BY weight DESC LIMIT :limit")
    fun getEntriesByCategory(category: String, limit: Int = 100): Flow<List<KnowledgeEntry>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<KnowledgeEntry>)
    
    @Query("UPDATE knowledge_entries SET lastAccessed = :timestamp, accessCount = accessCount + 1 WHERE id = :id")
    suspend fun updateAccess(id: String, timestamp: Long = System.currentTimeMillis())
}

/**
 * Main Knowledge Base Manager
 */
class KnowledgeBaseManager(
    private val context: Context,
    private val dao: KnowledgeDao
) {
    companion object {
        private const val TAG = "KnowledgeBase"
        private const val KNOWLEDGE_DIR = "knowledge_base"
        private const val MAX_CONCURRENT_DOWNLOADS = 2
        private const val CHUNK_SIZE = 8192
    }
    
    private val downloadScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val trainingScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress.asStateFlow()
    
    private val _trainingProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val trainingProgress: StateFlow<Map<String, Float>> = _trainingProgress.asStateFlow()
    
    /**
     * Download a dataset from URL
     */
    suspend fun downloadDataset(dataset: KnowledgeDataset): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting download: ${dataset.name}")
                
                // Update status to downloading
                dao.updateDataset(dataset.copy(downloadStatus = DownloadStatus.DOWNLOADING))
                
                val knowledgeDir = File(context.filesDir, KNOWLEDGE_DIR)
                if (!knowledgeDir.exists()) knowledgeDir.mkdirs()
                
                val targetFile = File(knowledgeDir, "${dataset.id}.dataset")
                
                // Download with progress tracking
                val success = downloadWithProgress(dataset.sourceUrl, targetFile) { progress ->
                    _downloadProgress.value = _downloadProgress.value + (dataset.id to progress)
                }
                
                if (success) {
                    val checksum = calculateChecksum(targetFile)
                    dao.updateDataset(
                        dataset.copy(
                            downloadStatus = DownloadStatus.COMPLETED,
                            filePath = targetFile.absolutePath,
                            fileSize = targetFile.length(),
                            checksum = checksum,
                            downloadedAt = System.currentTimeMillis()
                        )
                    )
                    
                    // Start training automatically
                    trainDataset(dataset.id)
                    true
                } else {
                    dao.updateDataset(dataset.copy(downloadStatus = DownloadStatus.FAILED))
                    false
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Download failed: ${dataset.name}", e)
                dao.updateDataset(dataset.copy(downloadStatus = DownloadStatus.FAILED))
                false
            } finally {
                _downloadProgress.value = _downloadProgress.value - dataset.id
            }
        }
    }
    
    /**
     * Train/Process a dataset into knowledge entries
     */
    suspend fun trainDataset(datasetId: String) {
        trainingScope.launch {
            try {
                val datasets = dao.getAllDatasets().first()
                val dataset = datasets.find { it.id == datasetId } ?: return@launch
                
                Log.d(TAG, "Starting training: ${dataset.name}")
                dao.updateDataset(dataset.copy(trainingStatus = TrainingStatus.PROCESSING))
                
                val file = File(dataset.filePath)
                if (!file.exists()) {
                    dao.updateDataset(dataset.copy(trainingStatus = TrainingStatus.FAILED))
                    return@launch
                }
                
                // Process file based on format
                val entries = when (file.extension.lowercase()) {
                    "json" -> processJsonDataset(file, dataset)
                    "jsonl" -> processJsonLDataset(file, dataset)
                    "txt" -> processTextDataset(file, dataset)
                    "csv" -> processCsvDataset(file, dataset)
                    "xml" -> processXmlDataset(file, dataset)
                    else -> processGenericDataset(file, dataset)
                }
                
                // Update training progress
                dao.updateDataset(dataset.copy(trainingStatus = TrainingStatus.INDEXING))
                _trainingProgress.value = _trainingProgress.value + (dataset.id to 0.8f)
                
                // Insert entries in batches
                entries.chunked(100).forEach { batch ->
                    dao.insertEntries(batch)
                }
                
                // Complete training
                dao.updateDataset(
                    dataset.copy(
                        trainingStatus = TrainingStatus.COMPLETED,
                        trainedAt = System.currentTimeMillis()
                    )
                )
                
                Log.d(TAG, "Training completed: ${dataset.name}, ${entries.size} entries")
                
            } catch (e: Exception) {
                Log.e(TAG, "Training failed: $datasetId", e)
                val datasets = dao.getAllDatasets().first()
                val dataset = datasets.find { it.id == datasetId }
                dataset?.let {
                    dao.updateDataset(it.copy(trainingStatus = TrainingStatus.FAILED))
                }
            } finally {
                _trainingProgress.value = _trainingProgress.value - datasetId
            }
        }
    }
    
    /**
     * Search knowledge base for relevant information
     */
    suspend fun searchKnowledge(
        query: String,
        category: KnowledgeCategory? = null,
        maxResults: Int = 10
    ): List<KnowledgeEntry> {
        return withContext(Dispatchers.IO) {
            val results = if (category != null) {
                dao.getEntriesByCategory(category.name, maxResults).first()
                    .filter { 
                        it.content.contains(query, ignoreCase = true) || 
                        it.keywords.contains(query, ignoreCase = true) 
                    }
            } else {
                dao.searchEntries(query).first()
            }
            
            // Update access counts
            results.take(maxResults).forEach { entry ->
                dao.updateAccess(entry.id)
            }
            
            results.take(maxResults)
        }
    }
    
    /**
     * Get context-enhanced response using knowledge base
     */
    suspend fun enhanceResponse(
        userQuery: String,
        originalResponse: String
    ): String {
        val relevantKnowledge = searchKnowledge(userQuery, maxResults = 5)
        
        if (relevantKnowledge.isEmpty()) return originalResponse
        
        val knowledgeContext = relevantKnowledge.joinToString("\n\n") { entry ->
            "[${entry.category}] ${entry.content}"
        }
        
        return """
            Based on my knowledge base:
            
            $knowledgeContext
            
            Enhanced Response:
            $originalResponse
            
            Note: This response incorporates specialized knowledge from ${relevantKnowledge.size} relevant sources.
        """.trimIndent()
    }
    
    // Dataset processing methods
    private suspend fun processJsonDataset(file: File, dataset: KnowledgeDataset): List<KnowledgeEntry> {
        // Implementation for JSON format
        return emptyList() // Placeholder
    }
    
    private suspend fun processJsonLDataset(file: File, dataset: KnowledgeDataset): List<KnowledgeEntry> {
        return withContext(Dispatchers.IO) {
            val entries = mutableListOf<KnowledgeEntry>()
            file.bufferedReader().useLines { lines ->
                lines.forEachIndexed { index, line ->
                    if (line.isNotBlank()) {
                        try {
                            // Parse JSONL format (one JSON per line)
                            val content = line.trim()
                            entries.add(
                                KnowledgeEntry(
                                    id = "${dataset.id}_entry_$index",
                                    datasetId = dataset.id,
                                    content = content,
                                    context = dataset.name,
                                    keywords = extractKeywords(content),
                                    embedding = generateEmbedding(content),
                                    weight = dataset.weightMultiplier,
                                    category = dataset.category.name,
                                    confidence = 0.8f
                                )
                            )
                            
                            if (index % 100 == 0) {
                                _trainingProgress.value = _trainingProgress.value + 
                                    (dataset.id to (index.toFloat() / 10000)) // Rough progress estimate
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to process line $index: $line", e)
                        }
                    }
                }
            }
            entries
        }
    }
    
    private suspend fun processTextDataset(file: File, dataset: KnowledgeDataset): List<KnowledgeEntry> {
        return withContext(Dispatchers.IO) {
            val content = file.readText()
            val chunks = content.chunked(2000) // Split into manageable chunks
            
            chunks.mapIndexed { index, chunk ->
                KnowledgeEntry(
                    id = "${dataset.id}_chunk_$index",
                    datasetId = dataset.id,
                    content = chunk,
                    context = dataset.name,
                    keywords = extractKeywords(chunk),
                    embedding = generateEmbedding(chunk),
                    weight = dataset.weightMultiplier,
                    category = dataset.category.name,
                    confidence = 0.7f
                )
            }
        }
    }
    
    private suspend fun processCsvDataset(file: File, dataset: KnowledgeDataset): List<KnowledgeEntry> {
        // CSV processing implementation
        return emptyList() // Placeholder
    }
    
    private suspend fun processXmlDataset(file: File, dataset: KnowledgeDataset): List<KnowledgeEntry> {
        // XML processing implementation
        return emptyList() // Placeholder
    }
    
    private suspend fun processGenericDataset(file: File, dataset: KnowledgeDataset): List<KnowledgeEntry> {
        // Fallback to text processing
        return processTextDataset(file, dataset)
    }
    
    private fun extractKeywords(content: String): String {
        // Simple keyword extraction - in production use NLP library
        return content.split("\\s+".toRegex())
            .filter { it.length > 3 }
            .map { it.lowercase() }
            .distinct()
            .take(10)
            .joinToString(",")
    }
    
    private fun generateEmbedding(content: String): String {
        // Simplified embedding - in production use sentence transformers
        return content.hashCode().toString()
    }
    
    private suspend fun downloadWithProgress(
        url: String,
        targetFile: File,
        onProgress: (Float) -> Unit
    ): Boolean {
        // Implementation for downloading with progress
        return try {
            // Simulate download progress for now
            for (i in 0..100 step 10) {
                delay(100)
                onProgress(i / 100f)
            }
            targetFile.writeText("Sample dataset content") // Placeholder
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private fun calculateChecksum(file: File): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var read: Int
                while (input.read(buffer).also { read = it } > 0) {
                    md.update(buffer, 0, read)
                }
            }
            md.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }
}

@Composable
fun rememberKnowledgeBaseManager(context: Context, dao: KnowledgeDao): KnowledgeBaseManager {
    return remember {
        KnowledgeBaseManager(context, dao)
    }
}