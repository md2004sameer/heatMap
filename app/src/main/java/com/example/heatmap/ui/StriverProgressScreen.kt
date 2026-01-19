package com.example.heatmap.ui

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
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
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

            if (isExpanded) {
                val subGrouped = sectionProblems.groupBy { it.subSection }
                subGrouped.forEach { (subSection, subProblems) ->
                    val isGeneral = subSection.equals("General", ignoreCase = true)
                    val subSectionKey = "${section}_$subSection"
                    val isSubExpanded = isGeneral || subSectionKey in expandedSubSections || searchQuery.isNotEmpty()

                    if (!isGeneral) {
                        item(key = "sub_${section}_$subSection") {
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
                    }

                    if (isSubExpanded) {
                        items(subProblems, key = { it.id }) { problem ->
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

    viewingUrl?.let { url ->
        BrowserDialog(url = url, onDismiss = { viewingUrl = null })
    }
}

@Composable
private fun StriverSummaryHeader(stats: StriverStats) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161b22)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Striver A2Z Sheet",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${stats.completedTotal} / ${stats.totalCount} Problems Solved",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
                Text(
                    text = "${stats.percentage}%",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = LeetCodeOrange
                )
            }
            
            Spacer(Modifier.height(12.dp))
            
            LinearProgressIndicator(
                progress = { stats.percentage / 100f },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color = LeetCodeOrange,
                trackColor = Color(0xFF0d1117)
            )
            
            Spacer(Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DifficultyPill("EASY", stats.easyDone, stats.easyTotal, LeetCodeGreen)
                DifficultyPill("MEDIUM", stats.mediumDone, stats.mediumTotal, LeetCodeYellow)
                DifficultyPill("HARD", stats.hardDone, stats.hardTotal, LeetCodeRed)
            }
        }
    }
}

@Composable
private fun DifficultyPill(label: String, done: Int, total: Int, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(6.dp).background(color, CircleShape))
            Spacer(Modifier.width(6.dp))
            Text(
                text = "$label: $done/$total",
                fontSize = 10.sp,
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
        placeholder = { Text("Search problems...", fontSize = 14.sp) },
        leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(20.dp)) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                }
            }
        },
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LeetCodeOrange,
            unfocusedBorderColor = Color(0xFF30363d),
            focusedContainerColor = Color(0xFF161b22),
            unfocusedContainerColor = Color(0xFF161b22)
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
    Card(
        onClick = onToggle,
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) Color(0xFF21262d) else Color(0xFF161b22)
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$completed/$total",
                    color = if (completed == total) LeetCodeGreen else Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
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
            .padding(start = 8.dp, top = 12.dp, bottom = 8.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = if (isExpanded) LeetCodeOrange.copy(alpha = 0.8f) else Color.Gray,
            letterSpacing = 1.sp,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = Color.Gray,
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

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161b22).copy(alpha = 0.5f)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
        border = if (isCompleted) BorderStroke(1.dp, LeetCodeGreen.copy(alpha = 0.1f)) else null
    ) {
        Row(
            modifier = Modifier.padding(10.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Status Toggle
            IconButton(onClick = onToggle, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (isCompleted) LeetCodeGreen else Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(8.dp))

            // 2. Title & Difficulty
            Column(modifier = Modifier.weight(1f).clickable(onClick = onProblemClick)) {
                Text(
                    text = problem.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isCompleted) Color.Gray else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = problem.difficulty,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = difficultyColor
                )
            }

            // 3. Solve Action
            IconButton(onClick = onSolve, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Solve",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
