package com.nervesparks.iris.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nervesparks.iris.data.UserPreferencesRepository
import com.nervesparks.iris.data.db.AppDatabase
import com.nervesparks.iris.data.db.ChatDao
import com.nervesparks.iris.data.db.DocumentDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        // Migration from v1 -> v2: ensure documents table exists
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `documents` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `text` TEXT NOT NULL,
                        `embedding` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "iris_chats.db"
        )
            .addMigrations(Migration(1, 2) { db -> MIGRATION_1_2.migrate(db) })
            .build()
    }

    @Provides
    fun provideChatDao(appDatabase: AppDatabase): ChatDao {
        return appDatabase.chatDao()
    }

    @Provides
    fun provideDocumentDao(appDatabase: AppDatabase): DocumentDao {
        return appDatabase.documentDao()
    }
}
