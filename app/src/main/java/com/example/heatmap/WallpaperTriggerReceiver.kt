package com.example.heatmap

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.*

class WallpaperTriggerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("WallpaperTrigger", "Received trigger: ${intent.action}")
        
        // Trigger a one-time update immediately to refresh the wallpaper
        WallpaperWorker.runOnce(context)

        // If this was the midnight alarm, schedule the next one for tomorrow
        if (intent.action == ACTION_MIDNIGHT_UPDATE) {
            scheduleMidnightAlarm(context)
        }
    }

    companion object {
        const val ACTION_MIDNIGHT_UPDATE = "com.example.heatmap.ACTION_MIDNIGHT_UPDATE"

        fun scheduleMidnightAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, WallpaperTriggerReceiver::class.java).apply {
                action = ACTION_MIDNIGHT_UPDATE
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context, 
                0, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 5) // 5 seconds past midnight
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                    }
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
                Log.d("WallpaperTrigger", "Midnight alarm scheduled for ${calendar.time}")
            } catch (e: Exception) {
                Log.e("WallpaperTrigger", "Failed to schedule midnight alarm", e)
            }
        }
    }
}
