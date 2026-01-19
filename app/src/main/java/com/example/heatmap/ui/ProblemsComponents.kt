package com.example.heatmap.ui

import android.content.Intent
import android.net.Uri
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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

                    if (problem.tags.isNotEmpty()) {
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.Label, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                        Text(
                            text = problem.tags.first(),
                            color = Color.Gray,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFF30363d), thickness = 1.dp)

                val styledHtml = remember(problem.content) {
                    val content = problem.content ?: "<div style='text-align:center; padding-top: 50px;'><h3>Content not available offline</h3><p>Please connect to internet to load this problem for the first time.</p></div>"
                    """
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
                }

                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            setBackgroundColor(0)
                            settings.apply {
                                javaScriptEnabled = false
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                defaultFontSize = 14
                                cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                            }
                            webViewClient = WebViewClient()
                        }
                    },
                    update = { webView ->
                        // Only load if content is actually different to avoid flickering/performance hit
                        if (webView.tag != problem.content) {
                            webView.loadDataWithBaseURL("https://leetcode.com", styledHtml, "text/html", "UTF-8", null)
                            webView.tag = problem.content
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    onRelease = { webView ->
                        webView.stopLoading()
                        webView.loadUrl("about:blank")
                        webView.clearHistory()
                        webView.removeAllViews()
                        webView.destroy()
                    }
                )
            }
        }
    }
}
