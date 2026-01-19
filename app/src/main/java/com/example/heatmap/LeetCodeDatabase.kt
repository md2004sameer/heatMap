package com.example.heatmap

import android.content.Context
import androidx.room.*
import com.example.heatmap.domain.GfgPotdEntity

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

@Dao
interface GfgDao {
    @Query("SELECT * FROM gfg_potd ORDER BY date DESC")
    suspend fun getAllPotd(): List<GfgPotdEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPotd(potd: GfgPotdEntity)

    @Query("UPDATE gfg_potd SET isSolved = :isSolved WHERE date = :date")
    suspend fun updateSolvedStatus(date: String, isSolved: Boolean)

    @Query("SELECT * FROM gfg_potd WHERE date = :date LIMIT 1")
    suspend fun getPotdByDate(date: String): GfgPotdEntity?
}

@Database(
    entities = [
        CachedLeetCodeData::class,
        Folder::class,
        Note::class,
        ProblemEntity::class,
        GfgPotdEntity::class,
        StriverProblemEntity::class,
        TrainingPlanEntity::class,
        TrainingTaskEntity::class,
        AppPreferenceEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class LeetCodeDatabase : RoomDatabase() {
    abstract fun leetCodeDao(): LeetCodeDao
    abstract fun notesDao(): NotesDao
    abstract fun problemsDao(): ProblemsDao
    abstract fun gfgDao(): GfgDao
    abstract fun striverDao(): StriverDao
    abstract fun trainingDao(): TrainingDao
    abstract fun preferenceDao(): PreferenceDao

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
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
