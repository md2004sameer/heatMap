package com.example.heatmap

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "striver_problems")
data class StriverProblemEntity(
    @PrimaryKey val id: Int,
    val section: String,
    val subSection: String,
    val title: String,
    val difficulty: String,
    val solveUrl: String,
    val editorialUrl: String,
    val postUrl: String,
    val youtubeUrl: String,
    val isCompleted: Boolean = false,
    val practiceCount: Int = 0,
    val revisionCount: Int = 0,
    val noteCount: Int = 0,
    val isBookmarked: Boolean = false,
    val lastRevisedAt: Long? = null,
    val stepOrder: Int = 0
)

@Entity(tableName = "training_plans")
data class TrainingPlanEntity(
    @PrimaryKey val date: String, // YYYY-MM-DD
    val lastGenerated: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "training_tasks",
    foreignKeys = [
        ForeignKey(
            entity = TrainingPlanEntity::class,
            parentColumns = ["date"],
            childColumns = ["planDate"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("planDate")]
)
data class TrainingTaskEntity(
    @PrimaryKey val id: String,
    val planDate: String,
    val title: String,
    val description: String,
    val category: String,
    val difficulty: String,
    val estimatedTime: Int,
    val type: String,
    val isCompleted: Boolean = false
)

@Entity(tableName = "app_prefs")
data class AppPreferenceEntity(
    @PrimaryKey val key: String,
    val value: String
)

@Entity(tableName = "pattern_progress")
data class PatternProgressEntity(
    @PrimaryKey val link: String,
    val isCompleted: Boolean = false,
    val lastSolvedAt: Long = System.currentTimeMillis()
)

@Dao
interface StriverDao {
    @Query("SELECT * FROM striver_problems ORDER BY id ASC")
    fun getAllStriverProblemsFlow(): Flow<List<StriverProblemEntity>>

    @Query("SELECT * FROM striver_problems ORDER BY id ASC")
    suspend fun getAllStriverProblems(): List<StriverProblemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStriverProblems(problems: List<StriverProblemEntity>)

    @Query("UPDATE striver_problems SET isCompleted = :completed WHERE id = :id")
    suspend fun updateProgress(id: Int, completed: Boolean)

    @Query("UPDATE striver_problems SET practiceCount = practiceCount + 1 WHERE id = :id")
    suspend fun incrementPractice(id: Int)

    @Query("UPDATE striver_problems SET revisionCount = revisionCount + 1, lastRevisedAt = :timestamp WHERE id = :id")
    suspend fun incrementRevision(id: Int, timestamp: Long)

    @Query("UPDATE striver_problems SET isBookmarked = :bookmarked WHERE id = :id")
    suspend fun updateBookmark(id: Int, bookmarked: Boolean)

    @Query("SELECT COUNT(*) FROM striver_problems")
    suspend fun getCount(): Int
}

@Dao
interface TrainingDao {
    @Query("SELECT * FROM training_plans WHERE date = :date")
    suspend fun getPlanByDate(date: String): TrainingPlanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPlan(plan: TrainingPlanEntity)

    @Query("SELECT * FROM training_tasks WHERE planDate = :date")
    suspend fun getTasksForPlan(date: String): List<TrainingTaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TrainingTaskEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TrainingTaskEntity)

    @Update
    suspend fun updateTask(task: TrainingTaskEntity)

    @Delete
    suspend fun deleteTask(task: TrainingTaskEntity)

    @Query("DELETE FROM training_tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: String)
}

@Dao
interface PreferenceDao {
    @Query("SELECT value FROM app_prefs WHERE `key` = :key")
    suspend fun getPreference(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setPreference(pref: AppPreferenceEntity)

    @Query("SELECT value FROM app_prefs WHERE `key` = :key")
    suspend fun getIntPreference(key: String): String? // Room returns String, parse manually
}

@Dao
interface PatternProgressDao {
    @Query("SELECT * FROM pattern_progress")
    fun getAllProgressFlow(): Flow<List<PatternProgressEntity>>

    @Query("SELECT * FROM pattern_progress")
    suspend fun getAllProgress(): List<PatternProgressEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateProgress(progress: PatternProgressEntity)

    @Query("SELECT isCompleted FROM pattern_progress WHERE link = :link")
    suspend fun isCompleted(link: String): Boolean?
}
