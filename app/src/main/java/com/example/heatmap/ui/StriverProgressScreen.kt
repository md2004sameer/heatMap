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
import com.example.heatmap.StriverProblemEntity
import com.example.heatmap.domain.Problem
import com.example.heatmap.domain.toLeetCodeProblem
import com.example.heatmap.ui.theme.LeetCodeGreen
import com.example.heatmap.ui.theme.LeetCodeOrange
import com.example.heatmap.ui.theme.LeetCodeRed
import com.example.heatmap.ui.theme.LeetCodeYellow

@Composable
fun StriverProgressScreen(
    problems: List<StriverProblemEntity>,
    completedIds: Set<Int>,
    onToggleProblem: (Int) -> Unit,
    onProblemClick: (Problem) -> Unit,
    viewModel: MainViewModel
) {
    var searchQuery by remember { mutableStateOf("") }
    var viewingUrl by remember { mutableStateOf<String?>(null) }
    
    val filteredProblems = remember(problems, searchQuery) {
        if (searchQuery.isBlank()) problems
        else problems.filter { 
            it.title.contains(searchQuery, ignoreCase = true) || 
            it.section.contains(searchQuery, ignoreCase = true) ||
            it.subSection.contains(searchQuery, ignoreCase = true)
        }
    }

    val groupedBySection = remember(filteredProblems) {
        filteredProblems.groupBy { it.section }
    }

    val stats by viewModel.striverStats.collectAsStateWithLifecycle()

    var expandedSections by remember { mutableStateOf(setOf<String>()) }
    var expandedSubSections by remember { mutableStateOf(setOf<String>()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0d1117)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Overall Progress Header
        item {
            StriverSummaryHeader(stats)
        }

        // 2. Search & Filter Bar
        item {
            SearchAndFilterBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it }
            )
        }

        // 3. Section Render
        groupedBySection.forEach { (section, sectionProblems) ->
            val completedInSection = sectionProblems.count { it.id in completedIds }
            val isExpanded = section in expandedSections || searchQuery.isNotEmpty()

            item(key = "section_$section") {
                SectionAccordionHeader(
                    title = section,
                    completed = completedInSection,
                    total = sectionProblems.size,
                    isExpanded = isExpanded,
                    onToggle = {
                        expandedSections = if (isExpanded) expandedSections - section else expandedSections + section
                    }
                )
            }

            item {
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                        val subGrouped = sectionProblems.groupBy { it.subSection }
                        subGrouped.forEach { (subSection, subProblems) ->
                            val isGeneral = subSection.equals("General", ignoreCase = true)
                            val subSectionKey = "${section}_$subSection"
                            val isSubExpanded = isGeneral || subSectionKey in expandedSubSections || searchQuery.isNotEmpty()

                            if (!isGeneral) {
                                SubSectionHeader(
                                    title = subSection,
                                    isExpanded = isSubExpanded,
                                    onToggle = {
                                        expandedSubSections = if (isSubExpanded) {
                                            expandedSubSections - subSectionKey
                                        } else {
                                            expandedSubSections + subSectionKey
                                        }
                                    }
                                )
                            }

                            AnimatedVisibility(
                                visible = isSubExpanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(start = 8.dp)) {
                                    subProblems.forEach { problem ->
                                        TopicRow(
                                            problem = problem,
                                            isCompleted = problem.id in completedIds,
                                            onToggle = { onToggleProblem(problem.id) },
                                            onProblemClick = { 
                                                problem.toLeetCodeProblem()?.let { onProblemClick(it) } ?: run {
                                                    if (problem.solveUrl.isNotEmpty()) {
                                                        viewingUrl = problem.solveUrl
                                                    }
                                                }
                                            },
                                            onSolve = {
                                                if (problem.solveUrl.isNotEmpty()) {
                                                    viewingUrl = problem.solveUrl
                                                }
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
        
        item { Spacer(Modifier.height(24.dp)) }
    }

    viewingUrl?.let { url ->
        BrowserDialog(url = url, onDismiss = { viewingUrl = null })
    }
}

@Composable
private fun StriverSummaryHeader(stats: com.example.heatmap.ui.StriverStats) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161b22)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, Color(0xFF30363d))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(LeetCodeOrange.copy(alpha = 0.08f), Color.Transparent)
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
                            text = "A2Z Progress",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "Striver's SDE Sheet",
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
                            text = "${stats.completedTotal}/${stats.totalCount} SOLVED",
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
                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                    color = LeetCodeOrange,
                    trackColor = Color(0xFF0d1117)
                )
                
                Spacer(Modifier.height(20.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DifficultyChip("Easy", stats.easyDone, stats.easyTotal, LeetCodeGreen)
                    DifficultyChip("Medium", stats.mediumDone, stats.mediumTotal, LeetCodeYellow)
                    DifficultyChip("Hard", stats.hardDone, stats.hardTotal, LeetCodeRed)
                }
            }
        }
    }
}

