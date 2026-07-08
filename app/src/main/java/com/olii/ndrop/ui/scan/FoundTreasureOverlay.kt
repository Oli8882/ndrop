package com.olii.ndrop.ui.scan

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.olii.ndrop.nfc.TreasureCodec
import com.olii.ndrop.ui.theme.NDropColors
import com.olii.ndrop.ui.theme.NDropTypography

/**
 * NDrop — FoundTreasureOverlay (Treasure Trail)
 *
 * Shown when a scanned tag turns out to be someone else's seeded Discovery
 * rather than one of the user's own registered tags. Distinct from
 * TagRegistrationSheet (which is for genuinely unknown/blank tags) and from
 * QuickCollectionOverlay (which follows a normal scan of your own tag).
 *
 * Signature: Olii-8882
 */
@Composable
fun FoundTreasureOverlay(
    found: TreasureCodec.FoundTreasure,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(NDropColors.NavySubtle)
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(NDropColors.Mint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(found.emoji, style = NDropTypography.titleLarge)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Treasure Found! ✨", style = NDropTypography.titleLarge, color = NDropColors.Mint)
                Spacer(Modifier.height(4.dp))
                Text(
                    found.title,
                    style = NDropTypography.bodyLarge,
                    color = NDropColors.White
                )
                Text(
                    "Someone left this Discovery here for you to find",
                    style = NDropTypography.bodyMedium,
                    color = NDropColors.WhiteMuted
                )
            }

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NDropColors.Mint)
            ) {
                Text("Save to My Discoveries", color = NDropColors.SpaceNavy,
                    style = NDropTypography.labelLarge)
            }

            TextButton(onClick = onDismiss) {
                Text("Not interested", color = NDropColors.WhiteDim,
                    style = NDropTypography.labelLarge)
            }
        }
    }
}
