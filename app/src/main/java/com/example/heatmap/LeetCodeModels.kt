package com.example.heatmap

import com.google.gson.annotations.SerializedName

data class GraphQLRequest(
    val query: String,
    val variables: Map<String, Any> = emptyMap()
)

data class LeetCodeResponse(
    val data: LeetCodeData?,
    val errors: List<LeetCodeError>?
)

data class LeetCodeError(
    val message: String
)

data class LeetCodeData(
    val allQuestionsCount: List<DifficultyCount>?,
    val matchedUser: MatchedUser?,
    val userContestRanking: UserContestRanking?,
    val streakCounter: StreakCounter?,
    val activeDailyCodingChallengeQuestion: DailyChallenge?,
    val upcomingContests: List<Contest>?
)

data class Contest(
    val title: String,
    val titleSlug: String,
    val startTime: Long,
    val duration: Int,
    val originStartTime: Long,
    val isVirtual: Boolean
)

data class DailyChallenge(
    val date: String,
    val userStatus: String,
    val link: String,
    val question: ChallengeQuestion
)

data class ChallengeQuestion(
    val questionId: String,
    val title: String,
    val difficulty: String,
    val titleSlug: String
)

data class StreakCounter(
    val streakCount: Int,
    val daysSkipped: Int,
    val currentDayCompleted: Boolean
)

data class DifficultyCount(
    val difficulty: String,
    val count: Int
)

data class MatchedUser(
    val username: String,
    val profile: UserProfile,
    val submitStats: SubmitStats,
    val userCalendar: UserCalendar
)

data class UserProfile(
    val realName: String?,
    val countryName: String?,
    val ranking: Int,
    val skillTags: List<String>?
)

data class SubmitStats(
    val acSubmissionNum: List<SubmissionNum>,
    val totalSubmissionNum: List<SubmissionNum>
)

data class SubmissionNum(
    val difficulty: String,
    val count: Int,
    val submissions: Int
)

data class UserCalendar(
    val activeYears: List<Int>,
    val streak: Int,
    val totalActiveDays: Int,
    val submissionCalendar: String // This is a JSON string of {timestamp: count}
)

data class UserContestRanking(
    val rating: Double,
    val globalRanking: Int,
    val totalParticipants: Int,
    val topPercentage: Double
)
