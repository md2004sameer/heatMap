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

    fun getProfile(username: String): Flow<LeetCodeData?> = flow {
        // 1. Emit cached data immediately if available
        val cached = dao.getCachedData(username)
        if (cached != null) {
            try {
                val data = gson.fromJson(cached.jsonData, LeetCodeData::class.java)
                emit(data)
            } catch (e: Exception) {
                Log.e("LeetCodeRepository", "Error parsing cached data", e)
            }
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
            } else {
                Log.e("LeetCodeRepository", "No matched user in response: ${response.errors}")
                if (cached == null) emit(null)
            }
        } catch (e: Exception) {
            Log.e("LeetCodeRepository", "Network fetch failed", e)
            if (cached == null) emit(null)
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
