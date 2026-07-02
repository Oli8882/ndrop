package com.olii.ndrop.ui.home

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olii.ndrop.ui.theme.NDropColors
import com.olii.ndrop.ui.theme.NDropTypography

/**
 * NDrop — NfcUnavailableScreen
 *
 * Two states:
 *  - NO_HARDWARE: device has no NFC chip — nothing the user can do, explain and offer
 *    limited mode (Discovery list still accessible, just no scanning).
 *  - DISABLED: NFC chip exists but is turned off — deep-link to NFC settings.
 *
 * Shown from MainActivity before rendering the main NavGraph.
 *
 * Signature: Olii-8882
 */

enum class NfcStatus { AVAILABLE, DISABLED, NO_HARDWARE }

@Composable
fun NfcUnavailableScreen(
    status: NfcStatus,
    onContinueAnyway: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NDropColors.SpaceNavy),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(horizontal = 40.dp)
        ) {
            // Icon orb
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(NDropColors.Amber.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(if (status == NfcStatus.NO_HARDWARE) "📡" else "⚡", fontSize = 44.sp)
            }

            Spacer(Modifier.height(8.dp))

            Text(
                if (status == NfcStatus.NO_HARDWARE) "No NFC Hardware" else "NFC is Off",
                style = NDropTypography.titleLarge,
                color = NDropColors.White,
                textAlign = TextAlign.Center
            )

            Text(
                if (status == NfcStatus.NO_HARDWARE)
                    "This device doesn't have an NFC chip. You can still browse your saved Discoveries and Parking spots."
                else
                    "NDrop needs NFC to scan your tags. Turn it on in Settings — it takes two taps.",
                style = NDropTypography.bodyLarge,
                color = NDropColors.WhiteMuted,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            if (status == NfcStatus.DISABLED) {
                // Deep-link to NFC settings
                Button(
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NDropColors.Amber
                    )
                ) {
                    Text(
                        "Turn On NFC",
                        style = NDropTypography.labelLarge,
                        color = NDropColors.SpaceNavy
                    )
                }
            }

            // Always offer limited mode access
            OutlinedButton(
                onClick = onContinueAnyway,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = NDropColors.WhiteMuted
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, NDropColors.WhiteDim
                )
            ) {
                Text(
                    if (status == NfcStatus.NO_HARDWARE) "Browse Saved Places" else "Continue Anyway",
                    style = NDropTypography.labelLarge
                )
            }
        }
    }
}
