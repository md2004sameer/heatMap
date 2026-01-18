package com.example.heatmap

import androidx.room.*

@Entity(tableName = "problems")
data class ProblemEntity(
    @PrimaryKey val questionId: String,
    val questionFrontendId: String,
    val title: String,
    val titleSlug: String,
    val difficulty: String,
    val isPaidOnly: Boolean,
    val acRate: Double,
    val tags: String, // Comma-separated tags
    val content: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Dao
interface ProblemsDao {
    @Query("SELECT * FROM problems ORDER BY CAST(questionFrontendId AS INTEGER) ASC")
    suspend fun getAllProblems(): List<ProblemEntity>

    @Query("SELECT * FROM problems WHERE title LIKE '%' || :query || '%' OR questionFrontendId LIKE '%' || :query || '%' ORDER BY CAST(questionFrontendId AS INTEGER) ASC")
    suspend fun searchProblems(query: String): List<ProblemEntity>

    @Query("SELECT * FROM problems WHERE difficulty = :difficulty ORDER BY CAST(questionFrontendId AS INTEGER) ASC")
    suspend fun filterByDifficulty(difficulty: String): List<ProblemEntity>

    @Query("SELECT * FROM problems WHERE tags LIKE '%' || :tag || '%' ORDER BY CAST(questionFrontendId AS INTEGER) ASC")
    suspend fun filterByTag(tag: String): List<ProblemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProblems(problems: List<ProblemEntity>)

    @Query("SELECT * FROM problems WHERE titleSlug = :slug LIMIT 1")
    suspend fun getProblemBySlug(slug: String): ProblemEntity?

    @Query("UPDATE problems SET content = :content, lastUpdated = :timestamp WHERE titleSlug = :slug")
    suspend fun updateProblemContent(slug: String, content: String, timestamp: Long)
}
