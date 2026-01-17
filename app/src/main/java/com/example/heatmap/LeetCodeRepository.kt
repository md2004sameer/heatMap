package com.example.heatmap

import android.content.Context
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
    private val gson = Gson()
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
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
            matchedUser(username: ${"$"}username) {
                username
                profile {
                    realName
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
            streakCounter {
                streakCount
                daysSkipped
                currentDayCompleted
            }
        }
    """.trimIndent()

    fun getProfile(username: String): Flow<LeetCodeData?> = flow {
        // 1. Emit cached data immediately if available
        val cached = dao.getCachedData(username)
        if (cached != null) {
            emit(gson.fromJson(cached.jsonData, LeetCodeData::class.java))
        }

        // 2. Fetch from network and update cache
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
            // Keep existing cache on failure
        }
    }

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
