package com.olii.ndrop.ui.scan

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.olii.ndrop.ui.theme.NDropColors
import com.olii.ndrop.ui.theme.NDropTypography

/**
 * NDrop — TreasureWriteOverlay (Treasure Trail)
 *
 * Shown while a "Leave as Treasure" write is queued — the next tag scanned
 * gets this drop's info written to it instead of being read normally
 * (see HomeViewModel.observeNfcScans). Dismissable without losing the drop.
 *
 * Signature: Olii-8882
 */
@Composable
fun TreasureWriteOverlay(
    dropTitle: String,
    onCancel: () -> Unit
) {
    val pulse = rememberInfiniteTransition(label = "write_pulse").animateFloat(
        initialValue = 0.92f,
        targetValue  = 1.05f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "write_pulse_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NDropColors.SpaceNavy.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .scale(pulse.value)
                    .clip(CircleShape)
                    .background(NDropColors.Mint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Nfc, contentDescription = null,
                    tint = NDropColors.Mint, modifier = Modifier.size(44.dp))
            }

            Text(
                "Hold your phone against a blank tag",
                style = NDropTypography.titleMedium,
                color = NDropColors.White,
                textAlign = TextAlign.Center
            )
            Text(
                "Leaving \"$dropTitle\" behind — whoever finds it can save it to their own Discoveries",
                style = NDropTypography.bodyMedium,
                color = NDropColors.WhiteMuted,
                textAlign = TextAlign.Center
            )

            TextButton(
                onClick = onCancel,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
            ) {
                Text("Cancel", color = NDropColors.WhiteDim, style = NDropTypography.labelLarge)
            }
        }
    }
}
