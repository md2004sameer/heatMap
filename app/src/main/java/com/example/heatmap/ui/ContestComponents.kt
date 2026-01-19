package com.example.heatmap.ui

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.example.heatmap.Contest
import com.example.heatmap.DailyChallenge
import com.example.heatmap.UserContestRanking
import com.example.heatmap.ui.theme.LeetCodeGreen
import com.example.heatmap.ui.theme.LeetCodeOrange
import com.example.heatmap.ui.theme.LeetCodeRed
import com.example.heatmap.ui.theme.LeetCodeYellow
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit

@Composable
fun ContestRankingCard(contest: UserContestRanking) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161b22)),
        border = BorderStroke(1.dp, Color(0xFF30363d)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Contest Ranking", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(12.dp))
            InfoRow("Rating", contest.rating.toInt().toString())
            InfoRow("Global Ranking", "#${contest.globalRanking}")
            InfoRow("Percentile", "Top ${contest.topPercentage}%")
        }
    }
}

@Composable
fun UpcomingContestsSection(contests: List<Contest>) {
    if (contests.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Upcoming Contests",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        contests.forEach { contest ->
            ContestCard(contest)
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
fun ContestCard(contest: Contest) {
    val context = LocalContext.current
    var timeLeft by remember { mutableLongStateOf(contest.startTime * 1000 - System.currentTimeMillis()) }

    LaunchedEffect(key1 = contest.startTime) {
        while (timeLeft > 0) {
            delay(1000)
            timeLeft = contest.startTime * 1000 - System.currentTimeMillis()
        }
    }

    val formattedTimeLeft = remember(timeLeft) {
        if (timeLeft <= 0) "Started"
        else {
            val hours = TimeUnit.MILLISECONDS.toHours(timeLeft)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(timeLeft) % 60
            val seconds = TimeUnit.MILLISECONDS.toSeconds(timeLeft) % 60
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        }
    }

    val startDateTime = remember(contest.startTime) {
        try {
            LocalDateTime.ofInstant(Instant.ofEpochSecond(contest.startTime), ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("MMM dd, hh:mm a", Locale.getDefault()))
        } catch (e: Exception) {
            "TBD"
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161b22)),
        border = BorderStroke(1.dp, Color(0xFF30363d)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        contest.title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Starts: $startDateTime",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                    Text(
                        "Duration: ${contest.duration / 60} mins",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }

                Surface(
                    color = if (timeLeft > 0) Color(0xFFffa116).copy(alpha = 0.1f) else Color.Red.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        formattedTimeLeft,
                        color = if (timeLeft > 0) Color(0xFFffa116) else Color.Red,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, "https://leetcode.com/contest/${contest.titleSlug}".toUri())
                        context.startActivity(intent)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFffa116)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Register", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(
                    onClick = { 
                        try {
                            // Assuming ContestUtils is available
                            com.example.heatmap.ui.ContestUtils.scheduleContestReminder(context, contest) 
                        } catch (e: Exception) {}
                    },
                    modifier = Modifier
                        .background(Color(0xFF2d333b), RoundedCornerShape(8.dp))
                        .size(40.dp)
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = "Reminder", tint = Color(0xFFffa116))
                }

                IconButton(
                    onClick = { 
                        try {
                            com.example.heatmap.ui.ContestUtils.addContestToCalendar(context, contest) 
                        } catch (e: Exception) {}
                    },
                    modifier = Modifier
                        .background(Color(0xFF2d333b), RoundedCornerShape(8.dp))
                        .size(40.dp)
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = "Calendar", tint = Color(0xFFffa116))
                }
            }
        }
    }
}

@Composable
fun DailyChallengeCard(challenge: DailyChallenge) {
    val context = LocalContext.current
    val question = challenge.question
    if (question == null) return

    val isSolved = challenge.userStatus == "Finish"
    val difficultyColor = when (question.difficulty) {
        "Easy" -> LeetCodeGreen
        "Medium" -> LeetCodeYellow
        "Hard" -> LeetCodeRed
        else -> Color.Gray
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161b22)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isSolved) LeetCodeGreen.copy(alpha = 0.3f) else Color(0xFF30363d))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "LeetCode Daily — ${challenge.date ?: "Today"}",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = question.title ?: "No Title",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            challenge.link?.let { link ->
                                val intent = Intent(Intent.ACTION_VIEW, "https://leetcode.com$link".toUri())
                                context.startActivity(intent)
                            }
                        }
                    )
                }
                
                if (isSolved) {
                    Icon(
                        imageVector = Icons.Default.Done,
                        contentDescription = "Solved",
                        tint = LeetCodeGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Text(text = "Difficulty", color = Color.Gray, fontSize = 10.sp)
                    Text(text = question.difficulty ?: "Unknown", color = difficultyColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text(text = "Status", color = Color.Gray, fontSize = 10.sp)
                    Text(text = if (isSolved) "Completed" else "Pending", color = if (isSolved) LeetCodeGreen else Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
