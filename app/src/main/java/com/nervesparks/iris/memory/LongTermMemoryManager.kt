package com.nervesparks.iris.memory

import android.content.Context
import androidx.room.*
import androidx.compose.runtime.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.Date

/**
 * Long-term memory system for personal assistant
 * Remembers conversations, user preferences, and context across sessions
 */

@Entity(tableName = "memory_entries")
data class MemoryEntry(
    @PrimaryKey val id: String,
    val type: MemoryType,
    val content: String,
    val context: String,
    val importance: Float, // 0.0 to 1.0
    val timestamp: Long,
    val userId: String = "default",
    val tags: String = "", // Comma-separated tags
    val embedding: String = "", // Vector embedding for semantic search
    val lastAccessed: Long = timestamp,
    val accessCount: Int = 0
)

@Entity(tableName = "conversation_history")
data class ConversationHistory(
    @PrimaryKey val id: String,
    val messageId: String,
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long,
    val sessionId: String,
    val embedding: String = "",
    val summary: String = ""
)

@Entity(tableName = "user_preferences")
data class UserPreference(
    @PrimaryKey val key: String,
    val value: String,
    val type: String, // string, boolean, number, json
    val lastUpdated: Long,
    val userId: String = "default"
)

enum class MemoryType {
    PERSONAL_INFO,      // Name, preferences, interests
    WORK_INFO,          // Job, projects, colleagues  
    RELATIONSHIPS,      // Family, friends, contacts
    IMPORTANT_DATES,    // Birthdays, appointments, deadlines
    SKILLS_KNOWLEDGE,   // What user knows, learning goals
    CONVERSATION_CONTEXT, // Previous important conversations
    USER_INSTRUCTIONS,  // How user wants to be helped
    HABITS_PATTERNS     // User's behavior patterns
}

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memory_entries WHERE userId = :userId ORDER BY importance DESC, lastAccessed DESC")
    fun getAllMemories(userId: String = "default"): Flow<List<MemoryEntry>>
    
    @Query("SELECT * FROM memory_entries WHERE type = :type AND userId = :userId ORDER BY importance DESC")
    fun getMemoriesByType(type: MemoryType, userId: String = "default"): Flow<List<MemoryEntry>>
    
    @Query("SELECT * FROM memory_entries WHERE content LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%' AND userId = :userId ORDER BY importance DESC")
    fun searchMemories(query: String, userId: String = "default"): Flow<List<MemoryEntry>>
    
    @Query("SELECT * FROM memory_entries WHERE importance >= :minImportance AND userId = :userId ORDER BY importance DESC LIMIT :limit")
    fun getImportantMemories(minImportance: Float = 0.7f, limit: Int = 50, userId: String = "default"): Flow<List<MemoryEntry>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntry)
    
    @Update
    suspend fun updateMemory(memory: MemoryEntry)
    
    @Delete
    suspend fun deleteMemory(memory: MemoryEntry)
    
    @Query("UPDATE memory_entries SET lastAccessed = :timestamp, accessCount = accessCount + 1 WHERE id = :id")
    suspend fun updateAccess(id: String, timestamp: Long = System.currentTimeMillis())
    
    // Conversation History
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationHistory)
    
    @Query("SELECT * FROM conversation_history WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getConversationHistory(sessionId: String): Flow<List<ConversationHistory>>
    
    @Query("SELECT * FROM conversation_history WHERE content LIKE '%' || :query || '%' ORDER BY timestamp DESC LIMIT 100")
    fun searchConversations(query: String): Flow<List<ConversationHistory>>
    
    // User Preferences  
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setPreference(preference: UserPreference)
    
    @Query("SELECT * FROM user_preferences WHERE key = :key AND userId = :userId")
    suspend fun getPreference(key: String, userId: String = "default"): UserPreference?
    
    @Query("SELECT * FROM user_preferences WHERE userId = :userId")
    fun getAllPreferences(userId: String = "default"): Flow<List<UserPreference>>
}

