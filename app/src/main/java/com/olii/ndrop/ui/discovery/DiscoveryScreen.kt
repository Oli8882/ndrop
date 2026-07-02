package com.olii.ndrop.ui.discovery

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.platform.LocalContext
import com.olii.ndrop.util.DropCardGenerator
import com.olii.ndrop.util.ExportHelper
import com.olii.ndrop.util.ShareHelper
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olii.ndrop.data.model.Drop
import com.olii.ndrop.ui.theme.NDropColors
import com.olii.ndrop.ui.theme.NDropTypography
import com.olii.ndrop.viewmodel.DiscoveryViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * NDrop — DiscoveryScreen
 * List of all saved Discovery pins with collection filtering.
 * Signature: Olii-8882
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoveryScreen(
    onBack: () -> Unit,
    onOpenInMaps: (lat: Double, lng: Double, title: String) -> Unit,
    isDarkTheme: Boolean = true,
    viewModel: DiscoveryViewModel = hiltViewModel()
) {
    val drops by viewModel.drops.collectAsStateWithLifecycle()
    val collections by viewModel.allCollections.collectAsStateWithLifecycle()
    val selectedCollection by viewModel.selectedCollection.collectAsStateWithLifecycle()

    var editingDrop by remember { mutableStateOf<Drop?>(null) }

    val surface   = if (isDarkTheme) NDropColors.SpaceNavy    else Color(0xFFF4F4F8)
    val card      = if (isDarkTheme) NDropColors.NavyElevated  else Color(0xFFFFFFFF)
    val textPrim  = if (isDarkTheme) NDropColors.White         else Color(0xFF0E0E12)
    val textSec   = if (isDarkTheme) NDropColors.WhiteMuted    else Color(0xFF5A5A72)
    val textDim   = if (isDarkTheme) NDropColors.WhiteDim      else Color(0xFFAAAAAC)
    val divider   = if (isDarkTheme) NDropColors.NavySubtle    else Color(0xFFE4E4EE)
    val chipBg    = if (isDarkTheme) NDropColors.NavySubtle    else Color(0xFFEEEEF6)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(surface)
            .statusBarsPadding()
    ) {
        val context = LocalContext.current

        // ── Header ────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = "Back",
                    tint = textSec)
            }
            Spacer(Modifier.width(4.dp))
            Column(Modifier.weight(1f)) {
                Text("Discoveries", style = NDropTypography.displayMedium, color = textPrim)
                Text("${drops.size} place${if (drops.size != 1) "s" else ""}",
                    style = NDropTypography.bodyMedium, color = textSec)
            }
            // Feature 8: GPX Export
            if (drops.isNotEmpty()) {
                IconButton(onClick = { ExportHelper.exportDropsAsGpx(context, drops) }) {
                    Icon(Icons.Rounded.FileUpload, contentDescription = "Export GPX",
                        tint = textSec)
                }
            }
        }

        // ── Collections filter row ────────────────────────────────────────────
        if (collections.size > 1) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                items(collections) { collection ->
                    CollectionChip(
                        label = collection,
                        selected = collection == selectedCollection,
                        card = card, textDim = textDim,
                        onClick = { viewModel.selectCollection(collection) }
                    )
                }
            }
        }

        // ── Drop list ─────────────────────────────────────────────────────────
        if (drops.isEmpty()) {
            EmptyDiscovery(textSec = textSec, textDim = textDim)
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 100.dp,
                    top = 4.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(drops, key = { it.id }) { drop ->
                    DropCard(
                        drop = drop,
                        card = card, textPrim = textPrim, textSec = textSec, textDim = textDim, divider = divider,
                        onOpenInMaps = { onOpenInMaps(drop.latitude, drop.longitude, drop.title) },
                        onEdit = { editingDrop = drop },
                        onDelete = { viewModel.deleteDrop(drop) }
                    )
                }
            }
        }
    }

    // ── Edit sheet ────────────────────────────────────────────────────────────
    editingDrop?.let { drop ->
        DropEditSheet(
            drop = drop,
            textPrim = textPrim,
            divider = divider,
            onSave = { newTitle, newCollection ->
                viewModel.updateDropDetails(drop, newTitle, newCollection)
                editingDrop = null
            },
            onDismiss = { editingDrop = null }
        )
    }
}

