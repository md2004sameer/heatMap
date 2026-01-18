package com.example.heatmap

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.delay
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
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36")
                .header("Referer", "https://leetcode.com/problemset/all/")
                .build()
            chain.proceed(request)
        }
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private val service = Retrofit.Builder()
        .baseUrl("https://leetcode.com/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(LeetCodeService::class.java)

    private val profileQuery = """
        query getProfile(${"$"}username: String!, ${"$"}year: Int!) {
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

    /**
     * Uses the LeetCode REST API to fetch all problems in a single call.
     * This is often more reliable than manual pagination via GraphQL for the initial sync.
     */
    fun getAllProblems(refresh: Boolean = false): Flow<List<ProblemEntity>> = flow {
        val cached = problemsDao.getAllProblems()
        if (cached.isNotEmpty()) emit(cached)

        if (refresh || cached.isEmpty()) {
            try {
                Log.d("LeetCodeRepository", "Fetching all problems via REST API")
                val response = service.getAllProblemsRest()
                
                val entities = response.stat_status_pairs.map { pair ->
                    val difficultyStr = when (pair.difficulty.level) {
                        1 -> "Easy"
                        2 -> "Medium"
                        3 -> "Hard"
                        else -> "Unknown"
                    }
                    
                    val acRate = if (pair.stat.total_submitted > 0) {
                        (pair.stat.total_acs.toDouble() / pair.stat.total_submitted.toDouble()) * 100.0
                    } else 0.0

                    ProblemEntity(
                        questionId = pair.stat.question_id.toString(),
                        questionFrontendId = pair.stat.frontend_question_id.toString(),
                        title = pair.stat.question__title,
                        titleSlug = pair.stat.question__title_slug,
                        difficulty = difficultyStr,
                        isPaidOnly = pair.paid_only,
                        acRate = acRate,
                        tags = "" // REST API doesn't provide tags in this call, can be updated later if needed
                    )
                }

                if (entities.isNotEmpty()) {
                    problemsDao.insertProblems(entities)
                    Log.d("LeetCodeRepository", "Successfully synced ${entities.size} problems.")
                    emit(problemsDao.getAllProblems())
                }
            } catch (e: Exception) {
                Log.e("LeetCodeRepository", "Failed to fetch problems via REST", e)
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
