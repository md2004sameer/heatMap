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
            .background(BackgroundDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically)
        ) {
            // Minimalist Logo
            Surface(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color.Transparent,
                border = androidx.compose.foundation.BorderStroke(1.dp, LeetCodeOrange.copy(alpha = 0.5f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = LeetCodeOrange
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "HeatMap",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Text(
                    "Track. Visualize. Conquer.",
                    style = MaterialTheme.typography.labelLarge,
                    color = LeetCodeOrange,
                    letterSpacing = 2.sp
                )
            }

            Text(
                "Your LeetCode journey, elegantly visualized and integrated into your daily workflow.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Minimalist Features
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OnboardingFeature(Icons.Default.Refresh, "Sync", Modifier.weight(1f))
                OnboardingFeature(Icons.Default.Build, "Widget", Modifier.weight(1f))
                OnboardingFeature(Icons.Default.Star, "Stats", Modifier.weight(1f))
            }

            Spacer(Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        errorMessage = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("LeetCode Username", color = TextMuted) },
                    isError = errorMessage != null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LeetCodeOrange,
                        unfocusedBorderColor = BorderDark,
                        cursorColor = LeetCodeOrange,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        errorBorderColor = LeetCodeRed.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                errorMessage?.let {
                    Text(
                        it, 
                        color = LeetCodeRed, 
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            Button(
                onClick = {
                    val error = validator(username)
                    if (error == null) onJoin(username) else errorMessage = error
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LeetCodeOrange),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "START JOURNEY", 
                    style = MaterialTheme.typography.titleSmall,
                    color = BackgroundDark,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
fun OnboardingFeature(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon, 
                contentDescription = null, 
                tint = LeetCodeOrange, 
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                label, 
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
