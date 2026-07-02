package com.olii.ndrop.ui.timer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olii.ndrop.ui.theme.NDropColors
import com.olii.ndrop.ui.theme.NDropTypography
import com.olii.ndrop.viewmodel.TimerViewModel
import com.olii.ndrop.viewmodel.TimerViewModel.TimerState
import com.olii.ndrop.viewmodel.toTimerDisplay

/**
 * NDrop — TimerScreen
 *
 * Circular countdown with arc progress, preset chips,
 * custom time picker, +time buttons, and background execution.
 *
 * Signature: Olii-8882
 */
@Composable
fun TimerScreen(
    onBack: () -> Unit,
    isDarkTheme: Boolean = true,
    viewModel: TimerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val surface   = if (isDarkTheme) NDropColors.SpaceNavy    else Color(0xFFF4F4F8)
    val card      = if (isDarkTheme) NDropColors.NavyElevated  else Color(0xFFFFFFFF)
    val textPrim  = if (isDarkTheme) NDropColors.White         else Color(0xFF0E0E12)
    val textSec   = if (isDarkTheme) NDropColors.WhiteMuted    else Color(0xFF5A5A72)
    val textDim   = if (isDarkTheme) NDropColors.WhiteDim      else Color(0xFFAAAAAC)
    val divider   = if (isDarkTheme) NDropColors.NavySubtle    else Color(0xFFE4E4EE)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(surface)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = "Back",
                    tint = textSec)
            }
            Spacer(Modifier.width(4.dp))
            Column {
                Text("Timer", style = NDropTypography.displayMedium, color = textPrim)
                Text(
                    when (state.state) {
                        TimerState.RUNNING  -> "Runs in background when you leave"
                        TimerState.PAUSED   -> "Paused"
                        TimerState.FINISHED -> "Done!"
                        TimerState.IDLE     -> "Set a duration to start"
                    },
                    style = NDropTypography.bodyMedium,
                    color = textSec
                )
            }
        }

        Spacer(Modifier.weight(0.4f))

        // ── Circular progress ring ────────────────────────────────────────────
        CircularTimerRing(
            progress         = state.progress,
            remainingSeconds = state.remainingSeconds,
            timerState       = state.state,
            divider          = divider,
            textSec          = textSec,
            textDim          = textDim,
            modifier         = Modifier.size(260.dp)
        )

        Spacer(Modifier.height(28.dp))

        // ── Quick-add buttons (running or paused) ─────────────────────────────
        AnimatedVisibility(visible = state.state == TimerState.RUNNING ||
                                     state.state == TimerState.PAUSED) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(horizontal = 24.dp)) {
                listOf(5 * 60L to "+5m", 15 * 60L to "+15m", 30 * 60L to "+30m").forEach { (s, l) ->
                    QuickAddChip(l, card = card, textSec = textSec) { viewModel.addTime(s) }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Preset chips + Custom button (idle or finished) ───────────────────
        AnimatedVisibility(visible = state.state == TimerState.IDLE ||
                                     state.state == TimerState.FINISHED) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Quick Start", style = NDropTypography.labelSmall,
                    color = textDim)

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(horizontal = 24.dp)) {
                    viewModel.presets.forEach { preset ->
                        PresetChip(preset.label, card = card, textPrim = textPrim) {
                            viewModel.startTimer(preset.seconds, preset.label)
                        }
                    }
                }

                // Custom timer button
                OutlinedButton(
                    onClick = viewModel::showCustomPicker,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = NDropColors.IndigoLight
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(NDropColors.IndigoDim)
                    )
                ) {
                    Icon(Icons.Rounded.EditNote, contentDescription = null,
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Custom Time", style = NDropTypography.labelLarge)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Controls ──────────────────────────────────────────────────────────
        TimerControls(
            state    = state.state,
            onPause  = viewModel::pauseTimer,
            onResume = viewModel::resumeTimer,
            onCancel = viewModel::cancelTimer
        )

        Spacer(Modifier.weight(1f))
    }

    // ── Custom time picker dialog ─────────────────────────────────────────────
    if (state.showCustomPicker) {
        CustomTimerDialog(
            divider = divider, textPrim = textPrim, textSec = textSec, textDim = textDim,
            onConfirm = { h, m, s, label -> viewModel.startCustomTimer(h, m, s, label) },
            onDismiss = viewModel::hideCustomPicker
        )
    }
}

