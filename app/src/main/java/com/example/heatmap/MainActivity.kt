package com.example.heatmap

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.CalendarContract
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.example.heatmap.ui.theme.HeatMapTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class MainViewModel(context: Context) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState
    private val repository = LeetCodeRepository.getInstance(context)
    private val prefs = context.getSharedPreferences("leetcode_prefs", Context.MODE_PRIVATE)

    init {
        checkOnboarding()
    }

    private fun checkOnboarding() {
        if (!prefs.contains("last_username")) {
            _uiState.value = UiState.Onboarding
        }
    }

    private fun isValidUsername(username: String): Boolean {
        val regex = "^[a-zA-Z][a-zA-Z0-9_-]{2,29}$".toRegex()
        return username.matches(regex)
    }

    fun saveUsernameAndFetch(username: String, context: Context) {
        if (!isValidUsername(username)) {
            _uiState.value = UiState.Error("Invalid username format")
            return
        }
        prefs.edit().putString("last_username", username).apply()
        fetchProfile(username, context)
    }

    fun fetchProfile(username: String, context: Context) {
        if (!isValidUsername(username)) {
            _uiState.value = UiState.Error("Invalid username format")
            return
        }

        prefs.edit().putString("last_username", username).apply()

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            repository.getProfile(username).collectLatest { data ->
                if (data != null && data.matchedUser != null) {
                    _uiState.value = UiState.Success(data)
                } else if (_uiState.value is UiState.Loading) {
                    _uiState.value = UiState.Error("User not found. Please check the username.")
                }
            }
        }
    }

    fun resetToOnboarding() {
        _uiState.value = UiState.Onboarding
    }
}

sealed class UiState {
    object Idle : UiState()
    object Onboarding : UiState()
    object Loading : UiState()
    data class Success(val data: LeetCodeData) : UiState()
    data class Error(val message: String) : UiState()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val viewModel = MainViewModel(applicationContext)
        setContent {
            HeatMapTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF1a1a2e)
                ) {
                    val uiState by viewModel.uiState.collectAsState()

                    AnimatedVisibility(
                        visible = uiState is UiState.Onboarding,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        OnboardingScreen(onJoin = { username ->
                            viewModel.saveUsernameAndFetch(username, this@MainActivity)
                        })
                    }

                    AnimatedVisibility(
                        visible = uiState !is UiState.Onboarding,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        MainScreen(viewModel)
                    }
                }
            }
        }

        // Initialize background tasks
        ReminderWorker.enqueue(this)
        WallpaperWorker.enqueue(this)
        
        // Schedule midnight trigger
        WallpaperTriggerReceiver.scheduleMidnightAlarm(this)
        
        // Ensure background reliability
        requestIgnoreBatteryOptimizations()
    }

    private fun requestIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Could not request battery optimization exemption", e)
                }
            }
        }
    }
}

