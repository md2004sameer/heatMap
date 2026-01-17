package com.example.heatmap

import android.app.WallpaperManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.work.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*
import java.util.concurrent.TimeUnit

class WallpaperWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("leetcode_prefs", Context.MODE_PRIVATE)
        val username = prefs.getString("last_username", null) ?: return@withContext Result.success()

        Log.d("WallpaperWorker", "Executing wallpaper update for $username")

        val service = Retrofit.Builder()
            .baseUrl("https://leetcode.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LeetCodeService::class.java)

        val query = """
            query(${"$"}username: String!, ${"$"}year: Int!) {
                allQuestionsCount { difficulty count }
                matchedUser(username: ${"$"}username) {
                    username
                    profile { realName countryName ranking skillTags }
                    submitStats {
                        acSubmissionNum { difficulty count submissions }
                        totalSubmissionNum { difficulty count submissions }
                    }
                    userCalendar(year: ${"$"}year) {
                      activeYears streak totalActiveDays submissionCalendar
                    }
                }
                userContestRanking(username: ${"$"}username) {
                    rating globalRanking totalParticipants topPercentage
                }
                streakCounter { streakCount daysSkipped currentDayCompleted }
            }
        """.trimIndent()

        // 1. Try Network
        val newData = try {
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val response = service.getProfile(
                GraphQLRequest(
                    query = query,
                    variables = mapOf("username" to username, "year" to currentYear)
                )
            )
            response.data
        } catch (e: Exception) {
            Log.e("WallpaperWorker", "Network fetch failed, falling back to cache", e)
            null
        }

        // 2. Resolve Data (New or Cached)
        val dataToRender = if (newData?.matchedUser != null) {
            prefs.edit().putString("last_data_json", Gson().toJson(newData)).apply()
            newData
        } else {
            val cachedJson = prefs.getString("last_data_json", null)
            if (cachedJson != null) {
                Gson().fromJson(cachedJson, LeetCodeData::class.java)
            } else {
                null
            }
        }

        // 3. Render & Apply
        if (dataToRender != null) {
            try {
                val target = prefs.getInt("wallpaper_target", 
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) 
                        WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK 
                    else 0
                )

                WallpaperUtils.applyWallpaper(context, dataToRender, target)
                prefs.edit().putLong("last_update_ts", System.currentTimeMillis()).apply()
                
                // If we used cache, return retry so WorkManager tries for fresh stats again later
                return@withContext if (newData != null) Result.success() else Result.retry()
            } catch (e: Exception) {
                return@withContext Result.retry()
            }
        }

        Result.retry()
    }

    companion object {
        fun enqueue(context: Context) {
            // REMOVED Network constraint. 
            // We want the worker to run every 4 hours even offline to update the DATE.
            // The worker internally handles the network fetch vs cache fallback.
            val request = PeriodicWorkRequestBuilder<WallpaperWorker>(4, TimeUnit.HOURS)
                .addTag("wallpaper_update")
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "wallpaper_update",
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun runOnce(context: Context) {
            val request = OneTimeWorkRequestBuilder<WallpaperWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .addTag("immediate_update")
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "immediate_update",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
