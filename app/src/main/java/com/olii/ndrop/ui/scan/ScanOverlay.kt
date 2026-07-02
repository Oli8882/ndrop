package com.olii.ndrop.ui.scan

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olii.ndrop.nfc.TagType
import com.olii.ndrop.ui.theme.NDropColors
import com.olii.ndrop.ui.theme.NDropTypography
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * NDrop — ScanOverlay
 *
 * The signature visual: a radial ripple burst emanating from center,
 * like dropping a stone in water. Color coded by tag type.
 *
 * Shown for 2.2s then auto-dismissed via [onDismiss].
 *
 * Signature: Olii-8882
 */

data class ScanResult(
    val tagType: TagType,
    val message: String,
    val subMessage: String = ""
)

@Composable
fun ScanOverlay(
    result: ScanResult,
    onDismiss: () -> Unit
) {
    val rippleColor = when (result.tagType) {
        TagType.PARKING   -> NDropColors.Mint
        TagType.DISCOVERY -> NDropColors.Amber
        TagType.TIMER     -> NDropColors.IndigoLight
        TagType.UNKNOWN   -> NDropColors.WhiteMuted
    }

    val icon = when (result.tagType) {
        TagType.PARKING   -> "🅿️"
        TagType.DISCOVERY -> "✦"
        TagType.TIMER     -> "⏱"
        TagType.UNKNOWN   -> "?"
    }

    // Auto-dismiss after 2200ms
    LaunchedEffect(Unit) {
        delay(2_200L)
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NDropColors.SpaceNavy.copy(alpha = 0.88f)),
        contentAlignment = Alignment.Center
    ) {
        // ── Three staggered ripple rings ──────────────────────────────────────
        RippleRing(color = rippleColor, delayMs = 0,    targetScale = 3.5f, alpha = 0.25f)
        RippleRing(color = rippleColor, delayMs = 200,  targetScale = 2.5f, alpha = 0.35f)
        RippleRing(color = rippleColor, delayMs = 400,  targetScale = 1.8f, alpha = 0.45f)

        // ── Center icon + text ────────────────────────────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon circle
            val iconScale = remember { Animatable(0.5f) }
            LaunchedEffect(Unit) {
                iconScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .scale(iconScale.value)
                    .clip(CircleShape)
                    .background(rippleColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = icon,
                    fontSize = 34.sp
                )
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = result.message,
                style = NDropTypography.titleLarge,
                color = NDropColors.White,
                textAlign = TextAlign.Center
            )

            if (result.subMessage.isNotEmpty()) {
                Text(
                    text = result.subMessage,
                    style = NDropTypography.bodyMedium,
                    color = NDropColors.WhiteMuted,
                    textAlign = TextAlign.Center
                )
            }

            // Colored accent line under the text
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(rippleColor)
            )
        }
    }
}

@Composable
private fun RippleRing(
    color: Color,
    delayMs: Int,
    targetScale: Float,
    alpha: Float
) {
    val scale = remember { Animatable(0f) }
    val rippleAlpha = remember { Animatable(alpha) }

    LaunchedEffect(Unit) {
        delay(delayMs.toLong())
        launch {
            scale.animateTo(
                targetValue = targetScale,
                animationSpec = tween(durationMillis = 1200, easing = EaseOut)
            )
        }
        launch {
            rippleAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 1200, easing = EaseIn)
            )
        }
    }

    Box(
        modifier = Modifier
            .size(100.dp)
            .scale(scale.value)
            .clip(CircleShape)
            .background(color.copy(alpha = rippleAlpha.value))
    )
}
