package com.example.heatmap.ui

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.CalendarContract
import android.provider.Settings
import android.widget.Toast
import com.example.heatmap.Contest
import com.example.heatmap.ContestReminderReceiver
import java.util.concurrent.TimeUnit

object ContestUtils {
    fun addContestToCalendar(context: Context, contest: Contest) {
        val intent = Intent(Intent.ACTION_INSERT)
            .setData(CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.Events.TITLE, contest.title)
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, contest.startTime * 1000)
            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, (contest.startTime + contest.duration) * 1000)
            .putExtra(CalendarContract.Events.DESCRIPTION, "LeetCode Contest: https://leetcode.com/contest/${contest.titleSlug}")
            .putExtra(CalendarContract.Events.EVENT_LOCATION, "LeetCode")
        context.startActivity(intent)
    }

    fun scheduleContestReminder(context: Context, contest: Contest) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                return
            }
        }

        val startTimeMillis = contest.startTime * 1000
        val preTimeMillis = startTimeMillis - TimeUnit.HOURS.toMillis(1)
        val endTimeMillis = startTimeMillis + (contest.duration * 1000L)

        fun createPendingIntent(type: String): PendingIntent {
            val intent = Intent(context, ContestReminderReceiver::class.java).apply {
                putExtra("contest_title", contest.title)
                putExtra("contest_slug", contest.titleSlug)
                putExtra("type", type)
            }
            val requestCode = contest.titleSlug.hashCode() + type.hashCode()
            return PendingIntent.getBroadcast(
                context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        if (preTimeMillis > System.currentTimeMillis()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, preTimeMillis, createPendingIntent("pre"))
        }

        if (startTimeMillis > System.currentTimeMillis()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, startTimeMillis, createPendingIntent("start"))
        }

        if (endTimeMillis > System.currentTimeMillis()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endTimeMillis, createPendingIntent("summary"))
        }

        Toast.makeText(context, "Reminders set for ${contest.title}", Toast.LENGTH_SHORT).show()
    }
}
