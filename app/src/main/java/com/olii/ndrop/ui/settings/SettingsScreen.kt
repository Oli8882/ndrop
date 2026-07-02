package com.olii.ndrop.ui.settings

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olii.ndrop.ui.theme.NDropColors
import com.olii.ndrop.ui.theme.NDropTypography
import com.olii.ndrop.viewmodel.SettingsViewModel

/**
 * NDrop — SettingsScreen
 * Includes Light/Dark theme toggle backed by DataStore.
 * Signature: Olii-8882
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToAbout: () -> Unit,
    isDarkTheme: Boolean,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val tags           by viewModel.registeredTags.collectAsStateWithLifecycle()
    val drops          by viewModel.allDrops.collectAsStateWithLifecycle()
    val parkingSpots   by viewModel.allParkingSpots.collectAsStateWithLifecycle()
    val tagSuggestions by viewModel.tagSuggestions.collectAsStateWithLifecycle()

    // Theme-aware colors
    val surface  = if (isDarkTheme) NDropColors.SpaceNavy   else Color(0xFFF4F4F8)
    val card     = if (isDarkTheme) NDropColors.NavyElevated else Color(0xFFFFFFFF)
    val textPrim = if (isDarkTheme) NDropColors.White        else Color(0xFF0E0E12)
    val textSec  = if (isDarkTheme) NDropColors.WhiteMuted   else Color(0xFF5A5A72)
    val textDim  = if (isDarkTheme) NDropColors.WhiteDim     else Color(0xFFAAAAAC)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(surface)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = "Back", tint = textSec)
            }
            Spacer(Modifier.width(4.dp))
            Text("Settings", style = NDropTypography.displayMedium, color = textPrim)
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            // ── Stats ─────────────────────────────────────────────────────────
            item { SectionHeader("Stats", textDim) }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatChip("📍 ${drops.size}", "Discoveries", card, textPrim, textDim, Modifier.weight(1f))
                    StatChip("🏷️ ${tags.size}", "Tags",         card, textPrim, textDim, Modifier.weight(1f))
                    StatChip("🅿️ ${parkingSpots.size}", "Cars", card, textPrim, textDim, Modifier.weight(1f))
                }
            }

            // ── Smart Suggestions ─────────────────────────────────────────────
            if (tagSuggestions.isNotEmpty()) {
                item { SectionHeader("Smart Insights", textDim) }
                items(tagSuggestions) { (tag, pattern) ->
                    SmartSuggestionCard(tag, pattern, card, textDim)
                }
            }

            // ── Registered Tags ───────────────────────────────────────────────
            item { SectionHeader("My Tags", textDim) }
            if (tags.isEmpty()) {
                item { EmptyTagsHint(card, textSec) }
            } else {
                items(tags, key = { it.uid }) { tag ->
                    RegisteredTagCard(
                        tag      = tag,
                        card     = card,
                        textPrim = textPrim,
                        textSec  = textSec,
                        textDim  = textDim,
                        onDelete = { viewModel.deleteTag(tag) }
                    )
                }
            }

            // ── Multi-Car ─────────────────────────────────────────────────────
            if (parkingSpots.size > 1) {
                item { SectionHeader("Parked Cars", textDim) }
                items(parkingSpots, key = { it.id }) { spot ->
                    MultiCarCard(spot, card, textSec) { viewModel.clearParkingSpot(spot.id) }
                }
            }

            // ── Appearance ────────────────────────────────────────────────────
            item { SectionHeader("Appearance", textDim) }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(card)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(NDropColors.IndigoDim),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (isDarkTheme) Icons.Rounded.DarkMode
                                else Icons.Rounded.LightMode,
                                contentDescription = null,
                                tint = NDropColors.IndigoLight,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                if (isDarkTheme) "Dark Mode" else "Light Mode",
                                style = NDropTypography.titleMedium,
                                color = textPrim
                            )
                            Text(
                                "Tap to switch theme",
                                style = NDropTypography.bodyMedium,
                                color = textDim
                            )
                        }
                    }
                    Switch(
                        checked         = isDarkTheme,
                        onCheckedChange = { viewModel.toggleTheme() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor       = NDropColors.White,
                            checkedTrackColor       = NDropColors.Indigo,
                            uncheckedThumbColor     = NDropColors.Indigo,
                            uncheckedTrackColor     = NDropColors.IndigoDim.copy(alpha = 0.4f)
                        )
                    )
                }
            }

            // ── App ───────────────────────────────────────────────────────────
            item { SectionHeader("App", textDim) }
            item {
                SettingsRow(
                    icon     = Icons.Rounded.Info,
                    iconBg   = NDropColors.IndigoDim,
                    iconTint = NDropColors.IndigoLight,
                    title    = "About NDrop",
                    subtitle = "Version, how-to, and tag guide",
                    card     = card,
                    textPrim = textPrim,
                    textDim  = textDim,
                    onClick  = onNavigateToAbout
                )
            }
            item {
                SettingsRow(
                    icon     = Icons.Rounded.Nfc,
                    iconBg   = NDropColors.MintDim,
                    iconTint = NDropColors.Mint,
                    title    = "Version",
                    subtitle = "1.0.0  ·  Olii-8882",
                    card     = card,
                    textPrim = textPrim,
                    textDim  = textDim,
                    onClick  = {}
                )
            }
        }
    }
}

// ── Components ────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, textDim: Color) {
    Text(
        title.uppercase(),
        style    = NDropTypography.labelSmall,
        color    = textDim,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
    )
}

@Composable
private fun StatChip(
    value: String, label: String,
    card: Color, textPrim: Color, textDim: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(card)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(value, style = NDropTypography.titleLarge, color = textPrim)
        Text(label, style = NDropTypography.labelSmall, color = textDim)
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector, iconBg: Color, iconTint: Color,
    title: String, subtitle: String,
    card: Color, textPrim: Color, textDim: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(card)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint,
                modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title,    style = NDropTypography.titleMedium, color = textPrim)
            Text(subtitle, style = NDropTypography.bodyMedium,  color = textDim)
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = textDim)
    }
}

@Composable
private fun RegisteredTagCard(
    tag: com.olii.ndrop.data.model.RegisteredTag,
    card: Color, textPrim: Color, textSec: Color, textDim: Color,
    onDelete: () -> Unit
) {
    var showDelete by remember { mutableStateOf(false) }
    val (accentColor, icon) = when (tag.tagType) {
        com.olii.ndrop.nfc.TagType.PARKING   -> NDropColors.Mint    to "🅿️"
        com.olii.ndrop.nfc.TagType.DISCOVERY -> NDropColors.Amber   to "✦"
        com.olii.ndrop.nfc.TagType.TIMER     -> NDropColors.IndigoLight to "⏱"
        com.olii.ndrop.nfc.TagType.UNKNOWN   -> NDropColors.WhiteMuted  to "?"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(card)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accentColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) { Text(icon, style = NDropTypography.titleMedium) }

        Column(Modifier.weight(1f)) {
            Text(tag.label, style = NDropTypography.titleMedium, color = textPrim)
            Text(tag.tagType.displayName, style = NDropTypography.bodyMedium, color = accentColor)
            Text("UID: ${tag.uid}  ·  ${tag.scanCount} scans",
                style = NDropTypography.labelSmall, color = textDim)
        }
        IconButton(onClick = { showDelete = true }) {
            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete",
                tint = NDropColors.Rose.copy(alpha = 0.7f))
        }
    }
    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            containerColor   = card,
            title = { Text("Remove Tag?", style = NDropTypography.titleMedium, color = textPrim) },
            text  = { Text("\"${tag.label}\" will be unregistered.",
                style = NDropTypography.bodyMedium, color = textSec) },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDelete = false }) {
                    Text("Remove", color = NDropColors.Rose)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) {
                    Text("Cancel", color = textDim)
                }
            }
        )
    }
}

@Composable
private fun SmartSuggestionCard(
    tag: com.olii.ndrop.data.model.RegisteredTag,
    pattern: com.olii.ndrop.data.model.ScanPattern,
    card: Color, textDim: Color
) {
    val hour    = pattern.avgHourOfDay.toInt()
    val timeStr = when { hour < 12 -> "${hour}am"; hour == 12 -> "noon"; else -> "${hour - 12}pm" }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(card)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("💡", style = NDropTypography.titleMedium)
        Column(Modifier.weight(1f)) {
            Text("${tag.label} pattern",
                style = NDropTypography.titleMedium, color = NDropColors.IndigoLight)
            Text("Usually scanned around $timeStr · ${pattern.scanDates.split(",").size} days tracked",
                style = NDropTypography.bodyMedium, color = textDim)
        }
    }
}

@Composable
private fun MultiCarCard(
    spot: com.olii.ndrop.data.model.ParkingSpot,
    card: Color, textSec: Color, onClear: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(card)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("🅿️", style = NDropTypography.titleMedium)
        Column(Modifier.weight(1f)) {
            Text(spot.carLabel, style = NDropTypography.titleMedium, color = NDropColors.Mint)
            if (spot.floorNote.isNotBlank())
                Text(spot.floorNote, style = NDropTypography.bodyMedium, color = textSec)
        }
        IconButton(onClick = onClear) {
            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Clear",
                tint = NDropColors.Rose.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun EmptyTagsHint(card: Color, textSec: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(card)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("📡", style = NDropTypography.titleLarge)
            Text("Scan any NFC tag to register it",
                style = NDropTypography.bodyMedium, color = textSec)
        }
    }
}
