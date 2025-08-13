package com.nervesparks.iris.data.repository

import com.nervesparks.iris.data.db.Chat
import com.nervesparks.iris.data.db.Message
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for chat operations
 */
interface ChatRepository {
    /** Observe all chats (ordered by updated desc) */
    fun observeChats(): Flow<List<Chat>>
    
    /**
     * Get all chats
     */
    suspend fun getAllChats(): List<Chat>
    
    /**
     * Get chat by ID
     */
    suspend fun getChat(chatId: Long): Chat?
    
    /**
     * Observe chat changes
     */
    fun observeChat(chatId: Long): Flow<Chat>
    
    /**
     * Create a new chat
     */
    suspend fun createChat(title: String = "New Chat"): Long
    
    /**
     * Update chat title
     */
    suspend fun updateChatTitle(chatId: Long, title: String)

    /** Convenience: rename chat by entity */
    suspend fun renameChat(chat: Chat, title: String)
    
    /**
     * Delete a chat
     */
    suspend fun deleteChat(chatId: Long)

    /** Convenience: delete chat by entity */
    suspend fun deleteChat(chat: Chat)
    
    /**
     * Get messages for a chat
     */
    suspend fun getMessages(chatId: Long): List<Message>
    
    /**
     * Load messages for a chat
     */
    suspend fun loadMessages(chatId: Long): List<Message>

    /** Save a chat and replace its messages atomically */
    suspend fun saveChatWithMessages(chat: Chat, messages: List<Message>): Long
    
    /**
     * Add a message to a chat
     */
    suspend fun addMessage(chatId: Long, role: String, content: String, index: Int = 0)
    
    /**
     * Update a message
     */
    suspend fun updateMessage(messageId: Long, content: String)
    
    /**
     * Delete a message
     */
    suspend fun deleteMessage(messageId: Long)
    
    /**
     * Clear all messages in a chat
     */
    suspend fun clearMessages(chatId: Long)
    
    /**
     * Get chat statistics
     */
    suspend fun getChatStats(chatId: Long): ChatStats
}

/**
 * Data class for chat statistics
 */
data class ChatStats(
    val totalMessages: Int,
    val userMessages: Int,
    val assistantMessages: Int,
    val totalTokens: Int,
    val averageResponseTime: Long
) 