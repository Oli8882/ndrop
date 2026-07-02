package com.olii.ndrop.ui.scan

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.olii.ndrop.ui.theme.NDropColors
import com.olii.ndrop.ui.theme.NDropTypography
import kotlinx.coroutines.delay

/**
 * NDrop — QuickCollectionOverlay (Feature 3)
 *
 * Slides up after a Discovery scan. Shows existing collections
 * as tappable chips. Selecting one updates the drop immediately.
 * Auto-dismisses after 6s if ignored.
 *
 * Signature: Olii-8882
 */
@Composable
fun QuickCollectionOverlay(
    dropTitle: String,
    existingCollections: List<String>,
    onAssign: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    var newCollection by remember { mutableStateOf("") }
    var showNewField by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(2_400L)  // After ScanOverlay finishes (2.2s)
        visible = true
        delay(6_000L)
        if (!showNewField) onDismiss()
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit  = slideOutVertically(targetOffsetY = { it }) + fadeOut()
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
                    .padding(top = 20.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Add to collection? ✦", style = NDropTypography.titleMedium)
                        Text(
                            dropTitle,
                            style = NDropTypography.bodyMedium,
                            color = NDropColors.Amber
                        )
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Skip", color = NDropColors.WhiteDim,
                            style = NDropTypography.labelLarge)
                    }
                }

                // Existing collection chips
                val displayCollections = existingCollections
                    .filter { it != "Uncategorized" && it != "All" }

                if (displayCollections.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(displayCollections) { col ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(NDropColors.AmberDim.copy(alpha = 0.5f))
                                    .border(1.dp, NDropColors.Amber.copy(alpha = 0.5f),
                                        RoundedCornerShape(20.dp))
                                    .clickable { onAssign(col) }
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Text(col, style = NDropTypography.labelLarge,
                                    color = NDropColors.Amber)
                            }
                        }
                        item {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(NDropColors.NavyElevated)
                                    .clickable { showNewField = true }
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = "New",
                                        tint = NDropColors.WhiteMuted,
                                        modifier = Modifier.size(16.dp))
                                    Text("New", style = NDropTypography.labelLarge,
                                        color = NDropColors.WhiteMuted)
                                }
                            }
                        }
                    }
                }

                // New collection input
                AnimatedVisibility(visible = showNewField || displayCollections.isEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newCollection,
                            onValueChange = { newCollection = it },
                            placeholder = { Text("New collection name") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor   = NDropColors.Amber,
                                unfocusedBorderColor = NDropColors.WhiteDim,
                                cursorColor          = NDropColors.Amber,
                                focusedTextColor     = NDropColors.White,
                                unfocusedTextColor   = NDropColors.WhiteMuted,
                                focusedPlaceholderColor   = NDropColors.WhiteDim,
                                unfocusedPlaceholderColor = NDropColors.WhiteDim
                            )
                        )
                        Button(
                            onClick = {
                                if (newCollection.isNotBlank()) onAssign(newCollection.trim())
                                else onDismiss()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NDropColors.Amber
                            )
                        ) {
                            Text("Add", color = NDropColors.SpaceNavy,
                                style = NDropTypography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}
