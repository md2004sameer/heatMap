package com.example.heatmap

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.heatmap.domain.GetProfileUseCase
import com.example.heatmap.domain.ValidateUsernameUseCase
import com.example.heatmap.ui.MainScreen
import com.example.heatmap.ui.MainViewModel
import com.example.heatmap.ui.OnboardingScreen
import com.example.heatmap.ui.UiState
import com.example.heatmap.ui.theme.HeatMapTheme

class MainActivity : ComponentActivity() {
    
    private val viewModel: MainViewModel by lazy {
        val repository = LeetCodeRepository.getInstance(applicationContext)
        val getProfileUseCase = GetProfileUseCase(repository)
        val validateUsernameUseCase = ValidateUsernameUseCase()
        
        ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(
                    application,
                    getProfileUseCase,
                    validateUsernameUseCase
                ) as T
            }
        })[MainViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
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
                            viewModel.saveUsernameAndFetch(username)
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

        try {
            // Initialize background tasks
            ReminderWorker.enqueue(this)
            WallpaperWorker.enqueue(this)
            
            // Schedule midnight trigger
            WallpaperTriggerReceiver.scheduleMidnightAlarm(this)
            
            // Ensure background reliability
            requestIgnoreBatteryOptimizations()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error during initialization", e)
        }
    }

    private fun requestIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Could not request battery optimization exemption", e)
                }
            }
        }
    }
}