@Composable
private fun DropCard(
    drop: Drop,
    card: Color, textPrim: Color, textSec: Color, textDim: Color, divider: Color,
    onOpenInMaps: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(card)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Emoji badge
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(NDropColors.AmberDim.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Text(drop.emoji, style = NDropTypography.titleLarge)
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                drop.title,
                style = NDropTypography.titleMedium,
                color = textPrim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                formatCoords(drop.latitude, drop.longitude),
                style = NDropTypography.bodyMedium,
                color = textDim
            )
            Text(
                formatTimestamp(drop.timestamp),
                style = NDropTypography.labelSmall,
                color = textSec
            )
        }

        // Action buttons
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MiniIconButton(
                icon = Icons.Rounded.OpenInNew,
                tint = NDropColors.Amber,
                bg = NDropColors.AmberDim,
                contentDescription = "Open in maps",
                onClick = onOpenInMaps
            )
            MiniIconButton(
                icon = Icons.Rounded.Share,
                tint = NDropColors.IndigoLight,
                bg   = NDropColors.IndigoDim,
                contentDescription = "Share",
                onClick = { ShareHelper.shareDrop(context, drop) }
            )
            // Feature 9: Drop Card image
            MiniIconButton(
                icon = Icons.Rounded.Image,
                tint = NDropColors.Amber,
                bg   = NDropColors.AmberDim,
                contentDescription = "Share as card",
                onClick = { DropCardGenerator.shareDropCard(context, drop) }
            )
            MiniIconButton(
                icon = Icons.Rounded.Edit,
                tint = textSec,
                bg = divider,
                contentDescription = "Edit",
                onClick = onEdit
            )
            MiniIconButton(
                icon = Icons.Rounded.DeleteOutline,
                tint = NDropColors.Rose,
                bg = NDropColors.Rose.copy(alpha = 0.1f),
                contentDescription = "Delete",
                onClick = { showDeleteConfirm = true }
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = divider,
            title = { Text("Delete ${drop.title}?", style = NDropTypography.titleMedium, color = textPrim) },
            text = { Text("This place will be removed permanently.", style = NDropTypography.bodyMedium, color = textSec) },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteConfirm = false }) {
                    Text("Delete", color = NDropColors.Rose)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = textSec)
                }
            }
        )
    }
}

@Composable
private fun MiniIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    bg: Color,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun CollectionChip(
    label: String,
    selected: Boolean,
    card: Color, textDim: Color,
    onClick: () -> Unit
) {
    val bg = if (selected) NDropColors.Amber.copy(alpha = 0.15f) else card
    val textColor = if (selected) NDropColors.Amber else textDim
    val borderColor = if (selected) NDropColors.Amber else Color.Transparent

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(label, style = NDropTypography.labelLarge, color = textColor)
    }
}

@Composable
private fun EmptyDiscovery(textSec: Color, textDim: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("✦", style = NDropTypography.displayLarge, color = NDropColors.Amber.copy(alpha = 0.4f))
        Text("No discoveries yet", style = NDropTypography.titleMedium, color = textSec)
        Text(
            "Scan a Discovery tag at an\ninteresting place to save it here",
            style = NDropTypography.bodyMedium,
            color = textDim,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropEditSheet(
    drop: Drop,
    divider: Color, textPrim: Color,
    onSave: (title: String, collection: String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(drop.title) }
    var collection by remember { mutableStateOf(drop.collectionName) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = divider,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Edit Place", style = NDropTypography.titleLarge, color = textPrim)

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Place Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors()
            )

            OutlinedTextField(
                value = collection,
                onValueChange = { collection = it },
                label = { Text("Collection") },
                placeholder = { Text("e.g. Coffee Spots, Tokyo Trip") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors()
            )

            Button(
                onClick = { onSave(title, collection) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NDropColors.Indigo)
            ) {
                Text("Save Changes", style = NDropTypography.labelLarge)
            }
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = NDropColors.Indigo,
    unfocusedBorderColor = NDropColors.WhiteDim,
    focusedLabelColor = NDropColors.Indigo,
    unfocusedLabelColor = NDropColors.WhiteDim,
    cursorColor = NDropColors.Indigo,
    focusedTextColor = NDropColors.White,
    unfocusedTextColor = NDropColors.WhiteMuted,
    focusedPlaceholderColor = NDropColors.WhiteDim,
    unfocusedPlaceholderColor = NDropColors.WhiteDim
)

private fun formatTimestamp(ts: Long): String =
    SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(ts))

private fun formatCoords(lat: Double, lng: Double): String =
    "%.4f, %.4f".format(lat, lng)
