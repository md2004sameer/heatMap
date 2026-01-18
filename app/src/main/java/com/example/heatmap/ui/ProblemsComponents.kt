package com.example.heatmap.ui

import android.content.Intent
import android.net.Uri
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import com.example.heatmap.domain.Problem
import com.example.heatmap.domain.StriverProblem
import com.example.heatmap.ui.theme.LeetCodeGreen
import com.example.heatmap.ui.theme.LeetCodeOrange
import com.example.heatmap.ui.theme.LeetCodeRed
import com.example.heatmap.ui.theme.LeetCodeYellow

@Composable
fun ProblemsScreen(
    problems: List<Problem>,
    onProblemClick: (Problem) -> Unit,
    onSearch: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                onSearch(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            placeholder = { Text("Search problems...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LeetCodeOrange,
                cursorColor = LeetCodeOrange
            )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(problems) { problem ->
                ProblemItem(problem = problem, onClick = { onProblemClick(problem) })
            }
        }
    }
}

@Composable
fun ProblemItem(problem: Problem, onClick: () -> Unit) {
    val difficultyColor = when (problem.difficulty) {
        "Easy" -> LeetCodeGreen
        "Medium" -> LeetCodeYellow
        "Hard" -> LeetCodeRed
        else -> Color.Gray
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161b22)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${problem.frontendId}. ${problem.title}",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = problem.difficulty,
                        color = difficultyColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${(problem.acRate).toInt()}% Accepted",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
            if (problem.isPaidOnly) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Paid Only",
                    tint = LeetCodeYellow,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun ProblemDetailDialog(problem: Problem, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0d1117),
        title = { Text(problem.title, color = Color.White) },
        text = {
            Column(horizontalAlignment = Alignment.End) {
                Box(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                    AndroidView(
                        factory = { context ->
                            TextView(context).apply {
                                setTextColor(android.graphics.Color.LTGRAY)
                                textSize = 14f
                            }
                        },
                        update = { textView ->
                            textView.text = HtmlCompat.fromHtml(
                                problem.content ?: "Loading content...",
                                HtmlCompat.FROM_HTML_MODE_COMPACT
                            )
                        },
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    )
                }
                
                Spacer(Modifier.height(16.dp))
                
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://leetcode.com/problems/${problem.slug}/"))
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LeetCodeOrange),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Solve Now", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = LeetCodeOrange)
            }
        }
    )
}

@Composable
fun StriverSheetScreen(
    problems: List<StriverProblem>
) {
    val context = LocalContext.current
    val groupedProblems = remember(problems) {
        problems.groupBy { it.section }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        groupedProblems.forEach { (section, sectionProblems) ->
            item {
                Text(
                    text = section,
                    style = MaterialTheme.typography.titleLarge,
                    color = LeetCodeOrange,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            val subGrouped = sectionProblems.groupBy { it.subSection }
            subGrouped.forEach { (subSection, subProblems) ->
                if (subSection.isNotEmpty()) {
                    item {
                        Text(
                            text = subSection,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                        )
                    }
                }

                items(subProblems) { problem ->
                    StriverProblemItem(problem = problem) { url ->
                        if (url.isNotEmpty()) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StriverProblemItem(problem: StriverProblem, onUrlClick: (String) -> Unit) {
    val difficultyColor = when (problem.difficulty) {
        "Easy" -> LeetCodeGreen
        "Medium" -> LeetCodeYellow
        "Hard" -> LeetCodeRed
        else -> Color.Gray
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161b22)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = problem.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = difficultyColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = problem.difficulty,
                        color = difficultyColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (problem.resources.solve.isNotEmpty()) {
                    ResourceButton("Solve", Icons.Default.PlayArrow, Color(0xFF00b8a3)) {
                        onUrlClick(problem.resources.solve)
                    }
                }
                if (problem.resources.youtube.isNotEmpty()) {
                    ResourceButton("Video", Icons.Default.PlayArrow, Color(0xFFFF0000)) {
                        onUrlClick(problem.resources.youtube)
                    }
                }
                if (problem.resources.editorial.isNotEmpty()) {
                    ResourceButton("Editorial", Icons.AutoMirrored.Filled.List, Color.Gray) {
                        onUrlClick(problem.resources.editorial)
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.ResourceButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .weight(1f)
            .height(32.dp)
            .clickable(onClick = onClick),
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}
