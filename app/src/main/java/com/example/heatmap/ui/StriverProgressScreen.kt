package com.example.heatmap.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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

    // Dynamically extract sections from problems to stay synchronized with the data
    val sections = remember(problems) { problems.map { it.section }.distinct() }
    
    // State to track expanded sections
    var expandedSections by remember { mutableStateOf(setOf<String>()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0d1117)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Percentage Top Summary
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$overallPercentage%",
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Black,
                    color = LeetCodeOrange
                )
                Text(
                    text = "Overall Progress",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray
                )
                Text(
                    text = "$completedTotal/$totalProblems",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // 2. Difficulty Breakdown
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161b22)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DifficultyRow("Easy", completedEasy, easyProblems.size, LeetCodeGreen)
                    DifficultyRow("Medium", completedMedium, mediumProblems.size, LeetCodeYellow)
                    DifficultyRow("Hard", completedHard, hardProblems.size, LeetCodeRed)
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }

        // 3. Expandable Topic Sections
        sections.forEach { section ->
            val sectionProblems = problems.filter { it.section == section }
            val completedCount = sectionProblems.count { it.id in completedIds }
            val isExpanded = section in expandedSections

            item {
                TopicHeader(
                    title = section,
                    completed = completedCount,
                    total = sectionProblems.size,
                    isExpanded = isExpanded,
                    onClick = {
                        expandedSections = if (isExpanded) {
                            expandedSections - section
                        } else {
                            expandedSections + section
                        }
                    }
                )
            }

            if (isExpanded) {
                items(sectionProblems, key = { "striver_${it.id}" }) { problem ->
                    StriverProblemCard(
                        problem = problem,
                        isCompleted = problem.id in completedIds,
                        onToggle = { onToggleProblem(problem.id) },
                        onSolve = {
                            if (problem.resources.solve.isNotEmpty()) {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(problem.resources.solve))
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
private fun TopicHeader(
    title: String,
    completed: Int,
    total: Int,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) Color(0xFF21262d) else Color(0xFF161b22)
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$completed/$total Problems Done",
                    color = if (completed == total && total > 0) LeetCodeGreen else Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = LeetCodeOrange
            )
        }
    }
}

@Composable
private fun StriverProblemCard(
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0d1117)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggle,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Toggle Complete",
                    tint = if (isCompleted) LeetCodeGreen else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onSolve)
            ) {
                Text(
                    text = problem.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isCompleted) Color.Gray else Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = problem.difficulty,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = difficultyColor
                )
            }

            if (isCompleted) {
                Text(
                    text = "DONE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = LeetCodeGreen.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun DifficultyRow(label: String, completed: Int, total: Int, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(text = "$completed/$total", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}
