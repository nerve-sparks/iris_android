package com.nervesparks.iris.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: Chat): Long

    @Update
    suspend fun updateChat(chat: Chat)

    @Delete
    suspend fun deleteChat(chat: Chat)

    @Query("SELECT * FROM chats ORDER BY updated DESC")
    fun observeChats(): Flow<List<Chat>>

    @Query("SELECT * FROM chats")
    suspend fun getAllChats(): List<Chat>

    @Query("SELECT * FROM chats WHERE id = :chatId")
    fun observeChat(chatId: Long): Flow<Chat>

    @Query("SELECT * FROM chats WHERE id = :chatId")
    suspend fun getChat(chatId: Long): Chat?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<Message>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)

    @Update
    suspend fun updateMessage(message: Message)

    @Delete
    suspend fun deleteMessage(message: Message)

    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getMessage(messageId: Long): Message?

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY `index`")
    suspend fun loadMessages(chatId: Long): List<Message>

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun deleteMessages(chatId: Long)

    // Stats queries
    @Query("SELECT COUNT(*) FROM messages WHERE chatId = :chatId")
    suspend fun countMessages(chatId: Long): Int

    @Query("SELECT COUNT(*) FROM messages WHERE chatId = :chatId AND LOWER(role) = LOWER(:role)")
    suspend fun countMessagesByRole(chatId: Long, role: String): Int

    @Query("SELECT timestamp FROM messages WHERE chatId = :chatId AND LOWER(role) IN ('user','assistant') ORDER BY `index`")
    suspend fun getUserAssistantTimestamps(chatId: Long): List<Long>
}