package com.example.heatmap

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat

class ContestReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("contest_title") ?: "LeetCode Contest"
        val slug = intent.getStringExtra("contest_slug") ?: ""
        val type = intent.getStringExtra("type") ?: "start" // "pre", "start", or "summary"
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "contest_reminders"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, 
                "Contest Reminders", 
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for upcoming LeetCode contests"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val message = when (type) {
            "pre" -> "Starts in 1 hour! Get your setup ready."
            "start" -> "The contest has started! Time to solve some problems."
            "summary" -> "Contest ended! Check your performance and ranking summary."
            else -> "Contest update"
        }

        val contestUri = Uri.parse("https://leetcode.com/contest/$slug")
        val contestIntent = Intent(Intent.ACTION_VIEW, contestUri)
        val pendingIntent = PendingIntent.getActivity(
            context, 
            slug.hashCode() + type.hashCode(), 
            contestIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .build()

        val notificationId = slug.hashCode() + when(type) {
            "pre" -> 100
            "start" -> 200
            "summary" -> 300
            else -> 0
        }
        
        notificationManager.notify(notificationId, notification)
    }
}
