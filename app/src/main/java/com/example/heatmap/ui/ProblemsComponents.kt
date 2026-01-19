package com.example.heatmap.ui

import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
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

@Composable
fun ProblemDetailDialog(problem: Problem, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0d1117),
        title = { Text(problem.title, color = Color.White) },
        text = {
            Column(horizontalAlignment = Alignment.End) {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .background(Color(0xFF0d1117), RoundedCornerShape(8.dp))
                ) {
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
                                        padding: 12px;
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
                    .padding(bottom = 16.dp)
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
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        when {
            selectedSection == null -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(sections) { section ->
                        StriverSelectionCard(title = section) {
                            selectedSection = section
                        }
                    }
                }
            }
            selectedSubSection == null -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(subSections) { subSection ->
                        val displayTitle = if (subSection.isEmpty()) "General" else subSection
                        StriverSelectionCard(title = displayTitle) {
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
                        key = { it.title }
                    ) { problem ->
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
}

@Composable
fun StriverSelectionCard(title: String, onClick: () -> Unit) {
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
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
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
fun StriverProblemItem(problem: StriverProblem, onUrlClick: (String) -> Unit) {
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
