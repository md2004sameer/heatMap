package com.example.heatmap.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.heatmap.MatchedUser
import com.example.heatmap.ui.theme.BorderDark
import com.example.heatmap.ui.theme.LeetCodeOrange
import com.example.heatmap.ui.theme.SurfaceDark
import com.example.heatmap.ui.theme.Typography

@Composable
fun ProfileHeader(user: MatchedUser) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        color = SurfaceDark,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(LeetCodeOrange.copy(alpha = 0.1f))
                    .border(2.dp, LeetCodeOrange, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(user.profile?.userAvatar)
                        .crossfade(true)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_report_image)
                        .build(),
                    contentDescription = "Profile Photo",
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.profile?.realName ?: user.username ?: "Unknown User",
                    style = Typography.titleLarge,
                    color = Color.White
                )
                Text(
                    text = "@${user.username ?: "unknown"}",
                    style = Typography.bodyMedium,
                    color = LeetCodeOrange,
                    fontWeight = FontWeight.Medium
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "RANK",
                    style = Typography.labelSmall,
                    color = Color.Gray,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "#${user.profile?.ranking ?: "N/A"}",
                    style = Typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
