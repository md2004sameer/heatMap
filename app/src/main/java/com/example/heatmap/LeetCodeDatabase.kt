package com.example.heatmap

import android.content.Context
import androidx.room.*

@Entity(tableName = "leetcode_data")
data class CachedLeetCodeData(
    @PrimaryKey val username: String,
    val jsonData: String,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Dao
interface LeetCodeDao {
    @Query("SELECT * FROM leetcode_data WHERE username = :username LIMIT 1")
    suspend fun getCachedData(username: String): CachedLeetCodeData?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertData(data: CachedLeetCodeData)
}

@Database(entities = [CachedLeetCodeData::class, Folder::class, Note::class], version = 2, exportSchema = false)
abstract class LeetCodeDatabase : RoomDatabase() {
    abstract fun leetCodeDao(): LeetCodeDao
    abstract fun notesDao(): NotesDao

    companion object {
        @Volatile
        private var INSTANCE: LeetCodeDatabase? = null

        fun getDatabase(context: Context): LeetCodeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LeetCodeDatabase::class.java,
                    "leetcode_database"
                )
                .fallbackToDestructiveMigration() // For development simplicity
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
