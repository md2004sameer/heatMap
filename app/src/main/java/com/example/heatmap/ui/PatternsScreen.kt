package com.example.heatmap.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.heatmap.ui.theme.LeetCodeGreen
import com.example.heatmap.ui.theme.LeetCodeOrange
import com.example.heatmap.ui.theme.LeetCodeRed
import com.example.heatmap.ui.theme.LeetCodeYellow

@Composable
fun PatternsScreen(
    viewModel: MainViewModel,
    onProblemClick: (String) -> Unit
) {
    val patterns by viewModel.patterns.collectAsStateWithLifecycle()
    val completedLinks by viewModel.patternCompletedLinks.collectAsStateWithLifecycle()
    val stats by viewModel.patternStats.collectAsStateWithLifecycle()
    
    var expandedPatternId by remember { mutableStateOf<Int?>(null) }
    var isSlidingWindowExpanded by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0d1117)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Overall Progress Tracker
        item {
            PatternProgressHeader(stats)
        }

        item {
            SlidingWindowFolderHeader(
                isExpanded = isSlidingWindowExpanded,
                onToggle = { isSlidingWindowExpanded = !isSlidingWindowExpanded }
            )
        }

        if (isSlidingWindowExpanded) {
            items(patterns, key = { it.id }) { pattern ->
                val solvedCount = pattern.practiceProblems.count { it.link in completedLinks } + 
                                pattern.bonusProblems.count { it.link in completedLinks }
                val totalCount = pattern.practiceProblems.size + pattern.bonusProblems.size
                
                Box(modifier = Modifier.padding(start = 12.dp)) {
                    PatternAccordion(
                        pattern = pattern,
                        isExpanded = expandedPatternId == pattern.id,
                        completedLinks = completedLinks,
                        solvedCount = solvedCount,
                        totalCount = totalCount,
                        onToggle = {
                            expandedPatternId = if (expandedPatternId == pattern.id) null else pattern.id
                        },
                        onProblemClick = onProblemClick,
                        onToggleProblem = { viewModel.togglePatternProblem(it) }
                    )
                }
            }
        }
        
        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
private fun PatternProgressHeader(stats: PatternStats) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161b22)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, Color(0xFF30363d))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Subtle Background Gradient
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(LeetCodeOrange.copy(alpha = 0.05f), Color.Transparent)
                        )
                    )
            )
            
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Pattern Mastery",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "Track your path to algorithm expertise",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${stats.percentage}%",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black,
                            color = LeetCodeOrange
                        )
                        Text(
                            text = "${stats.completedCount}/${stats.totalCount} DONE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = LeetCodeOrange.copy(alpha = 0.7f),
                            letterSpacing = 1.sp
                        )
                    }
                }
                
                Spacer(Modifier.height(20.dp))
                
                LinearProgressIndicator(
                    progress = { stats.percentage / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape),
                    color = LeetCodeOrange,
                    trackColor = Color(0xFF0d1117)
                )
            }
        }
    }
}

@Composable
fun SlidingWindowFolderHeader(
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 0f else -90f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "rotation"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onToggle() },
        color = Color(0xFF21262d),
        border = BorderStroke(1.dp, if (isExpanded) LeetCodeOrange.copy(alpha = 0.3f) else Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(LeetCodeOrange.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                    contentDescription = null,
                    tint = LeetCodeOrange,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Sliding Window",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "The foundation of subarray problems",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }
            
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier
                    .size(24.dp)
                    .rotate(rotation)
            )
        }
    }
}

@Composable
fun PatternAccordion(
    pattern: com.example.heatmap.ui.PatternSheet,
    isExpanded: Boolean,
    completedLinks: Set<String>,
    solvedCount: Int,
    totalCount: Int,
    onToggle: () -> Unit,
    onProblemClick: (String) -> Unit,
    onToggleProblem: (String) -> Unit
) {
    val isFullySolved = solvedCount == totalCount && totalCount > 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) Color(0xFF1c2128) else Color(0xFF161b22)
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (isFullySolved) BorderStroke(1.dp, LeetCodeGreen.copy(alpha = 0.2f)) else null
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = pattern.pattern,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (isFullySolved) {
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                Icons.Default.CheckCircle,
                                null,
                                tint = LeetCodeGreen,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    if (pattern.description.isNotEmpty()) {
                        Text(
                            text = pattern.description,
                            color = Color.Gray,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                Surface(
                    color = if (isFullySolved) LeetCodeGreen.copy(alpha = 0.1f) else Color(0xFF0d1117),
                    shape = CircleShape,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = "$solvedCount/$totalCount",
                        color = if (isFullySolved) LeetCodeGreen else Color.Gray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = if (isExpanded) LeetCodeOrange else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                        .fillMaxWidth()
                ) {
                    Divider(color = Color(0xFF30363d), thickness = 0.5.dp, modifier = Modifier.padding(bottom = 12.dp))
                    
                    if (pattern.practiceProblems.isNotEmpty()) {
                        SectionHeader("Practice Set", LeetCodeOrange)
                        pattern.practiceProblems.forEach { problem ->
                            PatternProblemRow(
                                problem = problem,
                                isCompleted = problem.link in completedLinks,
                                onClick = onProblemClick,
                                onToggle = { onToggleProblem(problem.link) }
                            )
                        }
                    }

                    if (pattern.bonusProblems.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        SectionHeader("Bonus Challenges", LeetCodeYellow)
                        pattern.bonusProblems.forEach { problem ->
                            PatternProblemRow(
                                problem = problem,
                                isCompleted = problem.link in completedLinks,
                                onClick = onProblemClick,
                                onToggle = { onToggleProblem(problem.link) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
        Box(modifier = Modifier.size(4.dp, 12.dp).background(color, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(8.dp))
        Text(
            text = title.uppercase(),
            color = color.copy(alpha = 0.8f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun PatternProblemRow(
    problem: com.example.heatmap.ui.PatternProblem,
    isCompleted: Boolean,
    onClick: (String) -> Unit,
    onToggle: () -> Unit
) {
    val difficultyColor = when (problem.difficulty) {
        "Easy" -> LeetCodeGreen
        "Medium" -> LeetCodeYellow
        "Hard" -> LeetCodeRed
        else -> Color.Gray
    }

    Surface(
        color = if (isCompleted) Color.Transparent else Color(0xFF0d1117).copy(alpha = 0.3f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggle,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (isCompleted) LeetCodeGreen else Color.Gray.copy(alpha = 0.5f),
                    modifier = Modifier.size(22.dp)
                )
            }
            
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onClick(problem.link) }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = problem.title,
                        color = if (isCompleted) Color.Gray else Color.White,
                        fontSize = 14.sp,
                        fontWeight = if (isCompleted) FontWeight.Normal else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(difficultyColor, CircleShape)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = problem.difficulty,
                            color = difficultyColor.copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.Gray.copy(alpha = 0.3f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