@Composable
fun OnboardingScreen(onJoin: (String) -> Unit) {
    var username by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun validate(name: String): String? {
        if (name.isBlank()) return "Username cannot be empty"
        if (name.length < 3) return "Too short (min 3 chars)"
        if (!name.first().isLetter()) return "Must start with a letter"
        val regex = "^[a-zA-Z][a-zA-Z0-9_-]*$".toRegex()
        if (!name.matches(regex)) return "Only letters, numbers, _ and - allowed"
        return null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.DateRange,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color(0xFFffa116)
        )

        Spacer(Modifier.height(24.dp))

        Text(
            "Welcome to HeatMap",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Track your LeetCode progress and set beautiful heatmap wallpapers automatically.",
            fontSize = 16.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(40.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            OnboardingFeature(Icons.Default.Refresh, "Auto Sync")
            OnboardingFeature(Icons.Default.Build, "Wallpapers")
            OnboardingFeature(Icons.Default.Star, "Streaks")
        }

        Spacer(Modifier.height(48.dp))

        TextField(
            value = username,
            onValueChange = {
                username = it
                errorMessage = null
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("LeetCode Username", color = Color.Gray) },
            isError = errorMessage != null,
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = Color(0xFF2d333b),
                focusedContainerColor = Color(0xFF2d333b),
                unfocusedTextColor = Color.White,
                focusedTextColor = Color.White,
                cursorColor = Color(0xFFffa116),
                focusedIndicatorColor = Color(0xFFffa116),
                errorIndicatorColor = Color.Red
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        errorMessage?.let {
            Text(it, color = Color.Red, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start).padding(top = 4.dp))
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                val error = validate(username)
                if (error == null) {
                    onJoin(username)
                } else {
                    errorMessage = error
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFffa116)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Get Started", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun OnboardingFeature(icon: ImageVector, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = Color(0xFF2d333b)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = Color(0xFFffa116), modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val initialUsername = remember {
        val prefs = context.getSharedPreferences("leetcode_prefs", Context.MODE_PRIVATE)
        prefs.getString("last_username", "") ?: ""
    }

    var username by remember { mutableStateOf(initialUsername) }

    LaunchedEffect(Unit) {
        if (username.isNotEmpty() && uiState is UiState.Idle) {
            viewModel.fetchProfile(username, context)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "LeetCode Profile",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFffa116)
                )

                IconButton(onClick = { viewModel.resetToOnboarding() }) {
                    Icon(Icons.Default.Edit, contentDescription = "Change User", tint = Color.Gray)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = username,
                    onValueChange = { username = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Enter username", color = Color.Gray) },
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color(0xFF2d333b),
                        focusedContainerColor = Color(0xFF2d333b),
                        unfocusedTextColor = Color.White,
                        focusedTextColor = Color.White,
                        cursorColor = Color(0xFFffa116),
                        focusedIndicatorColor = Color(0xFFffa116)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = { viewModel.fetchProfile(username, context) },
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color(0xFFffa116), RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
                }
            }

            Spacer(Modifier.height(16.dp))

            when (val state = uiState) {
                is UiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFFffa116))
                    }
                }
                is UiState.Error -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF442727)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(state.message, color = Color(0xFFFF8A8A), textAlign = TextAlign.Center)
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
                is UiState.Success -> {
                    ProfileDetails(state.data)
                }
                else -> {}
            }
        }

        AppFooter()
    }
}

@Composable
fun AppFooter() {
    val context = LocalContext.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF161b22),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Made with ",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = Color.Red,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = " by ",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp
            )

            Row(
                modifier = Modifier.clickable {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.linkedin.com/in/sameerog/")
                    )
                    context.startActivity(intent)
                },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_linkedin_logo),
                    contentDescription = "LinkedIn",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "sameerog",
                    color = Color(0xFF0b86ca),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}


