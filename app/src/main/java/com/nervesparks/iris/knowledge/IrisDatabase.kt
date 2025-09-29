package com.nervesparks.iris.knowledge

import android.content.Context
import androidx.room.*
import java.util.*

/**
 * Main Room Database for Iris
 * Handles knowledge base and development environment data
 */

@Database(
    entities = [
        KnowledgeDataset::class,
        KnowledgeChunk::class,
        DatasetUsageStats::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class IrisDatabase : RoomDatabase() {
    
    abstract fun knowledgeDao(): KnowledgeDao
    
    companion object {
        @Volatile
        private var INSTANCE: IrisDatabase? = null
        
        fun getDatabase(context: Context): IrisDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    IrisDatabase::class.java,
                    "iris_database"
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
    fun fromKnowledgeCategory(category: KnowledgeCategory): String {
        return category.name
    }
    
    @TypeConverter
    fun toKnowledgeCategory(categoryString: String): KnowledgeCategory {
        return try {
            KnowledgeCategory.valueOf(categoryString)
        } catch (e: IllegalArgumentException) {
            KnowledgeCategory.GENERAL
        }
    }
    
    @TypeConverter
    fun fromDownloadStatus(status: DownloadStatus): String {
        return status.name
    }
    
    @TypeConverter
    fun toDownloadStatus(statusString: String): DownloadStatus {
        return try {
            DownloadStatus.valueOf(statusString)
        } catch (e: IllegalArgumentException) {
            DownloadStatus.PENDING
        }
    }
    
    @TypeConverter
    fun fromTrainingStatus(status: TrainingStatus): String {
        return status.name
    }
    
    @TypeConverter
    fun toTrainingStatus(statusString: String): TrainingStatus {
        return try {
            TrainingStatus.valueOf(statusString)
        } catch (e: IllegalArgumentException) {
            TrainingStatus.PENDING
        }
    }
}