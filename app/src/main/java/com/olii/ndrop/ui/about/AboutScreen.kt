package com.olii.ndrop.ui.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olii.ndrop.ui.theme.NDropColors
import com.olii.ndrop.ui.theme.NDropTypography

/**
 * NDrop — AboutScreen
 * Rebuilt to match NAlarm reference layout.
 * Structure: icon → name/tagline → badge → chips → cards → link → footer
 * Signature: Olii-8882
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    isDarkTheme: Boolean = true
) {
    val context = LocalContext.current

    val surface   = if (isDarkTheme) NDropColors.SpaceNavy    else Color(0xFFF4F4F8)
    val card      = if (isDarkTheme) NDropColors.NavyElevated  else Color(0xFFFFFFFF)
    val textPrim  = if (isDarkTheme) NDropColors.White         else Color(0xFF0E0E12)
    val textSec   = if (isDarkTheme) NDropColors.WhiteMuted    else Color(0xFF5A5A72)
    val textDim   = if (isDarkTheme) NDropColors.WhiteDim      else Color(0xFFAAAAAC)
    val divider   = if (isDarkTheme) NDropColors.NavySubtle    else Color(0xFFE4E4EE)
    val chipBg    = if (isDarkTheme) NDropColors.NavySubtle    else Color(0xFFEEEEF6)

    Scaffold(
        containerColor = surface,
        topBar = {
            TopAppBar(
                title = { Text("About", style = NDropTypography.titleMedium, color = textPrim) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = "Back",
                            tint = textSec)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            // ── App icon orb ──────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(NDropColors.Indigo),
                contentAlignment = Alignment.Center
            ) {
                Text("📍", fontSize = 44.sp)
            }

            Spacer(Modifier.height(16.dp))

            // ── App name ──────────────────────────────────────────────────────
            Text(
                "NDrop",
                style = NDropTypography.displayMedium,
                color = textPrim,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(4.dp))

            // ── Tagline ───────────────────────────────────────────────────────
            Text(
                "Drop anywhere. Find everything.",
                style = NDropTypography.bodyMedium,
                color = NDropColors.Indigo,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(12.dp))

            // ── Mali badge ────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(chipBg)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    "🇲🇱  Mali made it  ·  by Olii-8882",
                    style = NDropTypography.labelSmall,
                    color = textSec
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Version + Platform chips ──────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoChip(
                    label = "VERSION",
                    value = "1.0.0",
                    icon  = Icons.Rounded.Info,
                    iconTint = NDropColors.Indigo,
                    bg   = card,
                    textPrim = textPrim,
                    textDim  = textDim,
                    modifier = Modifier.weight(1f)
                )
                InfoChip(
                    label = "PLATFORM",
                    value = "Android",
                    icon  = Icons.Rounded.PhoneAndroid,
                    iconTint = Color(0xFF3DDC84),
                    bg   = card,
                    textPrim = textPrim,
                    textDim  = textDim,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Info cards ────────────────────────────────────────────────────
            AboutInfoCard(
                icon     = Icons.Rounded.Help,
                iconTint = NDropColors.Indigo,
                title    = "What is NDrop?",
                body     = "NDrop is a zero-friction NFC location app. Tap any registered NFC tag " +
                           "and it instantly drops a pin at your current GPS position — no menus, " +
                           "no typing, no app navigation. Built by Olii-8882 as part of oliitech.com, " +
                           "a growing digital brand from Mali.",
                card = card, textPrim = textPrim, textSec = textSec
            )

            Spacer(Modifier.height(12.dp))

            AboutInfoCard(
                icon     = Icons.Rounded.Flag,
                iconTint = NDropColors.Amber,
                title    = "Our Mission",
                body     = "Location saving should be instant. NDrop exists to close the gap between " +
                           "\"I should remember this place\" and actually saving it. One tap is all it takes.",
                card = card, textPrim = textPrim, textSec = textSec
            )

            Spacer(Modifier.height(12.dp))

            AboutInfoCard(
                icon     = Icons.Rounded.Nfc,
                iconTint = NDropColors.Mint,
                title    = "NFC Intelligence",
                body     = "Tags are identified by hardware UID — no data is written unless you use " +
                           "the NFC Write feature. Everything stays on-device. No cloud, no accounts, " +
                           "no tracking. Compatible with NTag213 · NTag215 · NTag216.",
                card = card, textPrim = textPrim, textSec = textSec
            )

            Spacer(Modifier.height(20.dp))

            // ── oliitech.com link row ─────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(card)
                    .clickable {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://oliitech.com"))
                        )
                    }
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(NDropColors.IndigoDim),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Language, contentDescription = null,
                            tint = NDropColors.IndigoLight,
                            modifier = Modifier.size(20.dp))
                    }
                    Text("oliitech.com",
                        style = NDropTypography.titleMedium,
                        color = NDropColors.Indigo)
                }
                Icon(Icons.Rounded.ChevronRight, contentDescription = null,
                    tint = textDim)
            }

            Spacer(Modifier.height(24.dp))

            // ── Footer ────────────────────────────────────────────────────────
            Text(
                "NDrop  ·  v1.0.0  ·  Olii-8882",
                style = NDropTypography.labelSmall,
                color = textDim,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Reusable components ───────────────────────────────────────────────────────

@Composable
private fun InfoChip(
    label: String,
    value: String,
    icon: ImageVector,
    iconTint: Color,
    bg: Color,
    textPrim: Color,
    textDim: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label,
            style = NDropTypography.labelSmall.copy(fontSize = 10.sp),
            color = textDim)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(16.dp))
            Text(value,
                style = NDropTypography.titleMedium,
                color = textPrim,
                fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AboutInfoCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    body: String,
    card: Color,
    textPrim: Color,
    textSec: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(card)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp))
            }
            Text(title,
                style = NDropTypography.titleMedium,
                color = textPrim,
                fontWeight = FontWeight.Bold)
        }
        Text(body,
            style = NDropTypography.bodyMedium,
            color = textSec,
            lineHeight = 22.sp)
    }
}
