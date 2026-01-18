package com.example.heatmap

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import java.util.Calendar
import java.util.concurrent.TimeUnit

class ReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val reminders = listOf(
        "Brain feels hungry? Feed it 1 Medium Problem.",
        "Your DP skills are low on protein. Add 2 servings now.",
        "Chef’s Recommendation: Arrays Easy + Hashmap Medium.",
        "Streak calories burning! Solve 1 problem to refill.",
        "Healthy routine alert: 1 Easy + 1 Medium = Balanced diet.",
        "Interviewers asking about DP. You still ignoring DP?",
        "Your friend solved 5 problems today. You solved 0. Just saying.",
        "Arrays easy ho gaya. Trees kab karoge?",
        "Don’t break the streak — today’s problem is small and cute.",
        "You can’t master DP by skipping DP.",
        "Arrays are over. Time to face Trees."
    )

    override suspend fun doWork(): Result {
        val prefs = context.getSharedPreferences("leetcode_prefs", Context.MODE_PRIVATE)
        val username = prefs.getString("last_username", null) ?: return Result.success()

        val repository = LeetCodeRepository.getInstance(context)
        var leetCodeData: LeetCodeData? = null
        
        try {
            repository.getProfile(username).collect { data ->
                if (data != null) {
                    leetCodeData = data
                }
            }
        } catch (e: Exception) {
            Log.e("ReminderWorker", "Failed to fetch data", e)
        }

        val data = leetCodeData ?: return Result.retry()

        // 1. Check if any submission was made today (Streak maintained)
        val isStreakMaintained = data.streakCounter?.currentDayCompleted == true
        
        // 2. Check if the Daily Coding Challenge specifically is completed
        // userStatus is usually "Finish" or "NotStart"
        val isDailyChallengeDone = data.activeDailyCodingChallengeQuestion?.userStatus == "Finish"

        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val isEvening = currentHour >= 18 // After 6 PM

        if (isStreakMaintained && isDailyChallengeDone) {
            Log.d("ReminderWorker", "Everything completed for today. Skipping reminder.")
            return Result.success()
        }

        // Send a specialized reminder if it's evening and the Daily Challenge is pending
        if (isEvening && !isDailyChallengeDone) {
            sendDailyChallengeReminder(data)
        } else if (!isStreakMaintained) {
            // General streak reminder
            sendGeneralReminder(data)
        }

        return Result.success()
    }

    private fun sendDailyChallengeReminder(data: LeetCodeData) {
        val challenge = data.activeDailyCodingChallengeQuestion ?: return
        val question = challenge.question
        val title = "🌙 Evening Check-in"
        val message = "You haven’t attempted today’s daily challenge yet: \"${question?.title ?: "Daily Challenge"}\"."
        val bigText = "$message\n\nDon't let the day end without a small win. Solving the Daily Challenge is the best way to keep your skills sharp and your streak alive!"
        
        val link = "https://leetcode.com${challenge.link ?: ""}"
        sendNotification(title, message, bigText, link)
    }

    private fun sendGeneralReminder(data: LeetCodeData) {
        val streak = data.streakCounter?.streakCount ?: data.matchedUser?.userCalendar?.streak ?: 0
        val acStats = data.matchedUser?.submitStats?.acSubmissionNum
        val totalSolved = acStats?.find { it.difficulty == "All" }?.count ?: 0
        
        val titles = listOf(
            "🔥 $streak Day Streak! Keep it going?",
            "🚀 Ready for a challenge today?",
            "📈 Progress Update: $totalSolved Solved",
            "💡 Time to sharpen those skills!"
        )

        val reminder = reminders.random()
        val bigText = "$reminder\n\nConsistent practice is the key to mastery. Even one Easy problem keeps the momentum going!"
        
        sendNotification(titles.random(), reminder, bigText, "https://leetcode.com/problemset/all/")
    }

    private fun sendNotification(title: String, content: String, bigText: String, link: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "leetcode_reminder"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, 
                "LeetCode Reminders", 
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily reminders to maintain your LeetCode streak"
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val appIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val appPendingIntent = PendingIntent.getActivity(
            context, 0, appIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val leetCodeIntent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
        val leetCodePendingIntent = PendingIntent.getActivity(
            context, 1, leetCodeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(appPendingIntent)
            .addAction(android.R.drawable.ic_menu_compass, "Solve Now", leetCodePendingIntent)
            .build()

        notificationManager.notify(1001, notification)
    }

    companion object {
        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(4, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .addTag("leetcode_reminder")
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "leetcode_reminder",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