@Composable
fun ProfileDetails(data: LeetCodeData) {
    val user = data.matchedUser ?: return
    val context = LocalContext.current
    var showWallpaperOptions by remember { mutableStateOf(false) }

    val submissionByDate = remember(user.userCalendar.submissionCalendar) {
        try {
            val type = object : TypeToken<Map<String, Int>>() {}.type
            val rawMap = Gson().fromJson<Map<String, Int>>(user.userCalendar.submissionCalendar, type)
            rawMap.entries.associate { (tsStr, count) ->
                val ts = tsStr.toLong()
                val date = if (tsStr.length > 10) {
                    Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).toLocalDate()
                } else {
                    Instant.ofEpochSecond(ts).atZone(ZoneId.systemDefault()).toLocalDate()
                }
                date to count
            }
        } catch (e: Exception) {
            emptyMap<LocalDate, Int>()
        }
    }

    if (showWallpaperOptions) {
        AlertDialog(
            onDismissRequest = { showWallpaperOptions = false },
            containerColor = Color(0xFF161b22),
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = { Text("Apply Wallpaper", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select where to apply the heatmap wallpaper:")
                    Spacer(Modifier.height(8.dp))

                    val targets = listOf(
                        "Home Screen" to (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) WallpaperManager.FLAG_SYSTEM else 0),
                        "Lock Screen" to (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) WallpaperManager.FLAG_LOCK else 0),
                        "Both" to (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) (WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK) else 0)
                    )

                    targets.forEach { (label, flag) ->
                        Button(
                            onClick = {
                                showWallpaperOptions = false
                                val prefs = context.getSharedPreferences("leetcode_prefs", Context.MODE_PRIVATE)
                                prefs.edit().putInt("wallpaper_target", flag).apply()
                                
                                (context as? ComponentActivity)?.lifecycleScope?.launch {
                                    WallpaperUtils.applyWallpaper(context, data, flag)
                                    WallpaperWorker.enqueue(context)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFffa116)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(label, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showWallpaperOptions = false }) {
                    Text("Cancel", color = Color(0xFFffa116))
                }
            }
        )
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            data.activeDailyCodingChallengeQuestion?.let { challenge ->
                DailyChallengeCard(challenge)
            }
        }

        item {
            UpcomingContestsSection(data.upcomingContests ?: emptyList())
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161b22)),
                border = BorderStroke(1.dp, Color(0xFF30363d)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Profile Information", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.height(12.dp))
                    InfoRow("Username", user.username)
                    InfoRow("Real Name", user.profile.realName ?: "Not provided")
                    InfoRow("Country", user.profile.countryName ?: "Not specified")
                    InfoRow("Global Rank", "#${user.profile.ranking}")
                }
            }
        }

        item {
            StreakStatusCard(data.streakCounter, submissionByDate[LocalDate.now()] ?: 0)
        }

        item {
            Button(
                onClick = { showWallpaperOptions = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFffa116)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Set as Wallpaper", fontWeight = FontWeight.Bold)
            }
        }

        data.userContestRanking?.let { contest ->
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161b22)),
                    border = BorderStroke(1.dp, Color(0xFF30363d)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Contest Ranking", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(Modifier.height(12.dp))
                        InfoRow("Rating", contest.rating.toInt().toString())
                        InfoRow("Global Ranking", "#${contest.globalRanking}")
                        InfoRow("Percentile", "Top ${contest.topPercentage}%")
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161b22)),
                border = BorderStroke(1.dp, Color(0xFF30363d)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Problems Solved", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.height(12.dp))
                    val acStats = user.submitStats.acSubmissionNum
                    val allQuestions = data.allQuestionsCount ?: emptyList()

                    acStats.forEachIndexed { index, stat ->
                        if (index < allQuestions.size) {
                            ProblemStatItem(stat.difficulty, stat.count, allQuestions[index].count, stat.submissions)
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161b22)),
                border = BorderStroke(1.dp, Color(0xFF30363d)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    val totalSubYear = submissionByDate.values.sum()

                    Text(
                        text = "$totalSubYear",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "submissions in the past one year",
                        fontSize = 14.sp,
                        color = Color(0xFF8b949e)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Total active days: ${user.userCalendar.totalActiveDays}", color = Color.White, fontSize = 14.sp)

                    Text("Max streak (Year): ${user.userCalendar.streak}", color = Color.White, fontSize = 14.sp)

                    val currentMonth = LocalDate.now().month
                    val subThisMonth = submissionByDate.filter { it.key.month == currentMonth && it.key.year == LocalDate.now().year }.values.sum()
                    Text("Submissions this month: $subThisMonth", color = Color.White, fontSize = 14.sp)

                    Spacer(Modifier.height(16.dp))

                    SubmissionHeatMap(user.userCalendar.submissionCalendar)
                }
            }
        }

        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
fun UpcomingContestsSection(contests: List<Contest>) {
    if (contests.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Upcoming Contests",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        contests.forEach { contest ->
            ContestCard(contest)
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
fun ContestCard(contest: Contest) {
    val context = LocalContext.current
    var timeLeft by remember { mutableLongStateOf(contest.startTime * 1000 - System.currentTimeMillis()) }

    LaunchedEffect(key1 = contest.startTime) {
        while (timeLeft > 0) {
            delay(1000)
            timeLeft = contest.startTime * 1000 - System.currentTimeMillis()
        }
    }

    val formattedTimeLeft = remember(timeLeft) {
        if (timeLeft <= 0) "Started"
        else {
            val hours = TimeUnit.MILLISECONDS.toHours(timeLeft)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(timeLeft) % 60
            val seconds = TimeUnit.MILLISECONDS.toSeconds(timeLeft) % 60
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        }
    }

    val startDateTime = remember(contest.startTime) {
        LocalDateTime.ofInstant(Instant.ofEpochSecond(contest.startTime), ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("MMM dd, hh:mm a"))
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161b22)),
        border = BorderStroke(1.dp, Color(0xFF30363d)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        contest.title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Starts: $startDateTime",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                    Text(
                        "Duration: ${contest.duration / 60} mins",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }

                Surface(
                    color = if (timeLeft > 0) Color(0xFFffa116).copy(alpha = 0.1f) else Color.Red.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        formattedTimeLeft,
                        color = if (timeLeft > 0) Color(0xFFffa116) else Color.Red,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://leetcode.com/contest/${contest.titleSlug}"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFffa116)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Register", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(
                    onClick = { scheduleContestReminder(context, contest) },
                    modifier = Modifier
                        .background(Color(0xFF2d333b), RoundedCornerShape(8.dp))
                        .size(40.dp)
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = "Reminder", tint = Color(0xFFffa116))
                }

                IconButton(
                    onClick = { addContestToCalendar(context, contest) },
                    modifier = Modifier
                        .background(Color(0xFF2d333b), RoundedCornerShape(8.dp))
                        .size(40.dp)
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = "Calendar", tint = Color(0xFFffa116))
                }
            }
        }
    }
}

