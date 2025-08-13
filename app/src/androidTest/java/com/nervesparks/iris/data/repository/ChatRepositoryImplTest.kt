package com.nervesparks.iris.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nervesparks.iris.data.db.AppDatabase
import com.nervesparks.iris.data.db.Chat
import com.nervesparks.iris.data.db.ChatDao
import com.nervesparks.iris.data.db.Message
import com.nervesparks.iris.data.repository.impl.ChatRepositoryImpl
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatRepositoryImplTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: ChatDao
    private lateinit var repo: ChatRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.chatDao()
        repo = ChatRepositoryImpl(dao)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun getChatStats_countsMessagesAndAverageResponse() = runBlocking {
        val chatId = dao.insertChat(Chat(title = "Test"))
        val base = System.currentTimeMillis()
        // user then assistant cycles
        dao.insertMessage(Message(chatId = chatId, role = "user", content = "hi", timestamp = base + 0, index = 0))
        dao.insertMessage(Message(chatId = chatId, role = "assistant", content = "hey", timestamp = base + 100, index = 1))
        dao.insertMessage(Message(chatId = chatId, role = "user", content = "q", timestamp = base + 200, index = 2))
        dao.insertMessage(Message(chatId = chatId, role = "assistant", content = "a", timestamp = base + 500, index = 3))

        val stats = repo.getChatStats(chatId)

        assertEquals(4, stats.totalMessages)
        assertEquals(2, stats.userMessages)
        assertEquals(2, stats.assistantMessages)
        // Average of (100-0)=100 and (500-200)=300 => (100+300)/2 = 200
        assertEquals(200, stats.averageResponseTime)
    }
}


