package com.olii.ndrop.ui.home

import android.content.Intent
import android.net.Uri
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
 * NDrop — LocationDeniedScreen
 *
 * Shown on HomeScreen when ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION
 * has been denied. Provides a direct deep-link to app settings so the user
 * can grant the permission without manually navigating Settings.
 *
 * Signature: Olii-8882
 */
@Composable
fun LocationDeniedScreen(
    onRequestPermission: () -> Unit
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
                    .background(NDropColors.Rose.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text("📍", fontSize = 44.sp)
            }

            Spacer(Modifier.height(8.dp))

            Text(
                "Location Required",
                style = NDropTypography.titleLarge,
                color = NDropColors.White,
                textAlign = TextAlign.Center
            )

            Text(
                "NDrop needs your location to drop a pin when you scan an NFC tag. Without it, scans can't save anything.",
                style = NDropTypography.bodyLarge,
                color = NDropColors.WhiteMuted,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            // Primary — re-request permission
            Button(
                onClick = onRequestPermission,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NDropColors.Indigo
                )
            ) {
                Text("Grant Permission", style = NDropTypography.labelLarge)
            }

            // Secondary — open app settings (for "Don't ask again" case)
            OutlinedButton(
                onClick = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    )
                    context.startActivity(intent)
                },
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
                Text("Open Settings", style = NDropTypography.labelLarge)
            }
        }
    }
}
