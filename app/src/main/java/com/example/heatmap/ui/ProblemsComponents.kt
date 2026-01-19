package com.example.heatmap.ui

import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
    val listState = rememberLazyListState()

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
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = problems,
                key = { it.id }
            ) { problem ->
                ProblemItem(problem = problem, onClick = { onProblemClick(problem) })
            }
        }
    }
}

@Composable
fun ProblemItem(problem: Problem, onClick: () -> Unit) {
    val difficultyColor = remember(problem.difficulty) {
        when (problem.difficulty) {
            "Easy" -> LeetCodeGreen
            "Medium" -> LeetCodeYellow
            "Hard" -> LeetCodeRed
            else -> Color.Gray
        }
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
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProblemDetailDialog(problem: Problem, onDismiss: () -> Unit) {
    val context = LocalContext.current
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = problem.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color(0xFF0d1117),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            },
            bottomBar = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF0d1117),
                    tonalElevation = 4.dp,
                    shadowElevation = 8.dp
                ) {
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://leetcode.com/problems/${problem.slug}/"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LeetCodeOrange),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Solve on LeetCode", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            },
            containerColor = Color(0xFF0d1117)
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Header Info
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val difficultyColor = when (problem.difficulty) {
                        "Easy" -> LeetCodeGreen
                        "Medium" -> LeetCodeYellow
                        "Hard" -> LeetCodeRed
                        else -> Color.Gray
                    }
                    
                    Surface(
                        color = difficultyColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = problem.difficulty,
                            color = difficultyColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    
                    Text(
                        text = "Acceptance: ${(problem.acRate).toInt()}%",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }

                HorizontalDivider(color = Color(0xFF30363d), thickness = 1.dp)

                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            setBackgroundColor(0)
                            settings.apply {
                                javaScriptEnabled = false
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                defaultFontSize = 14
                            }
                            webViewClient = WebViewClient()
                        }
                    },
                    update = { webView ->
                        val content = problem.content ?: "Loading content..."
                        val styledHtml = """
                            <!DOCTYPE html>
                            <html>
                            <head>
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                            <style>
                                body {
                                    background-color: #0d1117;
                                    color: #c9d1d9;
                                    font-family: -apple-system, system-ui, sans-serif;
                                    line-height: 1.6;
                                    margin: 0;
                                    padding: 16px;
                                }
                                pre {
                                    background-color: #161b22;
                                    padding: 16px;
                                    border-radius: 8px;
                                    overflow-x: auto;
                                    border: 1px solid #30363d;
                                    font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
                                }
                                code {
                                    background-color: #21262d;
                                    padding: 0.2em 0.4em;
                                    border-radius: 4px;
                                    font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
                                }
                                img {
                                    max-width: 100%;
                                    height: auto;
                                    display: block;
                                    margin: 16px auto;
                                    border-radius: 8px;
                                }
                                p { margin-top: 0; margin-bottom: 16px; }
                                ul, ol { padding-left: 20px; margin-bottom: 16px; }
                                li { margin-bottom: 8px; }
                                strong, b { color: #ffffff; font-weight: 600; }
                                * { box-sizing: border-box; }
                            </style>
                            </head>
                            <body>
                                $content
                            </body>
                            </html>
                        """.trimIndent()
                        webView.loadDataWithBaseURL("https://leetcode.com", styledHtml, "text/html", "UTF-8", null)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun StriverSheetScreen(
    problems: List<StriverProblem>,
    completedIds: Set<Int>,
    onToggleProblem: (Int) -> Unit
) {
    val context = LocalContext.current
    var selectedSection by remember { mutableStateOf<String?>(null) }
    var selectedSubSection by remember { mutableStateOf<String?>(null) }

    val sections = remember(problems) { problems.map { it.section }.distinct() }

    val subSections = remember(problems, selectedSection) {
        if (selectedSection == null) emptyList()
        else problems.filter { it.section == selectedSection }.map { it.subSection }.distinct()
    }

    val filteredProblems = remember(problems, selectedSection, selectedSubSection) {
        if (selectedSection == null || selectedSubSection == null) emptyList()
        else problems.filter { it.section == selectedSection && it.subSection == selectedSubSection }
    }

    BackHandler(enabled = selectedSection != null) {
        if (selectedSubSection != null) {
            selectedSubSection = null
        } else {
            selectedSection = null
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Overall Progress Summary at the top
        StriverSummaryHeader(problems, completedIds)
        
        Spacer(Modifier.height(16.dp))

        if (selectedSection != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (selectedSubSection != null) {
                            selectedSubSection = null
                        } else {
                            selectedSection = null
                        }
                    }
                    .padding(vertical = 8.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = LeetCodeOrange)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (selectedSubSection != null) selectedSection!! else "Select Topic",
                    style = MaterialTheme.typography.titleMedium,
                    color = LeetCodeOrange,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Text(
                text = "Select Section",
                style = MaterialTheme.typography.titleMedium,
                color = LeetCodeOrange,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        when {
            selectedSection == null -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(sections) { section ->
                        val sectionProblems = problems.filter { it.section == section }
                        val completedCount = sectionProblems.count { it.id in completedIds }
                        StriverSelectionCard(
                            title = section,
                            progress = "$completedCount/${sectionProblems.size}"
                        ) {
                            selectedSection = section
                        }
                    }
                }
            }
            selectedSubSection == null -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(subSections) { subSection ->
                        val subSectionProblems = problems.filter { it.section == selectedSection && it.subSection == subSection }
                        val completedCount = subSectionProblems.count { it.id in completedIds }
                        val displayTitle = if (subSection.isEmpty()) "General" else subSection
                        StriverSelectionCard(
                            title = displayTitle,
                            progress = "$completedCount/${subSectionProblems.size}"
                        ) {
                            selectedSubSection = subSection
                        }
                    }
                }
            }
            else -> {
                if (!selectedSubSection.isNullOrEmpty()) {
                    Text(
                        text = selectedSubSection!!,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(
                        items = filteredProblems,
                        key = { it.id }
                    ) { problem ->
                        StriverProblemItem(
                            problem = problem,
                            isCompleted = problem.id in completedIds,
                            onToggleComplete = { onToggleProblem(problem.id) },
                            onUrlClick = { url ->
                                if (url.isNotEmpty()) {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(intent)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StriverSummaryHeader(problems: List<StriverProblem>, completedIds: Set<Int>) {
    val total = problems.size
    val completed = completedIds.size
    val percent = if (total > 0) (completed * 100 / total) else 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1c2128)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "$percent%", fontSize = 32.sp, fontWeight = FontWeight.Black, color = LeetCodeOrange)
            Text(text = "Overall Progress", fontSize = 12.sp, color = Color.Gray)
            Text(text = "$completed/$total Problems", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            
            Spacer(Modifier.height(12.dp))
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                DifficultyStat("Easy", problems.count { it.difficulty == "Easy" }, completedIds, problems)
                DifficultyStat("Medium", problems.count { it.difficulty == "Medium" }, completedIds, problems)
                DifficultyStat("Hard", problems.count { it.difficulty == "Hard" }, completedIds, problems)
            }
        }
    }
}

@Composable
private fun DifficultyStat(label: String, total: Int, completedIds: Set<Int>, allProblems: List<StriverProblem>) {
    val completed = allProblems.filter { it.difficulty == label }.count { it.id in completedIds }
    val color = when(label) {
        "Easy" -> LeetCodeGreen
        "Medium" -> LeetCodeYellow
        else -> LeetCodeRed
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(text = "$completed/$total", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun StriverSelectionCard(title: String, progress: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161b22)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = progress,
                    color = LeetCodeOrange.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun StriverProblemItem(
    problem: StriverProblem,
    isCompleted: Boolean,
    onToggleComplete: () -> Unit,
    onUrlClick: (String) -> Unit
) {
    val difficultyColor = remember(problem.difficulty) {
        when (problem.difficulty) {
            "Easy" -> LeetCodeGreen
            "Medium" -> LeetCodeYellow
            "Hard" -> LeetCodeRed
            else -> Color.Gray
        }
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
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    IconButton(onClick = onToggleComplete, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = "Toggle Complete",
                            tint = if (isCompleted) LeetCodeGreen else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = problem.title,
                        color = if (isCompleted) Color.Gray else Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { if (problem.resources.solve.isNotEmpty()) onUrlClick(problem.resources.solve) }
                    )
                }
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

            // Only show resource row if there are supplemental links
            if (problem.resources.youtube.isNotEmpty() || problem.resources.editorial.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (problem.resources.youtube.isNotEmpty()) {
                        ResourceLink(
                            label = "Video",
                            icon = Icons.Default.PlayCircleFilled,
                            color = Color(0xFFFF0000),
                            onClick = { onUrlClick(problem.resources.youtube) }
                        )
                    }
                    if (problem.resources.editorial.isNotEmpty()) {
                        ResourceLink(
                            label = "Editorial",
                            icon = Icons.AutoMirrored.Filled.List,
                            color = Color.Gray,
                            onClick = { onUrlClick(problem.resources.editorial) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResourceLink(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
