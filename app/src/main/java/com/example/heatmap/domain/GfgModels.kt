package com.example.heatmap.domain

import androidx.room.Entity
import androidx.room.PrimaryKey

data class GfgPotdResponse(
    val id: Int,
    val date: String,
    val is_solved: Boolean,
    val problem_id: Int,
    val problem_name: String,
    val problem_url: String,
    val difficulty: String,
    val tags: GfgTags,
    val remaining_time: Long,
    val end_date: String,
    val accuracy: Double,
    val total_submissions: Int,
    val is_time_machine_reward_active: Boolean
)

data class GfgTags(
    val company_tags: List<String>,
    val topic_tags: List<String>
)

@Entity(tableName = "gfg_potd")
data class GfgPotdEntity(
    @PrimaryKey val date: String, // format YYYY-MM-DD
    val id: Int,
    val problemName: String,
    val problemUrl: String,
    val difficulty: String,
    val accuracy: Double,
    val totalSubmissions: Int,
    var isSolved: Boolean,
    val remainingTime: Long,
    val endDate: String,
    val companyTags: String, // Comma separated
    val topicTags: String, // Comma separated
    val fetchTimestamp: Long = System.currentTimeMillis()
)