@Composable
private fun DifficultyChip(label: String, done: Int, total: Int, color: Color) {
    Surface(
        color = color.copy(alpha = 0.05f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(6.dp).background(color, CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(
                text = "$label $done/$total",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun SearchAndFilterBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Filter sheet problems...", color = Color.Gray) },
        leadingIcon = { Icon(Icons.Default.Search, null, tint = LeetCodeOrange, modifier = Modifier.size(20.dp)) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                }
            }
        },
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LeetCodeOrange.copy(alpha = 0.5f),
            unfocusedBorderColor = Color(0xFF30363d),
            focusedContainerColor = Color(0xFF161b22),
            unfocusedContainerColor = Color(0xFF161b22),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        ),
        singleLine = true
    )
}

@Composable
private fun SectionAccordionHeader(
    title: String,
    completed: Int,
    total: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    val rotation by animateFloatAsState(if (isExpanded) 0f else -90f, label = "rotation")
    val isFullySolved = completed == total && total > 0

    Surface(
        onClick = onToggle,
        color = if (isExpanded) Color(0xFF21262d) else Color(0xFF161b22),
        shape = RoundedCornerShape(12.dp),
        border = if (isExpanded) BorderStroke(1.dp, LeetCodeOrange.copy(alpha = 0.2f)) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        if (isFullySolved) LeetCodeGreen.copy(alpha = 0.1f) else Color(0xFF0d1117),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isFullySolved) {
                    Icon(Icons.Default.Check, null, tint = LeetCodeGreen, modifier = Modifier.size(16.dp))
                } else {
                    Text(
                        text = title.take(1).uppercase(),
                        color = if (isExpanded) LeetCodeOrange else Color.Gray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
            
            Spacer(Modifier.width(12.dp))
            
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Surface(
                color = if (isFullySolved) LeetCodeGreen.copy(alpha = 0.1f) else Color(0xFF0d1117),
                shape = CircleShape
            ) {
                Text(
                    text = "$completed/$total",
                    color = if (isFullySolved) LeetCodeGreen else Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            
            Spacer(Modifier.width(8.dp))
            
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = if (isExpanded) LeetCodeOrange else Color.Gray,
                modifier = Modifier.size(20.dp).rotate(rotation)
            )
        }
    }
}

@Composable
private fun SubSectionHeader(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(4.dp, 12.dp)
                .background(if (isExpanded) LeetCodeOrange else Color.Gray, RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = if (isExpanded) Color.White else Color.Gray,
            letterSpacing = 1.sp,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = Color.Gray.copy(alpha = 0.5f),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun TopicRow(
    problem: StriverProblemEntity,
    isCompleted: Boolean,
    onToggle: () -> Unit,
    onProblemClick: () -> Unit,
    onSolve: () -> Unit
) {
    val difficultyColor = when (problem.difficulty) {
        "Easy" -> LeetCodeGreen
        "Medium" -> LeetCodeYellow
        else -> LeetCodeRed
    }

    Surface(
        color = if (isCompleted) Color.Transparent else Color(0xFF161b22).copy(alpha = 0.6f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggle, modifier = Modifier.size(44.dp)) {
                Icon(
                    imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (isCompleted) LeetCodeGreen else Color.Gray.copy(alpha = 0.4f),
                    modifier = Modifier.size(24.dp)
                )
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onProblemClick)
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = problem.title,
                        fontSize = 14.sp,
                        fontWeight = if (isCompleted) FontWeight.Normal else FontWeight.SemiBold,
                        color = if (isCompleted) Color.Gray else Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                        Box(Modifier.size(6.dp).background(difficultyColor, CircleShape))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = problem.difficulty,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = difficultyColor.copy(alpha = 0.8f)
                        )
                    }
                }

                IconButton(onClick = onSolve, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Solve",
                        tint = Color.Gray.copy(alpha = 0.3f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
