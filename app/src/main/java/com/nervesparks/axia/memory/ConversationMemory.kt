package com.nervesparks.axia.memory

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import com.nervesparks.axia.security.AxiaSecurityLayer
import com.nervesparks.axia.security.EncryptedData
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * Conversation Memory System for Axia
 * Manages encrypted conversation storage and intelligent context retrieval
 */
class ConversationMemory(
    private val context: Context,
    private val securityLayer: AxiaSecurityLayer
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // Memory database
    private lateinit var database: ConversationDatabase
    
    // Memory state
    private val _activeConversations = MutableStateFlow<List<ConversationSummary>>(emptyList())
    val activeConversations: StateFlow<List<ConversationSummary>> = _activeConversations.asStateFlow()
    
    private val _currentContext = MutableStateFlow<ConversationContext?>(null)
    val currentContext: StateFlow<ConversationContext?> = _currentContext.asStateFlow()
    
    // Memory cache
    private val contextCache = mutableMapOf<String, ConversationContext>()
    
    suspend fun initialize(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Initialize Room database
                database = Room.databaseBuilder(
                    context,
                    ConversationDatabase::class.java,
                    "axia_conversations_db"
                ).build()
                
                // Load active conversations
                loadActiveConversations()
                
                true
            } catch (e: Exception) {
                android.util.Log.e("ConversationMemory", "Failed to initialize", e)
                false
            }
        }
    }
    
    /**
     * Create new conversation
     */
    suspend fun createConversation(
        title: String? = null,
        projectId: String? = null,
        mode: ConversationMode = ConversationMode.ASSISTANT
    ): String {
        return withContext(Dispatchers.IO) {
            val conversationId = UUID.randomUUID().toString()
            val timestamp = System.currentTimeMillis()
            
            val conversation = ConversationEntity(
                id = conversationId,
                title = title ?: "New Conversation",
                projectId = projectId,
                mode = mode,
                createdAt = timestamp,
                lastUpdated = timestamp,
                messageCount = 0,
                isActive = true
            )
            
            database.conversationDao().insertConversation(conversation)
            loadActiveConversations()
            
            conversationId
        }
    }
    
    /**
     * Store message with encryption
     */
    suspend fun storeMessage(
        conversationId: String,
        content: String,
        isUser: Boolean,
        metadata: MessageMetadata? = null
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Encrypt message content
                val encryptedData = securityLayer.encryptChatMessage(content, conversationId, isUser)
                    ?: return@withContext false
                
                val messageId = UUID.randomUUID().toString()
                val timestamp = System.currentTimeMillis()
                
                val message = MessageEntity(
                    id = messageId,
                    conversationId = conversationId,
                    encryptedContent = Json.encodeToString(EncryptedData.serializer(), encryptedData),
                    isUser = isUser,
                    timestamp = timestamp,
                    metadata = metadata?.let { Json.encodeToString(MessageMetadata.serializer(), it) }
                )
                
                database.messageDao().insertMessage(message)
                updateConversationStats(conversationId)
                
                true
            } catch (e: Exception) {
                android.util.Log.e("ConversationMemory", "Failed to store message", e)
                false
            }
        }
    }
    
    /**
     * Retrieve conversation messages with decryption
     */
    suspend fun getConversationMessages(
        conversationId: String,
        limit: Int = 50
    ): List<DecryptedMessage> {
        return withContext(Dispatchers.IO) {
            try {
                val messages = database.messageDao().getConversationMessages(conversationId, limit)
                
                messages.mapNotNull { message ->
                    try {
                        val encryptedData = Json.decodeFromString<EncryptedData>(message.encryptedContent)
                        val content = securityLayer.decryptChatMessage(
                            encryptedData, 
                            conversationId, 
                            message.isUser
                        ) ?: return@mapNotNull null
                        
                        DecryptedMessage(
                            id = message.id,
                            conversationId = message.conversationId,
                            content = content,
                            isUser = message.isUser,
                            timestamp = message.timestamp,
                            metadata = message.metadata?.let { 
                                Json.decodeFromString<MessageMetadata>(it) 
                            }
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("ConversationMemory", "Failed to decrypt message", e)
                        null
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ConversationMemory", "Failed to retrieve messages", e)
                emptyList()
            }
        }
    }
    
    /**
     * Generate conversation context for AI
     */
    suspend fun generateConversationContext(conversationId: String): ConversationContext? {
        return withContext(Dispatchers.IO) {
            try {
                // Check cache
                contextCache[conversationId]?.let { cached ->
                    if (System.currentTimeMillis() - cached.generatedAt < 300_000) { // 5 minutes
                        return@withContext cached
                    }
                }
                
                val messages = getConversationMessages(conversationId, 20)
                if (messages.isEmpty()) return@withContext null
                
                val conversation = database.conversationDao().getConversation(conversationId)
                    ?: return@withContext null
                
                val context = ConversationContext(
                    conversationId = conversationId,
                    title = conversation.title,
                    mode = conversation.mode,
                    recentMessages = messages.takeLast(5).map { "${if (it.isUser) "User" else "Axia"}: ${it.content}" },
                    messageCount = messages.size,
                    generatedAt = System.currentTimeMillis()
                )
                
                contextCache[conversationId] = context
                _currentContext.value = context
                
                context
            } catch (e: Exception) {
                android.util.Log.e("ConversationMemory", "Failed to generate context", e)
                null
            }
        }
    }
    
    private suspend fun loadActiveConversations() {
        val conversations = database.conversationDao().getActiveConversations()
        _activeConversations.value = conversations.map { entity ->
            ConversationSummary(
                id = entity.id,
                title = entity.title,
                mode = entity.mode,
                projectId = entity.projectId,
                lastUpdated = entity.lastUpdated,
                messageCount = entity.messageCount
            )
        }
    }
    
    private suspend fun updateConversationStats(conversationId: String) {
        database.conversationDao().updateConversationStats(conversationId, System.currentTimeMillis())
        loadActiveConversations()
    }
}

// Data classes

@Serializable
data class MessageMetadata(
    val tokensUsed: Int = 0,
    val responseTime: Long = 0,
    val modelUsed: String? = null,
    val isAutonomous: Boolean = false
)

data class DecryptedMessage(
    val id: String,
    val conversationId: String,
    val content: String,
    val isUser: Boolean,
    val timestamp: Long,
    val metadata: MessageMetadata?
)

data class ConversationContext(
    val conversationId: String,
    val title: String,
    val mode: ConversationMode,
    val recentMessages: List<String>,
    val messageCount: Int,
    val generatedAt: Long
)

data class ConversationSummary(
    val id: String,
    val title: String,
    val mode: ConversationMode,
    val projectId: String?,
    val lastUpdated: Long,
    val messageCount: Int
)

enum class ConversationMode {
    ASSISTANT, AUTONOMOUS_AGENT, DEEP_RESEARCH, DEVELOPMENT, SYSTEM_CONTROL, IMAGE_GENERATION
}

// Room entities

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val projectId: String?,
    val mode: ConversationMode,
    val createdAt: Long,
    val lastUpdated: Long,
    val messageCount: Int,
    val isActive: Boolean
)

@Entity(
    tableName = "messages",
    foreignKeys = [ForeignKey(
        entity = ConversationEntity::class,
        parentColumns = ["id"],
        childColumns = ["conversationId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val encryptedContent: String,
    val isUser: Boolean,
    val timestamp: Long,
    val metadata: String?
)

@Database(
    entities = [ConversationEntity::class, MessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ConversationDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
}

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations WHERE isActive = 1 ORDER BY lastUpdated DESC")
    suspend fun getActiveConversations(): List<ConversationEntity>
    
    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getConversation(id: String): ConversationEntity?
    
    @Insert
    suspend fun insertConversation(conversation: ConversationEntity)
    
    @Query("UPDATE conversations SET lastUpdated = :timestamp, messageCount = messageCount + 1 WHERE id = :id")
    suspend fun updateConversationStats(id: String, timestamp: Long)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getConversationMessages(conversationId: String, limit: Int): List<MessageEntity>
    
    @Insert
    suspend fun insertMessage(message: MessageEntity)
}