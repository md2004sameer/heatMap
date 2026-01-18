package com.example.heatmap

import com.google.gson.annotations.SerializedName

data class GraphQLRequest(
    val query: String,
    val variables: Map<String, Any?> = emptyMap(),
    val operationName: String? = null
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
    val upcomingContests: List<Contest>?,
    val recentSubmissionList: List<RecentSubmission>?,
    // Mapping for problem list queries
    @SerializedName("problemsetQuestionList", alternate = ["questionList"])
    val problemsetQuestionList: ProblemsetQuestionList?,
    val question: QuestionDetail?
)

data class RecentSubmission(
    val title: String,
    val titleSlug: String,
    val timestamp: String,
    val statusDisplay: String,
    val lang: String
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
    val date: String?,
    val userStatus: String?,
    val link: String?,
    val question: ChallengeQuestion?
)

data class ChallengeQuestion(
    val questionId: String?,
    val title: String?,
    val difficulty: String?,
    val titleSlug: String?
)

data class StreakCounter(
    val streakCount: Int,
    val daysSkipped: Int,
    val currentDayCompleted: Boolean
)

data class DifficultyCount(
    val difficulty: String?,
    val count: Int
)

data class MatchedUser(
    val username: String?,
    val profile: UserProfile?,
    val submitStats: SubmitStats?,
    val userCalendar: UserCalendar?
)

data class UserProfile(
    val realName: String?,
    val userAvatar: String?,
    val countryName: String?,
    val ranking: Int,
    val skillTags: List<String>?
)

data class SubmitStats(
    val acSubmissionNum: List<SubmissionNum>?,
    val totalSubmissionNum: List<SubmissionNum>?
)

data class SubmissionNum(
    val difficulty: String?,
    val count: Int,
    val submissions: Int
)

data class UserCalendar(
    val activeYears: List<Int>?,
    val streak: Int,
    val totalActiveDays: Int,
    val submissionCalendar: String? // This is a JSON string of {timestamp: count}
)

data class UserContestRanking(
    val rating: Double,
    val globalRanking: Int,
    val totalParticipants: Int,
    val topPercentage: Double
)

// Problems List Models
data class ProblemsetQuestionList(
    @SerializedName("totalNum") val total: Int,
    @SerializedName("questions", alternate = ["data"]) val questions: List<ProblemSummary>
)

data class ProblemsetTag(
    val name: String,
    val slug: String
)

data class ProblemSummary(
    val questionId: String?,
    @SerializedName("questionFrontendId") val frontendQuestionId: String,
    val title: String,
    val titleSlug: String,
    val difficulty: String,
    val isPaidOnly: Boolean,
    val acRate: Double?,
    val topicTags: List<ProblemsetTag>?
)

// Problem Detail Models
data class QuestionDetail(
    val questionId: String,
    val questionFrontendId: String,
    val title: String,
    val titleSlug: String,
    val content: String?,
    val difficulty: String,
    val isPaidOnly: Boolean,
    val codeSnippets: List<CodeSnippet>?,
    val stats: String?, // JSON string containing totalAccepted, totalSubmission
    val hints: List<String>?,
    val sampleTestCase: String?,
    val topicTags: List<ProblemsetTag>?
)

data class CodeSnippet(
    val lang: String,
    val langSlug: String,
    val code: String
)

// Training Plan Models
data class DailyTrainingPlan(
    val date: String,
    val tasks: List<TrainingTask>
)

data class TrainingTask(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val difficulty: String,
    val estimatedTime: Int, // in minutes
    val type: TaskType,
    var isCompleted: Boolean = false
)

enum class TaskType {
    NEW_SOLVE, RE_SOLVE, REVIEW, DRILL, CUSTOM
}
