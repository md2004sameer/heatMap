package com.example.heatmap

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Calendar
import java.util.concurrent.TimeUnit

class LeetCodeRepository private constructor(context: Context) {
    private val dao = LeetCodeDatabase.getDatabase(context).leetCodeDao()
    private val problemsDao = LeetCodeDatabase.getDatabase(context).problemsDao()
    private val gson = Gson()
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36")
                .header("Referer", "https://leetcode.com/")
                .build()
            chain.proceed(request)
        }
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        })
        .build()

    private val service = Retrofit.Builder()
        .baseUrl("https://leetcode.com/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(LeetCodeService::class.java)

    private val profileQuery = """
        query(${"$"}username: String!, ${"$"}year: Int!) {
            activeDailyCodingChallengeQuestion {
                date
                userStatus
                link
                question {
                    questionId
                    title
                    difficulty
                    titleSlug
                }
            }
            upcomingContests {
                title
                titleSlug
                startTime
                duration
                originStartTime
                isVirtual
            }
            allQuestionsCount {
                difficulty
                count
            }
            recentSubmissionList(username: ${"$"}username, limit: 15) {
                title
                titleSlug
                timestamp
                statusDisplay
                lang
            }
            matchedUser(username: ${"$"}username) {
                username
                profile {
                    realName
                    userAvatar
                    countryName
                    ranking
                    skillTags
                }
                submitStats {
                    acSubmissionNum {
                        difficulty
                        count
                        submissions
                    }
                    totalSubmissionNum {
                        difficulty
                        count
                        submissions
                    }
                }
                userCalendar(year: ${"$"}year) {
                  activeYears
                  streak
                  totalActiveDays
                  submissionCalendar
                }
            }
            userContestRanking(username: ${"$"}username) {
                rating
                globalRanking
                totalParticipants
                topPercentage
            }
        }
    """.trimIndent()

    private val problemsQuery = """
        query problemsetQuestionList(${"$"}categorySlug: String, ${"$"}limit: Int, ${"$"}skip: Int, ${"$"}filters: QuestionListFilterInput) {
          problemsetQuestionList(
            categorySlug: ${"$"}categorySlug
            limit: ${"$"}limit
            skip: ${"$"}skip
            filters: ${"$"}filters
          ) {
            total: totalNum
            questions: questions {
              acRate
              difficulty
              isPaidOnly
              questionFrontendId
              questionId
              title
              titleSlug
              topicTags {
                name
                slug
              }
            }
          }
        }
    """.trimIndent()

    private val questionDetailQuery = """
        query questionData(${"$"}titleSlug: String!) {
          question(titleSlug: ${"$"}titleSlug) {
            questionId
            questionFrontendId
            title
            titleSlug
            content
            difficulty
            isPaidOnly
            codeSnippets {
              lang
              langSlug
              code
            }
            stats
            hints
            sampleTestCase
            topicTags {
              name
              slug
            }
          }
        }
    """.trimIndent()

    fun getProfile(username: String): Flow<LeetCodeData?> = flow {
        val cached = dao.getCachedData(username)
        if (cached != null) {
            try {
                emit(gson.fromJson(cached.jsonData, LeetCodeData::class.java))
            } catch (e: Exception) {
                Log.e("LeetCodeRepository", "Error parsing cached data", e)
            }
        }

        try {
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val response = service.getProfile(
                GraphQLRequest(
                    query = profileQuery,
                    variables = mapOf("username" to username, "year" to currentYear)
                )
            )
            
            val newData = response.data
            if (newData?.matchedUser != null) {
                dao.insertData(CachedLeetCodeData(username, gson.toJson(newData)))
                emit(newData)
            }
        } catch (e: Exception) {
            Log.e("LeetCodeRepository", "Network fetch failed", e)
        }
    }

    fun getAllProblems(refresh: Boolean = false): Flow<List<ProblemEntity>> = flow {
        val cached = problemsDao.getAllProblems()
        if (cached.isNotEmpty()) emit(cached)

        if (refresh || cached.isEmpty()) {
            try {
                val response = service.getProfile(
                    GraphQLRequest(
                        query = problemsQuery,
                        variables = mapOf("categorySlug" to "", "skip" to 0, "limit" to 50) // Adjust limit as needed
                    )
                )
                val questions = response.data?.problemsetQuestionList?.questions ?: emptyList()
                val entities = questions.map { q ->
                    ProblemEntity(
                        questionId = q.questionId,
                        questionFrontendId = q.questionFrontendId,
                        title = q.title,
                        titleSlug = q.titleSlug,
                        difficulty = q.difficulty,
                        isPaidOnly = q.isPaidOnly,
                        acRate = q.acRate,
                        tags = q.topicTags?.joinToString(",") { it.name } ?: ""
                    )
                }
                problemsDao.insertProblems(entities)
                emit(entities)
            } catch (e: Exception) {
                Log.e("LeetCodeRepository", "Failed to fetch problems", e)
            }
        }
    }

    fun getProblemDetail(slug: String): Flow<ProblemEntity?> = flow {
        val cached = problemsDao.getProblemBySlug(slug)
        if (cached != null) emit(cached)

        if (cached?.content == null) {
            try {
                val response = service.getProfile(
                    GraphQLRequest(
                        query = questionDetailQuery,
                        variables = mapOf("titleSlug" to slug)
                    )
                )
                val detail = response.data?.question
                if (detail != null) {
                    val content = detail.content ?: "No content available"
                    problemsDao.updateProblemContent(slug, content, System.currentTimeMillis())
                    emit(problemsDao.getProblemBySlug(slug))
                }
            } catch (e: Exception) {
                Log.e("LeetCodeRepository", "Failed to fetch problem detail", e)
            }
        }
    }

    suspend fun searchProblems(query: String) = problemsDao.searchProblems(query)
    suspend fun filterByDifficulty(difficulty: String) = problemsDao.filterByDifficulty(difficulty)
    suspend fun filterByTag(tag: String) = problemsDao.filterByTag(tag)

    companion object {
        @Volatile
        private var INSTANCE: LeetCodeRepository? = null

        fun getInstance(context: Context): LeetCodeRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LeetCodeRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
