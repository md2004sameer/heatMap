package com.example.heatmap.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.heatmap.MatchedUser
import com.example.heatmap.RecentSubmission
import com.example.heatmap.StreakCounter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri

@Composable
fun SubmissionStatsCard(user: MatchedUser, submissionByDate: Map<LocalDate, Int>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161b22)),
        border = BorderStroke(1.dp, Color(0xFF30363d)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            val totalSubYear = submissionByDate.values.sum()

            Text(
                text = "$totalSubYear",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "submissions in the past one year",
                fontSize = 14.sp,
                color = Color(0xFF8b949e)
            )
            Spacer(Modifier.height(8.dp))
            Text("Total active days: ${user.userCalendar?.totalActiveDays ?: 0}", color = Color.White, fontSize = 14.sp)

            Text("Max streak (Year): ${user.userCalendar?.streak ?: 0}", color = Color.White, fontSize = 14.sp)

            val currentMonth = LocalDate.now().month
            val subThisMonth = submissionByDate.filter { it.key.month == currentMonth && it.key.year == LocalDate.now().year }.values.sum()
            Text("Submissions this month: $subThisMonth", color = Color.White, fontSize = 14.sp)

            Spacer(Modifier.height(16.dp))

            SubmissionHeatMap(user.userCalendar?.submissionCalendar ?: "{}")
        }
    }
}

