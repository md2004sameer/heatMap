package com.example.heatmap.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.heatmap.domain.ValidateUsernameUseCase
import com.example.heatmap.ui.theme.*

@Composable
fun OnboardingScreen(onJoin: (String) -> Unit) {
    var username by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val validator = remember { ValidateUsernameUseCase() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(BackgroundDark, Color(0xFF1a1a2e))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                color = LeetCodeOrange.copy(alpha = 0.1f),
                border = androidx.compose.foundation.BorderStroke(2.dp, LeetCodeOrange)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = LeetCodeOrange
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            Text(
                "LeetCode HeatMap",
                style = Typography.displaySmall,
                color = TextPrimaryDark,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(12.dp))

            Text(
                "Automate your progress tracking and personalize your device with your coding journey.",
                style = Typography.bodyLarge,
                color = TextSecondaryDark,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(Modifier.height(48.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OnboardingFeature(Icons.Default.Refresh, "Auto Sync")
                OnboardingFeature(Icons.Default.Build, "Wallpapers")
                OnboardingFeature(Icons.Default.Star, "Insights")
            }

            Spacer(Modifier.height(56.dp))

            OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it
                    errorMessage = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("LeetCode Username") },
                placeholder = { Text("e.g. sameerog") },
                isError = errorMessage != null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LeetCodeOrange,
                    unfocusedBorderColor = BorderDark,
                    focusedLabelColor = LeetCodeOrange,
                    cursorColor = LeetCodeOrange,
                    focusedTextColor = TextPrimaryDark,
                    unfocusedTextColor = TextPrimaryDark
                ),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            errorMessage?.let {
                Text(
                    it, 
                    color = LeetCodeRed, 
                    style = Typography.labelMedium,
                    modifier = Modifier.align(Alignment.Start).padding(start = 12.dp, top = 6.dp)
                )
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    val error = validator(username)
                    if (error == null) {
                        onJoin(username)
                    } else {
                        errorMessage = error
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LeetCodeOrange),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Text(
                    "Launch Journey", 
                    style = Typography.titleMedium,
                    color = BackgroundDark,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun OnboardingFeature(icon: ImageVector, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(16.dp),
            color = SurfaceDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon, 
                    contentDescription = null, 
                    tint = LeetCodeOrange, 
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            label, 
            style = Typography.labelMedium,
            color = TextSecondaryDark,
            fontWeight = FontWeight.SemiBold
        )
    }
}