fun addContestToCalendar(context: Context, contest: Contest) {
    val intent = Intent(Intent.ACTION_INSERT)
        .setData(CalendarContract.Events.CONTENT_URI)
        .putExtra(CalendarContract.Events.TITLE, contest.title)
        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, contest.startTime * 1000)
        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, (contest.startTime + contest.duration) * 1000)
        .putExtra(CalendarContract.Events.DESCRIPTION, "LeetCode Contest: https://leetcode.com/contest/${contest.titleSlug}")
        .putExtra(CalendarContract.Events.EVENT_LOCATION, "LeetCode")
    context.startActivity(intent)
}

fun scheduleContestReminder(context: Context, contest: Contest) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (!alarmManager.canScheduleExactAlarms()) {
            context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            return
        }
    }

    val startTimeMillis = contest.startTime * 1000
    val preTimeMillis = startTimeMillis - TimeUnit.HOURS.toMillis(1)
    val endTimeMillis = startTimeMillis + (contest.duration * 1000L)

    fun createPendingIntent(type: String): PendingIntent {
        val intent = Intent(context, ContestReminderReceiver::class.java).apply {
            putExtra("contest_title", contest.title)
            putExtra("contest_slug", contest.titleSlug)
            putExtra("type", type)
        }
        val requestCode = contest.titleSlug.hashCode() + type.hashCode()
        return PendingIntent.getBroadcast(
            context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    if (preTimeMillis > System.currentTimeMillis()) {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, preTimeMillis, createPendingIntent("pre"))
    }

    if (startTimeMillis > System.currentTimeMillis()) {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, startTimeMillis, createPendingIntent("start"))
    }

    if (endTimeMillis > System.currentTimeMillis()) {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endTimeMillis, createPendingIntent("summary"))
    }

    Toast.makeText(context, "Reminders set for ${contest.title}", Toast.LENGTH_SHORT).show()
}

@Composable
fun DailyChallengeCard(challenge: DailyChallenge) {
    val context = LocalContext.current
    val difficultyColor = when (challenge.question.difficulty) {
        "Easy" -> Color(0xFF00b8a3)
        "Medium" -> Color(0xFFffc01e)
        "Hard" -> Color(0xFFff375f)
        else -> Color.Gray
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF282828).copy(alpha = 0.6f)),
        border = BorderStroke(1.dp, Color(0xFFffa116).copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://leetcode.com${challenge.link}"))
                context.startActivity(intent)
            },
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFffa116),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Daily Challenge",
                        color = Color(0xFFffa116),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    challenge.question.title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    challenge.question.difficulty,
                    color = difficultyColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            IconButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://leetcode.com${challenge.link}"))
                    context.startActivity(intent)
                },
                modifier = Modifier.background(Color(0xFFffa116).copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Solve",
                    tint = Color(0xFFffa116)
                )
            }
        }
    }
}

