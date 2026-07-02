package com.olii.ndrop.ui.scan

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.olii.ndrop.ui.theme.NDropColors
import com.olii.ndrop.ui.theme.NDropTypography
import kotlinx.coroutines.delay

/**
 * NDrop — FloorNoteOverlay (Feature 1)
 *
 * Slides up 1.5s after a Parking scan completes.
 * Lets user add "Level 3", "Zone B4" etc.
 * Auto-dismisses after 8s if ignored.
 *
 * Signature: Olii-8882
 */
@Composable
fun FloorNoteOverlay(
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var note by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    // Slide up after 1.5s delay, auto-dismiss after 8s
    LaunchedEffect(Unit) {
        delay(1_500L)
        visible = true
        delay(8_000L)
        if (note.isBlank()) onDismiss()
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
                        Text("Where exactly? 🅿️", style = NDropTypography.titleMedium)
                        Text(
                            "Optional — tap Save or swipe to skip",
                            style = NDropTypography.bodyMedium,
                            color = NDropColors.WhiteDim
                        )
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Skip", color = NDropColors.WhiteDim,
                            style = NDropTypography.labelLarge)
                    }
                }

                // Quick suggestion chips
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Level 1", "Level 2", "Level 3", "Ground").forEach { suggestion ->
                        SuggestionChip(
                            onClick = { note = suggestion },
                            label = { Text(suggestion,
                                style = NDropTypography.labelSmall,
                                color = NDropColors.WhiteMuted) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = NDropColors.NavyElevated
                            ),
                            border = null
                        )
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text("e.g. Level 3, Zone B4, Near elevator") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboard?.hide()
                            onSave(note.trim())
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = NDropColors.Mint,
                        unfocusedBorderColor = NDropColors.WhiteDim,
                        focusedLabelColor    = NDropColors.Mint,
                        cursorColor          = NDropColors.Mint,
                        focusedTextColor     = NDropColors.White,
                        unfocusedTextColor   = NDropColors.WhiteMuted,
                        focusedPlaceholderColor   = NDropColors.WhiteDim,
                        unfocusedPlaceholderColor = NDropColors.WhiteDim
                    )
                )

                Button(
                    onClick = { keyboard?.hide(); onSave(note.trim()) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NDropColors.Mint)
                ) {
                    Text("Save Note", style = NDropTypography.labelLarge,
                        color = NDropColors.SpaceNavy)
                }
            }
        }
    }
}
