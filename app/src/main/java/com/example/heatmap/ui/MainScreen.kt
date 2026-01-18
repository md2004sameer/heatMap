package com.example.heatmap.ui

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import com.example.heatmap.*
import com.example.heatmap.ui.theme.LeetCodeOrange
import com.example.heatmap.ui.theme.Typography
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()

    Scaffold(
        bottomBar = {
            if (uiState is UiState.Success) {
                DualBottomNavigation(
                    currentScreen = currentScreen,
                    onNavigate = { viewModel.navigateTo(it) }
                )
            }
        },
        containerColor = Color(0xFF0d1117)
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (val state = uiState) {
                is UiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = LeetCodeOrange)
                    }
                }
                is UiState.Error -> {
                    Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF442727)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(state.message, color = Color(0xFFFF8A8A))
                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = { viewModel.resetToOnboarding() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Try Another Username")
                                }
                            }
                        }
                    }
                }
                is UiState.Success -> {
                    ContentSwitcher(state.data, currentScreen, viewModel)
                }
                else -> {}
            }
        }
    }
}

@Composable
fun DualBottomNavigation(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF161b22).copy(alpha = 0.95f))
            .navigationBarsPadding()
            .padding(bottom = 8.dp)
    ) {
        // Top Bar: Dynamic Sub-sections
        AnimatedVisibility(visible = currentScreen is Screen.Profile) {
            ScrollableTabRow(
                selectedTabIndex = ProfileSection.all.indexOfFirst { (currentScreen as? Screen.Profile)?.section == it }.coerceAtLeast(0),
                containerColor = Color.Transparent,
                contentColor = LeetCodeOrange,
                edgePadding = 16.dp,
                divider = {},
                indicator = {}
            ) {
                ProfileSection.all.forEach { section ->
                    val isSelected = (currentScreen as? Screen.Profile)?.section == section
                    NavigationItem(
                        label = section.title,
                        icon = section.icon,
                        isSelected = isSelected,
                        onClick = { onNavigate(Screen.Profile(section)) }
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // Bottom Bar: Main Sections Hub
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(64.dp)
                .background(Color(0xFF21262d), RoundedCornerShape(32.dp))
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Hub
            MainHubItem(
                label = "Profile",
                icon = Icons.Default.Person,
                isSelected = currentScreen is Screen.Profile,
                onClick = { onNavigate(Screen.Profile(ProfileSection.Details)) }
            )

            // Problems Hub
            MainHubItem(
                label = "Problems",
                icon = Icons.Default.Search,
                isSelected = currentScreen is Screen.Problems,
                onClick = { onNavigate(Screen.Problems(ProblemsSection.Explore)) }
            )

            // Productivity Hub
            MainHubItem(
                label = "Tools",
                icon = Icons.Default.Build,
                isSelected = currentScreen is Screen.Productivity,
                onClick = { onNavigate(Screen.Productivity(ProductivitySection.Todo)) }
            )
        }
    }
}

@Composable
fun MainHubItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(if (isSelected) LeetCodeOrange.copy(alpha = 0.15f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (isSelected) LeetCodeOrange else Color.Gray,
                modifier = Modifier.size(24.dp)
            )
            if (isSelected) {
                Spacer(Modifier.width(8.dp))
                Text(
                    label,
                    color = LeetCodeOrange,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun NavigationItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (isSelected) LeetCodeOrange else Color.Gray,
            modifier = Modifier.size(20.dp)
        )
        Text(
            label,
            color = if (isSelected) LeetCodeOrange else Color.Gray,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun ContentSwitcher(data: LeetCodeData, currentScreen: Screen, viewModel: MainViewModel) {
    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        label = "ContentTransition"
    ) { screen ->
        Column(modifier = Modifier.fillMaxSize()) {
            when (screen) {
                is Screen.Profile -> ProfileContent(data, screen.section)
                is Screen.Problems -> ProblemsHubContent(viewModel)
                is Screen.Productivity -> ProductivityHubContent(screen.section, viewModel, data)
            }
        }
    }
}

@Composable
fun ProblemsHubContent(viewModel: MainViewModel) {
    val problems by viewModel.problems.collectAsState()
    val selectedProblem by viewModel.selectedProblem.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Explore Problems", style = Typography.headlineMedium, color = Color.White)
        Spacer(Modifier.height(16.dp))
        
        ProblemsScreen(
            problems = problems,
            onProblemClick = { viewModel.selectProblem(it) },
            onSearch = { viewModel.searchProblems(it) }
        )
    }

    if (selectedProblem != null) {
        ProblemDetailDialog(
            problem = selectedProblem!!,
            onDismiss = { viewModel.clearSelectedProblem() }
        )
    }
}

@Composable
fun ProductivityHubContent(section: ProductivitySection, viewModel: MainViewModel, data: LeetCodeData) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Sub-tabs for Productivity
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ProductivitySection.all.forEach { s ->
                val isSelected = section == s
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.navigateTo(Screen.Productivity(s)) },
                    label = { Text(s.title) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LeetCodeOrange.copy(alpha = 0.2f),
                        selectedLabelColor = LeetCodeOrange
                    )
                )
            }
        }

        ProductivityContent(section, viewModel, data)
    }
}

