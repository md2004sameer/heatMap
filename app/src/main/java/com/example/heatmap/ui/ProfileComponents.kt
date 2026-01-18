package com.example.heatmap.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.heatmap.DifficultyCount
import com.example.heatmap.MatchedUser
import com.example.heatmap.ui.theme.BorderDark
import com.example.heatmap.ui.theme.LeetCodeGreen
import com.example.heatmap.ui.theme.LeetCodeOrange
import com.example.heatmap.ui.theme.LeetCodeRed
import com.example.heatmap.ui.theme.LeetCodeYellow
import com.example.heatmap.ui.theme.SurfaceDark
import com.example.heatmap.ui.theme.Typography

@Composable
fun ProfileInfoCard(user: MatchedUser) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, BorderDark),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Profile Information", style = Typography.titleMedium, color = Color.White)
            Spacer(Modifier.height(12.dp))
            InfoRow("Username", user.username ?: "Unknown")
            InfoRow("Real Name", user.profile?.realName ?: "Not provided")
            InfoRow("Country", user.profile?.countryName ?: "Not specified")
            InfoRow("Global Rank", "#${user.profile?.ranking ?: "N/A"}")
        }
    }
}

@Composable
fun ProblemsSolvedCard(user: MatchedUser, allQuestions: List<DifficultyCount>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, BorderDark),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Problems Solved", style = Typography.titleMedium, color = Color.White)
            Spacer(Modifier.height(12.dp))
            val acStats = user.submitStats?.acSubmissionNum ?: emptyList()

            acStats.forEachIndexed { index, stat ->
                val total = allQuestions.getOrNull(index)?.count ?: 0
                ProblemStatItem(
                    difficulty = stat.difficulty ?: "Unknown", 
                    solved = stat.count, 
                    total = total, 
                    submissions = stat.submissions
                )
            }
        }
    }
}

@Composable
fun ProblemStatItem(difficulty: String, solved: Int, total: Int, submissions: Int) {
    val color = when(difficulty) {
        "Easy" -> LeetCodeGreen
        "Medium" -> LeetCodeYellow
        "Hard" -> LeetCodeRed
        else -> LeetCodeOrange
    }

    Column(Modifier.padding(vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(difficulty, color = color, style = Typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text("$solved / $total", color = Color.White, style = Typography.bodySmall)
        }
        val progress = if (total > 0) solved.toFloat() / total.toFloat() else 0f
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(8.dp).padding(top = 8.dp).clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = Color(0xFF2d333b),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        Text(
            text = "Total Submissions: $submissions",
            color = Color.Gray,
            style = Typography.labelSmall,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color(0xFF8b949e), style = Typography.bodyMedium)
        Text(value, color = Color.White, style = Typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