// ── Custom Timer Dialog ───────────────────────────────────────────────────────

@Composable
private fun CustomTimerDialog(
    divider: Color, textPrim: Color, textSec: Color, textDim: Color,
    onConfirm: (hours: Int, minutes: Int, seconds: Int, label: String) -> Unit,
    onDismiss: () -> Unit
) {
    var hours   by remember { mutableStateOf("0") }
    var minutes by remember { mutableStateOf("25") }
    var seconds by remember { mutableStateOf("0") }
    var label   by remember { mutableStateOf("") }

    val totalSecs = ((hours.toIntOrNull() ?: 0) * 3600) +
                    ((minutes.toIntOrNull() ?: 0) * 60) +
                    (seconds.toIntOrNull() ?: 0)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = divider,
        title = {
            Text("Custom Timer", style = NDropTypography.titleLarge, color = textPrim)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                // H : M : S row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TimeField(value = hours,   label = "HH", onValueChange = { hours = it },
                        modifier = Modifier.weight(1f))
                    Text(":", style = NDropTypography.displayMedium, color = textDim)
                    TimeField(value = minutes, label = "MM", onValueChange = { minutes = it },
                        modifier = Modifier.weight(1f))
                    Text(":", style = NDropTypography.displayMedium, color = textDim)
                    TimeField(value = seconds, label = "SS", onValueChange = { seconds = it },
                        modifier = Modifier.weight(1f))
                }

                // Preview
                if (totalSecs > 0) {
                    Text(
                        totalSecs.toLong().toTimerDisplay(),
                        style = NDropTypography.titleLarge,
                        color = NDropColors.IndigoLight,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                // Optional label
                OutlinedTextField(
                    value       = label,
                    onValueChange = { label = it },
                    label       = { Text("Label (optional)") },
                    placeholder = { Text("e.g. Parking meter, Laundry...") },
                    singleLine  = true,
                    modifier    = Modifier.fillMaxWidth(),
                    colors      = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = NDropColors.Indigo,
                        unfocusedBorderColor = NDropColors.WhiteDim,
                        focusedLabelColor    = NDropColors.Indigo,
                        cursorColor          = NDropColors.Indigo,
                        focusedTextColor     = NDropColors.White,
                        unfocusedTextColor   = NDropColors.WhiteMuted,
                        focusedPlaceholderColor   = NDropColors.WhiteDim,
                        unfocusedPlaceholderColor = NDropColors.WhiteDim
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        hours.toIntOrNull() ?: 0,
                        minutes.toIntOrNull() ?: 0,
                        seconds.toIntOrNull() ?: 0,
                        label
                    )
                },
                enabled = totalSecs > 0,
                colors  = ButtonDefaults.buttonColors(containerColor = NDropColors.Indigo)
            ) {
                Text("Start", style = NDropTypography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = textSec)
            }
        }
    )
}

@Composable
private fun TimeField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value         = value,
        onValueChange = { new ->
            // Only allow 0-99
            if (new.length <= 2 && (new.isEmpty() || new.all { it.isDigit() })) {
                onValueChange(new)
            }
        },
        label         = { Text(label, style = NDropTypography.labelSmall) },
        singleLine    = true,
        textStyle     = NDropTypography.titleLarge.copy(textAlign = TextAlign.Center),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier      = modifier,
        colors        = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = NDropColors.Indigo,
            unfocusedBorderColor = NDropColors.WhiteDim,
            focusedLabelColor    = NDropColors.Indigo,
            cursorColor          = NDropColors.Indigo,
            focusedTextColor     = NDropColors.White,
            unfocusedTextColor   = NDropColors.White
        )
    )
}

