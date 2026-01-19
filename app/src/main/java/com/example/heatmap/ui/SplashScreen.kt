package com.example.heatmap.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.heatmap.R
import com.example.heatmap.ui.theme.LeetCodeOrange

@Composable
fun AppSplashScreen() {
    val infiniteTransition = rememberInfiniteTransition(label = "splash")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0d1117)),
        contentAlignment = Alignment.Center
    ) {
        // Decorative background heatmap grid
        Column(
            modifier = Modifier.fillMaxSize().alpha(0.05f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            repeat(40) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(20) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if ((0..10).random() > 7) LeetCodeOrange else Color.Gray)
                        )
                    }
                }
            }
        }

        // Main Content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .scale(scale)
                    .alpha(alpha)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                LeetCodeOrange.copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_leetcode_logo),
                    contentDescription = "App Logo",
                    modifier = Modifier.size(80.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "HEATMAP",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 4.sp
            )
            
            Text(
                text = "CONSISTENCY IS KEY",
                fontSize = 12.sp,
                color = LeetCodeOrange,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier = Modifier.alpha(0.8f)
            )
        }
        
        // Footer Loading state
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = LeetCodeOrange,
                strokeWidth = 2.dp,
                trackColor = Color.Transparent
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Initializing workspace...",
                fontSize = 12.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
