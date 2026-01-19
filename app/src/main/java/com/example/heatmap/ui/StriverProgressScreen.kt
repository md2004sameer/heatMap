package com.example.heatmap.ui

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.example.heatmap.domain.StriverProblem
import com.example.heatmap.ui.theme.LeetCodeGreen
import com.example.heatmap.ui.theme.LeetCodeOrange
import com.example.heatmap.ui.theme.LeetCodeRed
import com.example.heatmap.ui.theme.LeetCodeYellow

@Composable
fun StriverProgressScreen(
    problems: List<StriverProblem>,
    completedIds: Set<Int>,
    onToggleProblem: (Int) -> Unit
) {
    val context = LocalContext.current
    val totalProblems = problems.size
    val completedTotal = completedIds.size
    val overallPercentage = if (totalProblems > 0) (completedTotal * 100 / totalProblems) else 0

    val easyProblems = problems.filter { it.difficulty == "Easy" }
    val mediumProblems = problems.filter { it.difficulty == "Medium" }
    val hardProblems = problems.filter { it.difficulty == "Hard" }

    val completedEasy = easyProblems.count { it.id in completedIds }
    val completedMedium = mediumProblems.count { it.id in completedIds }
    val completedHard = hardProblems.count { it.id in completedIds }

    val sections = remember(problems) { problems.map { it.section }.distinct() }
    var expandedSections by remember { mutableStateOf(setOf<String>()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0d1117)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Prominent Top Summary
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161b22)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$overallPercentage%",
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Black,
                        color = LeetCodeOrange
                    )
                    Text(
                        text = "TOTAL PROGRESS",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "$completedTotal / $totalProblems Problems",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    
                    Spacer(Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        DifficultyStatVertical("EASY", completedEasy, easyProblems.size, LeetCodeGreen)
                        DifficultyStatVertical("MEDIUM", completedMedium, mediumProblems.size, LeetCodeYellow)
                        DifficultyStatVertical("HARD", completedHard, hardProblems.size, LeetCodeRed)
                    }
                }
            }
        }

        // 2. Expandable Topic Sections
        sections.forEach { section ->
            val sectionProblems = problems.filter { it.section == section }
            val completedCount = sectionProblems.count { it.id in completedIds }
            val isExpanded = section in expandedSections

            item {
                LargeTopicHeader(
                    title = section,
                    completed = completedCount,
                    total = sectionProblems.size,
                    isExpanded = isExpanded,
                    onClick = {
                        expandedSections = if (isExpanded) expandedSections - section else expandedSections + section
                    }
                )
            }

            if (isExpanded) {
                items(sectionProblems, key = { "striver_${it.id}" }) { problem ->
                    LargeStriverProblemCard(
                        problem = problem,
                        isCompleted = problem.id in completedIds,
                        onToggle = { onToggleProblem(problem.id) },
                        onSolve = {
                            if (problem.resources.solve.isNotEmpty()) {
                                val intent = Intent(Intent.ACTION_VIEW, problem.resources.solve.toUri())
                                context.startActivity(intent)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DifficultyStatVertical(label: String, completed: Int, total: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Black)
        Text(text = "$completed/$total", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LargeTopicHeader(
    title: String,
    completed: Int,
    total: Int,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val progress = if (total > 0) completed.toFloat() / total else 0f
    
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) Color(0xFF21262d) else Color(0xFF161b22)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "$completed / $total COMPLETED",
                        color = if (completed == total && total > 0) LeetCodeGreen else LeetCodeOrange,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            if (!isExpanded) {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp).background(Color.Black, RoundedCornerShape(2.dp)),
                    color = if (completed == total) LeetCodeGreen else LeetCodeOrange,
                    trackColor = Color(0xFF0d1117)
                )
            }
        }
    }
}

@Composable
private fun LargeStriverProblemCard(
    problem: StriverProblem,
    isCompleted: Boolean,
    onToggle: () -> Unit,
    onSolve: () -> Unit
) {
    val difficultyColor = when (problem.difficulty) {
        "Easy" -> LeetCodeGreen
        "Medium" -> LeetCodeYellow
        "Hard" -> LeetCodeRed
        else -> Color.Gray
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161b22).copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        border = if (isCompleted) BorderStroke(1.dp, LeetCodeGreen.copy(alpha = 0.2f)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggle,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (isCompleted) LeetCodeGreen else Color.Gray,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onSolve)
            ) {
                Text(
                    text = problem.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCompleted) Color.Gray else Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = difficultyColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = problem.difficulty.uppercase(),
                            color = difficultyColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (isCompleted) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "SOLVED",
                            color = LeetCodeGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.DarkGray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
