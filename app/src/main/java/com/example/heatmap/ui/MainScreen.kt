package com.example.heatmap.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.heatmap.*
import com.example.heatmap.ui.theme.LeetCodeOrange
import com.example.heatmap.ui.theme.Typography
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    
    val allScreens = Screen.all
    val pagerState = rememberPagerState(pageCount = { allScreens.size })

    // Sync pager state with currentScreen from ViewModel
    LaunchedEffect(currentScreen) {
        val targetPage = allScreens.indexOf(currentScreen)
        if (targetPage != -1 && pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    // Sync currentScreen in ViewModel with pager state
    LaunchedEffect(pagerState.currentPage) {
        val newScreen = allScreens[pagerState.currentPage]
        if (newScreen != currentScreen) {
            viewModel.navigateTo(newScreen)
        }
    }

    val onNavigate = remember(viewModel) { { screen: Screen -> viewModel.navigateTo(screen) } }

    Scaffold(
        bottomBar = {
            if (uiState is UiState.Success) {
                DualBottomNavigation(
                    currentScreen = currentScreen,
                    onNavigate = onNavigate
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
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        beyondViewportPageCount = 1
                    ) { page ->
                        val screen = allScreens[page]
                        Box(modifier = Modifier.fillMaxSize()) {
                            when (screen) {
                                is Screen.Profile -> ProfileContent(state.data, screen.section, viewModel)
                                is Screen.Problems -> ProblemsHubContent(viewModel, screen.section)
                                is Screen.Productivity -> ProductivityHubContent(screen.section, viewModel, state.data)
                            }
                        }
                    }
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
        AnimatedVisibility(visible = currentScreen is Screen.Profile || currentScreen is Screen.Problems || currentScreen is Screen.Productivity) {
            val tabs = remember(currentScreen) {
                when (currentScreen) {
                    is Screen.Profile -> ProfileSection.all.map { it.title to { onNavigate(Screen.Profile(it)) } }
                    is Screen.Problems -> ProblemsSection.all.map { it.title to { onNavigate(Screen.Problems(it)) } }
                    is Screen.Productivity -> ProductivitySection.all.map { it.title to { onNavigate(Screen.Productivity(it)) } }
                }
            }
            
            val selectedIndex by remember(currentScreen) {
                derivedStateOf {
                    when (currentScreen) {
                        is Screen.Profile -> ProfileSection.all.indexOfFirst { currentScreen.section == it }
                        is Screen.Problems -> ProblemsSection.all.indexOfFirst { currentScreen.section == it }
                        is Screen.Productivity -> ProductivitySection.all.indexOfFirst { currentScreen.section == it }
                    }.coerceAtLeast(0)
                }
            }

            ScrollableTabRow(
                selectedTabIndex = selectedIndex,
                containerColor = Color.Transparent,
                contentColor = LeetCodeOrange,
                edgePadding = 16.dp,
                divider = {},
                indicator = {}
            ) {
                val currentIcons = remember(currentScreen) {
                    when (currentScreen) {
                        is Screen.Profile -> ProfileSection.all.map { it.icon }
                        is Screen.Problems -> ProblemsSection.all.map { it.icon }
                        is Screen.Productivity -> ProductivitySection.all.map { it.icon }
                    }
                }

                tabs.forEachIndexed { index, (title, onClick) ->
                    val isSelected = selectedIndex == index
                    NavigationItem(
                        label = title,
                        icon = currentIcons.getOrElse(index) { Icons.Default.Info },
                        isSelected = isSelected,
                        onClick = onClick
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
            MainHubItem(
                label = "Profile",
                icon = Icons.Default.Person,
                isSelected = currentScreen is Screen.Profile,
                onClick = { onNavigate(Screen.Profile(ProfileSection.Details)) }
            )

            MainHubItem(
                label = "Problems",
                icon = Icons.Default.Search,
                isSelected = currentScreen is Screen.Problems,
                onClick = { onNavigate(Screen.Problems(ProblemsSection.Explore)) }
            )

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
fun ProblemsHubContent(viewModel: MainViewModel, section: ProblemsSection) {
    val problems by viewModel.problems.collectAsStateWithLifecycle()
    val striverProblems by viewModel.striverProblems.collectAsStateWithLifecycle()
    val completedStriverIds by viewModel.completedStriverIds.collectAsStateWithLifecycle()
    val selectedProblem by viewModel.selectedProblem.collectAsStateWithLifecycle()
    val viewingUrl = remember { mutableStateOf<String?>(null) }

    val onProblemClick = remember(viewModel) { { problem: com.example.heatmap.domain.Problem -> viewModel.selectProblem(problem) } }
    val onSearch = remember(viewModel) { { query: String -> viewModel.searchProblems(query) } }
    val onToggleStriver = remember(viewModel) { { id: Int -> viewModel.toggleStriverProblem(id) } }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(section.title, style = Typography.headlineMedium, color = Color.White)
        Spacer(Modifier.height(16.dp))
        
        when (section) {
            ProblemsSection.Explore -> {
                ProblemsScreen(
                    problems = problems,
                    onProblemClick = onProblemClick,
                    onSearch = onSearch
                )
            }
            ProblemsSection.Striver -> {
                StriverProgressScreen(
                    problems = striverProblems,
                    completedIds = completedStriverIds,
                    onToggleProblem = onToggleStriver,
                    onProblemClick = onProblemClick,
                    viewModel = viewModel
                )
            }
            ProblemsSection.Patterns -> {
                PatternsScreen(
                    viewModel = viewModel,
                    onProblemClick = onProblemClick
                )
            }
        }
    }

    selectedProblem?.let { problem ->
        ProblemDetailDialog(
            problem = problem,
            onDismiss = { viewModel.clearSelectedProblem() }
        )
    }

    viewingUrl.value?.let { url ->
        BrowserDialog(url = url, onDismiss = { viewingUrl.value = null })
    }
}

@Composable
fun ProductivityHubContent(section: ProductivitySection, viewModel: MainViewModel, data: LeetCodeData) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Sub-tabs for Productivity
        Text(section.title, style = Typography.headlineMedium, color = Color.White)
        Spacer(Modifier.height(16.dp))

        when (section) {
            ProductivitySection.Todo -> {
                MinimalistTodoScreen(viewModel)
            }
            ProductivitySection.Notes -> {
                NotesModuleSection(viewModel)
            }
        }
    }
}

@Composable
fun ProfileContent(data: LeetCodeData, section: ProfileSection, viewModel: MainViewModel) {
    val user = data.matchedUser ?: return
    val gfgPotdList by viewModel.gfgPotdList.collectAsStateWithLifecycle()
    val submissionByDate by viewModel.submissionByDate.collectAsStateWithLifecycle()
    val viewingUrl = remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "header") {
            ProfileHeader(user)
        }

        when (section) {
            ProfileSection.Details -> {
                item(key = "daily_leetcode") {
                    data.activeDailyCodingChallengeQuestion?.let { challenge ->
                        DailyChallengeCard(challenge)
                    }
                }
                
                item(key = "daily_gfg") {
                    val today = remember { LocalDate.now().toString() }
                    val todayGfg = remember(gfgPotdList, today) { gfgPotdList.find { it.date == today } }
                    todayGfg?.let { potd ->

                    }
                }

                item(key = "stats") {
                    SubmissionStatsCard(user, submissionByDate)
                }

                item(key = "streak") {
                    val today = remember { LocalDate.now() }
                    StreakStatusCard(data.streakCounter, submissionByDate[today] ?: 0)
                }
            }
            ProfileSection.Submissions -> {
                item(key = "solved_card") {
                    ProblemsSolvedCard(user, data.allQuestionsCount ?: emptyList())
                }
                item(key = "recent_subs") {
                    RecentSubmissionsSection(data.recentSubmissionList ?: emptyList())
                }
            }
            ProfileSection.Contest -> {
                item(key = "contest_card") {
                    data.userContestRanking?.let { ContestRankingCard(it) }
                }
                item(key = "upcoming_contests") {
                    UpcomingContestsSection(data.upcomingContests ?: emptyList())
                }
            }
            ProfileSection.Info -> {
                item(key = "info_card") {
                    ProfileInfoCard(user)
                }
                item(key = "wallpaper_module") {
                    WallpaperModule(viewModel)
                }
            }
        }
    }

    viewingUrl.value?.let { url ->
        BrowserDialog(url = url, onDismiss = { viewingUrl.value = null })
    }
}

@Composable
fun WallpaperModule(viewModel: MainViewModel) {
    val showWallpaperOptions = remember { mutableStateOf(false) }

    if (showWallpaperOptions.value) {
        WallpaperSelectionDialog(
            onDismiss = { showWallpaperOptions.value = false },
            onApply = { flag: Int ->
                showWallpaperOptions.value = false
                viewModel.applyWallpaperNow(flag)
            }
        )
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161b22)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Daily Wallpaper", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Text("Keep your progress on your home screen.", color = Color.Gray, fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            
            Button(
                onClick = { showWallpaperOptions.value = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = LeetCodeOrange),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Wallpaper, null, modifier = Modifier.size(18.dp), tint = Color.Black)
                Spacer(Modifier.width(8.dp))
                Text("Set Wallpaper Now", color = Color.Black)
            }
        }
    }
}

@Composable
fun WallpaperSelectionDialog(onDismiss: () -> Unit, onApply: (Int) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Where to apply?") },
        text = {
            Column {
                Text("Select target screen:")
                Spacer(Modifier.height(16.dp))
                
                OutlinedButton(
                    onClick = { onApply(1) }, // FLAG_SYSTEM
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Home Screen")
                }
                Spacer(Modifier.height(8.dp))
                
                OutlinedButton(
                    onClick = { onApply(2) }, // FLAG_LOCK
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Lock Screen")
                }
                Spacer(Modifier.height(8.dp))
                
                Button(
                    onClick = { onApply(3) }, // Both
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = LeetCodeOrange),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Both", color = Color.Black)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