@Database(
    entities = [MemoryEntry::class, ConversationHistory::class, UserPreference::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MemoryDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
    
    companion object {
        @Volatile
        private var INSTANCE: MemoryDatabase? = null
        
        fun getDatabase(context: Context): MemoryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MemoryDatabase::class.java,
                    "memory_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class Converters {
    @TypeConverter
    fun fromMemoryType(value: MemoryType): String {
        return value.name
    }
    
    @TypeConverter
    fun toMemoryType(value: String): MemoryType {
        return MemoryType.valueOf(value)
    }
}

/**
 * Long-term memory manager for personal assistant capabilities
 */
class LongTermMemoryManager(context: Context) {
    private val database = MemoryDatabase.getDatabase(context)
    private val memoryDao = database.memoryDao()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    companion object {
        private const val TAG = "LongTermMemory"
        private const val MAX_MEMORY_ENTRIES = 10000
        private const val CLEANUP_THRESHOLD = 0.1f // Delete memories below this importance
    }
    
    /**
     * Store a new memory entry
     */
    suspend fun storeMemory(
        content: String,
        type: MemoryType,
        context: String = "",
        importance: Float = 0.5f,
        tags: List<String> = emptyList()
    ) {
        val memory = MemoryEntry(
            id = generateMemoryId(),
            type = type,
            content = content,
            context = context,
            importance = importance.coerceIn(0f, 1f),
            timestamp = System.currentTimeMillis(),
            tags = tags.joinToString(","),
            embedding = generateEmbedding(content) // Simplified embedding
        )
        
        memoryDao.insertMemory(memory)
    }
    
    /**
     * Store conversation in long-term memory
     */
    suspend fun storeConversation(
        messageId: String,
        content: String,
        isFromUser: Boolean,
        sessionId: String,
        summary: String = ""
    ) {
        val conversation = ConversationHistory(
            id = generateConversationId(),
            messageId = messageId,
            content = content,
            isFromUser = isFromUser,
            timestamp = System.currentTimeMillis(),
            sessionId = sessionId,
            embedding = generateEmbedding(content),
            summary = summary
        )
        
        memoryDao.insertConversation(conversation)
    }
    
    /**
     * Retrieve relevant memories for current context
     */
    fun getRelevantMemories(
        query: String,
        type: MemoryType? = null,
        limit: Int = 10
    ): Flow<List<MemoryEntry>> {
        return if (type != null) {
            memoryDao.getMemoriesByType(type)
        } else {
            memoryDao.searchMemories(query)
        }.map { memories ->
            memories.take(limit).also { relevantMemories ->
                // Update access counts
                coroutineScope.launch {
                    relevantMemories.forEach { memory ->
                        memoryDao.updateAccess(memory.id)
                    }
                }
            }
        }
    }
    
    /**
     * Get conversation context for better responses
     */
    fun getConversationContext(sessionId: String): Flow<List<ConversationHistory>> {
        return memoryDao.getConversationHistory(sessionId)
    }
    
    /**
     * Store user preference
     */
    suspend fun setUserPreference(key: String, value: String, type: String = "string") {
        val preference = UserPreference(
            key = key,
            value = value,
            type = type,
            lastUpdated = System.currentTimeMillis()
        )
        memoryDao.setPreference(preference)
    }
    
    /**
     * Get user preference
     */
    suspend fun getUserPreference(key: String, defaultValue: String = ""): String {
        return memoryDao.getPreference(key)?.value ?: defaultValue
    }
    
    /**
     * Generate context summary for AI responses
     */
    suspend fun generateContextSummary(query: String): String {
        val relevantMemories = getRelevantMemories(query, limit = 5)
        return relevantMemories.first().joinToString("\n") { memory ->
            "${memory.type}: ${memory.content}"
        }
    }
    
    /**
     * Clean up old and low-importance memories
     */
    suspend fun cleanupMemories() {
        coroutineScope.launch {
            // Implementation for memory cleanup
            // Remove memories with low importance and old access times
        }
    }
    
    private fun generateMemoryId(): String {
        return "mem_${System.currentTimeMillis()}_${(Math.random() * 1000).toInt()}"
    }
    
    private fun generateConversationId(): String {
        return "conv_${System.currentTimeMillis()}_${(Math.random() * 1000).toInt()}"
    }
    
    private fun generateEmbedding(text: String): String {
        // Simplified embedding - in production, use proper embedding model
        return text.hashCode().toString()
    }
}

@Composable
fun rememberLongTermMemory(context: Context): LongTermMemoryManager {
    return remember {
        LongTermMemoryManager(context)
    }
}