@Composable
fun SubmissionHeatMap(calendarJson: String) {
    var selectedInfo by remember { mutableStateOf<String?>(null) }

    val submissionByDate = remember(calendarJson) {
        try {
            val type = object : TypeToken<Map<String, Int>>() {}.type
            val rawMap = Gson().fromJson<Map<String, Int>>(calendarJson, type)
            rawMap?.entries?.associate { (tsStr, count) ->
                val ts = tsStr.toLong()
                val date = if (tsStr.length > 10) {
                    Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).toLocalDate()
                } else {
                    Instant.ofEpochSecond(ts).atZone(ZoneId.systemDefault()).toLocalDate()
                }
                date to count
            } ?: emptyMap()
        } catch (_: Exception) {
            emptyMap()
        }
    }

    val today = LocalDate.now()
    val startDate = remember {
        var start = LocalDate.of(today.year, 1, 1)
        while (start.dayOfWeek != java.time.DayOfWeek.SUNDAY) {
            start = start.minusDays(1)
        }
        start
    }

    val weeksCount = remember(startDate) {
        val endOfYear = LocalDate.of(today.year, 12, 31)
        var count = 0
        var current = startDate
        while (current.isBefore(endOfYear) || current.isEqual(endOfYear)) {
            current = current.plusWeeks(1)
            count++
        }
        count.coerceAtLeast(53)
    }

    val weeks = remember(startDate, weeksCount) {
        (0 until weeksCount).map { w ->
            val weekStart = startDate.plusWeeks(w.toLong())
            (0..6).map { d -> weekStart.plusDays(d.toLong()) }
        }
    }

    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMM", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0d1117), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFF30363d), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val groupedWeeks = weeks.groupBy { it.find { d -> d.year == today.year }?.month }

                    groupedWeeks.forEach { (month, monthWeeks) ->
                        Column {
                            Box(modifier = Modifier.height(22.dp)) {
                                val firstValidDate = monthWeeks.first().find { it.year == today.year }
                                if (firstValidDate != null) {
                                    Text(
                                        text = firstValidDate.format(monthFormatter),
                                        color = Color(0xFF8b949e),
                                        fontSize = 10.sp,
                                        modifier = Modifier.width(40.dp)
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                monthWeeks.forEach { weekDays ->
                                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        weekDays.forEach { date ->
                                            if (date.year != today.year || date.month != month) {
                                                Box(Modifier.size(11.dp))
                                            } else if (date.isAfter(today)) {
                                                Box(
                                                    Modifier
                                                        .size(11.dp)
                                                        .background(Color(0xFF161b22), RoundedCornerShape(2.dp))
                                                )
                                            } else {
                                                val count = submissionByDate[date] ?: 0
                                                val color = getHeatmapColor(count)

                                                Box(
                                                    Modifier
                                                        .size(11.dp)
                                                        .background(color, RoundedCornerShape(2.dp))
                                                        .clickable {
                                                            selectedInfo = "${if (count == 0) "No" else count} submissions on ${date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault()))}"
                                                        }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedInfo ?: "Tap a square for details",
                color = Color(0xFF8b949e),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )

            HeatMapLegend()
        }
    }
}

fun getHeatmapColor(count: Int): Color {
    return when {
        count == 0 -> Color(0xFF161B22)
        count <= 2 -> Color(0xFF0E4429)
        count <= 5 -> Color(0xFF006D32)
        count <= 10 -> Color(0xFF26A641)
        else -> Color(0xFF39D353)
    }
}

@Composable
fun HeatMapLegend() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Less", color = Color(0xFF8B949E), fontSize = 10.sp)
        listOf(
            Color(0xFF161B22),
            Color(0xFF0E4429),
            Color(0xFF006D32),
            Color(0xFF26A641),
            Color(0xFF39D353)
        ).forEach { color ->
            Box(Modifier.size(10.dp).background(color, RoundedCornerShape(2.dp)).border(0.5.dp, Color(0xFF30363D), RoundedCornerShape(2.dp)))
        }
        Text("More", color = Color(0xFF8B949E), fontSize = 10.sp)
    }
}

@Composable
fun StreakStatusCard(streakCounter: StreakCounter?, submissionsToday: Int) {
    if (streakCounter == null) return
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161b22)),
        border = BorderStroke(1.dp, Color(0xFF30363d)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Daily Activity & Streak", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "${streakCounter.streakCount}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFffa116)
                    )
                    Text(text = "Current Streak", color = Color.Gray, fontSize = 12.sp)
                }

                Column(horizontalAlignment = Alignment.End) {
                    val statusColor = if (streakCounter.currentDayCompleted) Color(0xFF00b8a3) else Color(0xFFff375f)
                    val statusText = if (streakCounter.currentDayCompleted) "Today Completed" else "Today Pending"
                    val icon = if (streakCounter.currentDayCompleted) Icons.Default.CheckCircle else Icons.Default.Info

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = statusText,
                            color = statusColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "$submissionsToday submissions today",
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    if (streakCounter.daysSkipped > 0) {
                        Text(
                            text = "${streakCounter.daysSkipped} days skipped",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecentSubmissionsSection(submissions: List<RecentSubmission>) {
    if (submissions.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Recent Submissions",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161b22)),
            border = BorderStroke(1.dp, Color(0xFF30363d)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                submissions.take(5).forEachIndexed { index, submission ->
                    RecentSubmissionItem(submission)
                    if (index < submissions.take(5).size - 1) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFF30363d))
                    }
                }
            }
        }
    }
}

@Composable
fun RecentSubmissionItem(submission: RecentSubmission) {
    val context = LocalContext.current
    val statusColor = when (submission.statusDisplay) {
        "Accepted" -> Color(0xFF00b8a3)
        "Wrong Answer" -> Color(0xFFff375f)
        "Time Limit Exceeded" -> Color(0xFFffc01e)
        "Runtime Error" -> Color(0xFFff375f)
        else -> Color.Gray
    }

    val timeAgo = remember(submission.timestamp) {
        val seconds = try { System.currentTimeMillis() / 1000 - (submission.timestamp.toLongOrNull() ?: 0L) } catch (e: Exception) { 0L }
        when {
            seconds < 0 -> "just now"
            seconds < 60 -> "just now"
            seconds < 3600 -> "${seconds / 60}m ago"
            seconds < 86400 -> "${seconds / 3600}h ago"
            else -> "${seconds / 86400}d ago"
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val intent = Intent(Intent.ACTION_VIEW, "https://leetcode.com/problems/${submission.titleSlug}".toUri())
                context.startActivity(intent)
            }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                submission.title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${submission.lang} • $timeAgo",
                color = Color.Gray,
                fontSize = 12.sp
            )
        }

        Surface(
            color = statusColor.copy(alpha = 0.1f),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                submission.statusDisplay,
                color = statusColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}