@Composable
fun ProfileContent(data: LeetCodeData, section: ProfileSection) {
    val user = data.matchedUser ?: return
    val submissionCalendar = user.userCalendar?.submissionCalendar
    val submissionByDate = remember(submissionCalendar) {
        try {
            if (submissionCalendar == null) {
                emptyMap()
            } else {
                val type = object : TypeToken<Map<String, Int>>() {}.type
                val rawMap = Gson().fromJson<Map<String, Int>>(submissionCalendar, type)
                rawMap.entries.associate { (tsStr, count) ->
                    val ts = tsStr.toLong()
                    val date = if (tsStr.length > 10) {
                        Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).toLocalDate()
                    } else {
                        Instant.ofEpochSecond(ts).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    date to count
                }
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ProfileHeader(user)
        }

        when (section) {
            ProfileSection.Details -> {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        data.activeDailyCodingChallengeQuestion?.let { challenge ->
                            DailyChallengeCard(challenge)
                        }
                        SubmissionStatsCard(user, submissionByDate)
                        StreakStatusCard(data.streakCounter, submissionByDate[LocalDate.now()] ?: 0)
                        WallpaperModule(data)
                    }
                }
            }
            ProfileSection.Submissions -> {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        ProblemsSolvedCard(user, data.allQuestionsCount ?: emptyList())
                        RecentSubmissionsSection(data.recentSubmissionList ?: emptyList())
                    }
                }
            }
            ProfileSection.Contest -> {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        data.userContestRanking?.let { ContestRankingCard(it) }
                        UpcomingContestsSection(data.upcomingContests ?: emptyList())
                    }
                }
            }
            ProfileSection.Info -> {
                item { ProfileInfoCard(user) }
            }
        }
    }
}

@Composable
fun WallpaperModule(data: LeetCodeData) {
    val showWallpaperOptions = remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showWallpaperOptions.value) {
        WallpaperSelectionDialog(
            onDismiss = { showWallpaperOptions.value = false },
            onApply = { flag ->
                showWallpaperOptions.value = false
                (context as? ComponentActivity)?.lifecycleScope?.launch {
                    WallpaperUtils.applyWallpaper(context, data, flag)
                    WallpaperWorker.enqueue(context)
                }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161b22)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = Icons.Default.Build, contentDescription = null, tint = LeetCodeOrange, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(16.dp))
            Text("Set as Wallpaper", style = Typography.headlineSmall, color = Color.White)
            Spacer(Modifier.height(8.dp))
            Text(
                "Keep your LeetCode heatmap as your wallpaper to stay motivated and track your daily streak.",
                style = Typography.bodyMedium,
                color = Color.Gray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { showWallpaperOptions.value = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = LeetCodeOrange),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Customize & Apply", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ProductivityContent(section: ProductivitySection, viewModel: MainViewModel, data: LeetCodeData) {
    val trainingPlan by viewModel.trainingPlan.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        when (section) {
            ProductivitySection.Todo -> {
                TrainingPlanSection(
                    plan = trainingPlan,
                    onGenerate = { viewModel.generateTrainingPlan(data) },
                    onToggleTask = { taskId -> viewModel.toggleTaskCompletion(taskId) },
                    onAddTask = { title, desc, cat, time -> viewModel.addCustomTask(title, desc, cat, time) },
                    onDeleteTask = { taskId -> viewModel.removeTask(taskId) }
                )
            }
            ProductivitySection.Notes -> {
                NotesModuleSection(viewModel)
            }
        }
    }
}

@Composable
fun WallpaperSelectionDialog(onDismiss: () -> Unit, onApply: (Int) -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF161b22),
        titleContentColor = Color.White,
        textContentColor = Color.White,
        title = { Text("Apply Wallpaper", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Select where to apply the heatmap wallpaper:")
                Spacer(Modifier.height(8.dp))

                val targets = listOf(
                    "Home Screen" to android.app.WallpaperManager.FLAG_SYSTEM,
                    "Lock Screen" to android.app.WallpaperManager.FLAG_LOCK,
                    "Both" to (android.app.WallpaperManager.FLAG_SYSTEM or android.app.WallpaperManager.FLAG_LOCK)
                )

                targets.forEach { (label, flag) ->
                    Button(
                        onClick = {
                            val prefs = context.getSharedPreferences("leetcode_prefs", Context.MODE_PRIVATE)
                            prefs.edit { putInt("wallpaper_target", flag) }
                            onApply(flag)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = LeetCodeOrange),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(label, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = LeetCodeOrange)
            }
        }
    )
}
