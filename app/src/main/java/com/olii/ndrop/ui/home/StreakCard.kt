package com.olii.ndrop.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.olii.ndrop.data.model.Streak
import com.olii.ndrop.ui.theme.NDropColors
import com.olii.ndrop.ui.theme.NDropTypography

/**
 * NDrop — StreakCard (Feature 5)
 * Shows current daily discovery streak on the home screen.
 * Only visible when streak >= 2.
 * Signature: Olii-8882
 */
@Composable
fun StreakCard(streak: Streak?) {
    AnimatedVisibility(
        visible = (streak?.currentStreak ?: 0) >= 2,
        enter = fadeIn(),
        exit  = fadeOut()
    ) {
        streak ?: return@AnimatedVisibility
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(NDropColors.AmberDim.copy(alpha = 0.6f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("🔥", fontSize = 24.sp)
                Column {
                    Text(
                        "${streak.currentStreak} day streak!",
                        style = NDropTypography.titleMedium,
                        color = NDropColors.Amber
                    )
                    Text(
                        "Best: ${streak.longestStreak} days",
                        style = NDropTypography.labelSmall,
                        color = NDropColors.WhiteDim
                    )
                }
            }
            Text(
                "Explorer 🗺️",
                style = NDropTypography.labelLarge,
                color = NDropColors.Amber
            )
        }
    }
}
