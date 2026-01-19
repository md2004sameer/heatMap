package com.example.heatmap.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
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
import com.example.heatmap.DailyTrainingPlan
import com.example.heatmap.LeetCodeData
import com.example.heatmap.domain.GfgPotdEntity
import com.example.heatmap.ui.theme.LeetCodeGreen
import com.example.heatmap.ui.theme.LeetCodeOrange
import com.example.heatmap.ui.theme.LeetCodeRed
import com.example.heatmap.ui.theme.LeetCodeYellow

@Composable
fun TrainingPlanSection(
    plan: DailyTrainingPlan?,
    onGenerate: () -> Unit,
    onToggleTask: (String) -> Unit,
    onAddTask: (String, String, String, Int) -> Unit,
    onDeleteTask: (String) -> Unit
) {
    // This section can be updated to include the new GFG POTD logic
}

@Composable
fun DailyChallengeContent(
    leetCodeData: LeetCodeData,
    gfgPotdList: List<GfgPotdEntity>
) {
    val context = LocalContext.current
    var selectedSort by remember { mutableStateOf("Newest First") }
    
    val sortedGfgList = remember(gfgPotdList, selectedSort) {
        when (selectedSort) {
            "Newest First" -> gfgPotdList.sortedByDescending { it.date }
            "Difficulty" -> gfgPotdList.sortedBy { it.difficulty }
            "Status" -> gfgPotdList.sortedBy { it.isSolved }
            else -> gfgPotdList
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. LeetCode Daily (Standard)
        item {
            Text("LeetCode Daily", style = MaterialTheme.typography.titleMedium, color = LeetCodeOrange)
            Spacer(Modifier.height(8.dp))
            leetCodeData.activeDailyCodingChallengeQuestion?.let { challenge ->
                DailyChallengeCard(challenge)
            }
        }

        // 2. GFG POTD Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("GeeksforGeeks POTD", style = MaterialTheme.typography.titleMedium, color = LeetCodeOrange)
                
                // Sorting Menu
                Box {
                    var expanded by remember { mutableStateOf(false) }
                    TextButton(onClick = { expanded = true }) {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(selectedSort, fontSize = 12.sp)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        listOf("Newest First", "Difficulty", "Status").forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    selectedSort = option
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        if (gfgPotdList.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161b22))
                ) {
                    Text(
                        "Unable to fetch POTD today or no history found.",
                        modifier = Modifier.padding(16.dp),
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            items(sortedGfgList, key = { it.date }) { potd ->
                GfgPotdCard(
                    potd = potd,
                    onOpenLink = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(potd.problemUrl))
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
fun GfgPotdCard(
    potd: GfgPotdEntity,
    onOpenLink: () -> Unit
) {
    val difficultyColor = when (potd.difficulty) {
        "Easy" -> LeetCodeGreen
        "Medium" -> LeetCodeYellow
        "Hard" -> LeetCodeRed
        else -> Color.Gray
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161b22)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (potd.isSolved) LeetCodeGreen.copy(alpha = 0.3f) else Color.Transparent)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "POTD — ${potd.date}",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = potd.problemName,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(onClick = onOpenLink)
                    )
                }
                
                // Read-only status icon
                Icon(
                    imageVector = if (potd.isSolved) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (potd.isSolved) "Solved" else "Unsolved",
                    tint = if (potd.isSolved) LeetCodeGreen else Color.Gray,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InfoStat(label = "Difficulty", value = potd.difficulty, color = difficultyColor)
                InfoStat(label = "Accuracy", value = "${potd.accuracy}%", color = Color.White)
                InfoStat(label = "Submissions", value = potd.totalSubmissions.toString(), color = Color.White)
            }

            if (potd.topicTags.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    potd.topicTags.split(",").take(3).forEach { tag ->
                        Surface(
                            color = Color.White.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = tag,
                                color = Color.Gray,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoStat(label: String, value: String, color: Color) {
    Column {
        Text(text = label, color = Color.Gray, fontSize = 10.sp)
        Text(text = value, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}
