package com.example.heatmap

import android.app.WallpaperManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class WallpaperWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("leetcode_prefs", Context.MODE_PRIVATE)
        val username = prefs.getString("last_username", null) ?: return@withContext Result.success()

        Log.d("WallpaperWorker", "Executing wallpaper update for $username")

        val repository = LeetCodeRepository.getInstance(context)
        
        var latestData: LeetCodeData? = null
        try {
            // Collecting the first emitted item and then closing the flow to avoid blocking
            repository.getProfile(username).collect { data ->
                if (data != null) {
                    latestData = data
                }
            }
        } catch (e: Exception) {
            Log.e("WallpaperWorker", "Error fetching profile", e)
        }

        val dataToApply = latestData
        if (dataToApply != null) {
            try {
                val target = prefs.getInt("wallpaper_target", 
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) 
                        WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK 
                    else 0
                )

                WallpaperUtils.applyWallpaper(context, dataToApply, target)
                prefs.edit().putLong("last_update_ts", System.currentTimeMillis()).apply()
                
                return@withContext Result.success()
            } catch (e: Exception) {
                Log.e("WallpaperWorker", "Error applying wallpaper", e)
                return@withContext Result.retry()
            }
        }

        Result.retry()
    }

    companion object {
        fun enqueue(context: Context) {
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