@Composable
fun StreakStatusCard(streakCounter: StreakCounter?, submissionsToday: Int) {
    if (streakCounter == null) return
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161b22)),
        border = BorderStroke(1.dp, Color(0xFF30363d)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Daily Activity & Streak", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "${streakCounter.streakCount}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFffa116)
                    )
                    Text(text = "Current Streak", color = Color.Gray, fontSize = 12.sp)
                }

                Column(horizontalAlignment = Alignment.End) {
                    val statusColor = if (streakCounter.currentDayCompleted) Color(0xFF00b8a3) else Color(0xFFff375f)
                    val statusText = if (streakCounter.currentDayCompleted) "Today Completed" else "Today Pending"
                    val icon = if (streakCounter.currentDayCompleted) Icons.Default.CheckCircle else Icons.Default.Info

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = statusText,
                            color = statusColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "$submissionsToday submissions today",
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    if (streakCounter.daysSkipped > 0) {
                        Text(
                            text = "${streakCounter.daysSkipped} days skipped",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color(0xFF8b949e), fontSize = 14.sp)
        Text(value, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
fun ProblemStatItem(difficulty: String, solved: Int, total: Int, submissions: Int) {
    val color = when(difficulty) {
        "Easy" -> Color(0xFF00b8a3)
        "Medium" -> Color(0xFFffc01e)
        "Hard" -> Color(0xFFff375f)
        else -> Color(0xFFffa116)
    }

    Column(Modifier.padding(vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(difficulty, color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("$solved / $total", color = Color.White, fontSize = 13.sp)
        }
        val progress = if (total > 0) solved.toFloat() / total.toFloat() else 0f
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(6.dp).padding(top = 6.dp).clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = Color(0xFF2d333b),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        Text(
            text = "Total Submissions: $submissions",
            color = Color.Gray,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun SubmissionHeatMap(calendarJson: String) {
    var selectedInfo by remember { mutableStateOf<String?>(null) }

    val submissionByDate = remember(calendarJson) {
        try {
            val type = object : TypeToken<Map<String, Int>>() {}.type
            val rawMap = Gson().fromJson<Map<String, Int>>(calendarJson, type)
            rawMap.entries.associate { (tsStr, count) ->
                val ts = tsStr.toLong()
                val date = if (tsStr.length > 10) {
                    Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).toLocalDate()
                } else {
                    Instant.ofEpochSecond(ts).atZone(ZoneId.systemDefault()).toLocalDate()
                }
                date to count
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    val today = LocalDate.now()
    val startDate = remember {
        var start = LocalDate.of(today.year, 1, 1)
        while (start.dayOfWeek != java.time.DayOfWeek.SUNDAY) {
            start = start.minusDays(1)
        }
        start
    }

    val weeksCount = remember(startDate) {
        val endOfYear = LocalDate.of(today.year, 12, 31)
        var count = 0
        var current = startDate
        while (current.isBefore(endOfYear) || current.isEqual(endOfYear)) {
            current = current.plusWeeks(1)
            count++
        }
        count.coerceAtLeast(53)
    }

    val weeks = remember(startDate, weeksCount) {
        (0 until weeksCount).map { w ->
            val weekStart = startDate.plusWeeks(w.toLong())
            (0..6).map { d -> weekStart.plusDays(d.toLong()) }
        }
    }

    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMM") }

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0d1117), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFF30363d), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val groupedWeeks = weeks.groupBy { it.find { d -> d.year == today.year }?.month }

                    groupedWeeks.forEach { (month, monthWeeks) ->
                        Column {
                            Box(modifier = Modifier.height(22.dp)) {
                                val firstValidDate = monthWeeks.first().find { it.year == today.year }
                                if (firstValidDate != null) {
                                    Text(
                                        text = firstValidDate.format(monthFormatter),
                                        color = Color(0xFF8b949e),
                                        fontSize = 10.sp,
                                        modifier = Modifier.width(40.dp)
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                monthWeeks.forEach { weekDays ->
                                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        weekDays.forEach { date ->
                                            if (date.year != today.year || date.month != month) {
                                                Box(Modifier.size(11.dp))
                                            } else if (date.isAfter(today)) {
                                                Box(
                                                    Modifier
                                                        .size(11.dp)
                                                        .background(Color(0xFF161b22), RoundedCornerShape(2.dp))
                                                )
                                            } else {
                                                val count = submissionByDate[date] ?: 0
                                                val color = getHeatmapColor(count)

                                                Box(
                                                    Modifier
                                                        .size(11.dp)
                                                        .background(color, RoundedCornerShape(2.dp))
                                                        .clickable {
                                                            selectedInfo = "${if (count == 0) "No" else count} submissions on ${date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}"
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
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedInfo ?: "Tap a square for details",
                color = Color(0xFF8b949e),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )

            HeatMapLegend()
        }
    }
}

fun getHeatmapColor(count: Int): Color {
    return when {
        count == 0 -> Color(0xFF161B22)
        count <= 2 -> Color(0xFF0E4429)
        count <= 5 -> Color(0xFF006D32)
        count <= 10 -> Color(0xFF26A641)
        else -> Color(0xFF39D353)
    }
}

@Composable
fun HeatMapLegend() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Less", color = Color(0xFF8B949E), fontSize = 10.sp)
        listOf(
            Color(0xFF161B22),
            Color(0xFF0E4429),
            Color(0xFF006D32),
            Color(0xFF26A641),
            Color(0xFF39D353)
        ).forEach { color ->
            Box(Modifier.size(10.dp).background(color, RoundedCornerShape(2.dp)).border(0.5.dp, Color(0xFF30363D), RoundedCornerShape(2.dp)))
        }
        Text("More", color = Color(0xFF8B949E), fontSize = 10.sp)
    }
}