// ── Circular ring ─────────────────────────────────────────────────────────────

@Composable
private fun CircularTimerRing(
    progress: Float,
    remainingSeconds: Long,
    timerState: TimerState,
    divider: Color, textSec: Color, textDim: Color,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue    = progress,
        animationSpec  = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label          = "timer_progress"
    )

    val ringColor = when (timerState) {
        TimerState.RUNNING  -> NDropColors.IndigoLight
        TimerState.PAUSED   -> NDropColors.Amber
        TimerState.FINISHED -> NDropColors.Rose
        TimerState.IDLE     -> textDim
    }

    val pulseAlpha by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue  = 0.6f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label         = "pulse_alpha"
    )

    Box(
        modifier = modifier.drawBehind {
            val strokeWidth = 14.dp.toPx()
            val inset   = strokeWidth / 2f
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(inset, inset)

            drawArc(color = divider, startAngle = -90f, sweepAngle = 360f,
                useCenter = false, topLeft = topLeft, size = arcSize,
                style = Stroke(strokeWidth, cap = StrokeCap.Round))

            drawArc(
                color = ringColor.copy(
                    alpha = if (timerState == TimerState.RUNNING) pulseAlpha else 1f
                ),
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter  = false, topLeft = topLeft, size = arcSize,
                style = Stroke(strokeWidth, cap = StrokeCap.Round)
            )
        },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            when (timerState) {
                TimerState.IDLE -> {
                    Text("⏱", fontSize = 48.sp)
                    Text("Pick a duration", style = NDropTypography.bodyMedium, color = textSec)
                }
                TimerState.FINISHED -> {
                    Text("✓", fontSize = 48.sp, color = NDropColors.Rose)
                    Text("Done!", style = NDropTypography.titleLarge, color = NDropColors.Rose)
                }
                else -> {
                    Text(
                        remainingSeconds.toTimerDisplay(),
                        style = NDropTypography.displayLarge.copy(fontSize = 44.sp),
                        color = ringColor, textAlign = TextAlign.Center
                    )
                    if (timerState == TimerState.PAUSED) {
                        Text("PAUSED", style = NDropTypography.labelSmall,
                            color = NDropColors.Amber)
                    }
                }
            }
        }
    }
}

// ── Controls ──────────────────────────────────────────────────────────────────

@Composable
private fun TimerControls(
    state: TimerState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        when (state) {
            TimerState.RUNNING -> {
                ControlButton(Icons.Rounded.Pause,     "Pause",  NDropColors.Amber, onPause)
                ControlButton(Icons.Rounded.Stop,      "Cancel", NDropColors.Rose,  onCancel, 48)
            }
            TimerState.PAUSED -> {
                ControlButton(Icons.Rounded.PlayArrow, "Resume", NDropColors.Mint,  onResume)
                ControlButton(Icons.Rounded.Stop,      "Cancel", NDropColors.Rose,  onCancel, 48)
            }
            TimerState.FINISHED -> {
                ControlButton(Icons.Rounded.Refresh,   "Reset",  NDropColors.Indigo, onCancel)
            }
            else -> {}
        }
    }
}

@Composable
private fun ControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String, color: Color, onClick: () -> Unit, size: Int = 64
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier.size(size.dp).clip(CircleShape)
                .background(color.copy(alpha = 0.15f)).clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = color,
                modifier = Modifier.size((size * 0.45f).dp))
        }
        Text(label, style = NDropTypography.labelSmall, color = color)
    }
}

@Composable
private fun PresetChip(label: String, card: Color, textPrim: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(12.dp))
            .background(card)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = NDropTypography.labelLarge, color = textPrim)
    }
}

@Composable
private fun QuickAddChip(label: String, card: Color, textSec: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(10.dp))
            .background(card)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = NDropTypography.labelLarge, color = textSec)
    }
}
