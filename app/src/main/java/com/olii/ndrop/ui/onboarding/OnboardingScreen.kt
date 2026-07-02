package com.olii.ndrop.ui.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olii.ndrop.ui.theme.NDropColors
import com.olii.ndrop.ui.theme.NDropTypography
import kotlinx.coroutines.launch

/**
 * NDrop — OnboardingScreen
 *
 * Shown only on first launch (gated by DataStore flag in OnboardingViewModel).
 * 3 pages, horizontal swipe, skip + get started CTAs.
 *
 * Signature: Olii-8882
 */

data class OnboardingPage(
    val emoji: String,
    val title: String,
    val body: String,
    val accentColor: Color
)

private val pages = listOf(
    OnboardingPage(
        emoji       = "📡",
        title       = "Tap. Drop. Done.",
        body        = "Hold any NFC tag near your phone to instantly save your location. No menus, no typing — just one tap.",
        accentColor = NDropColors.Indigo
    ),
    OnboardingPage(
        emoji       = "🅿️",
        title       = "Never Lose Your Car",
        body        = "Scan your Parking Tag and NDrop pins your car's exact location. Get walking directions back in one tap.",
        accentColor = NDropColors.Mint
    ),
    OnboardingPage(
        emoji       = "✦",
        title       = "Collect Cool Places",
        body        = "Scan your Discovery Tag at any interesting spot — a café, viewpoint, or hidden gem. Build your personal map.",
        accentColor = NDropColors.Amber
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.lastIndex

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NDropColors.SpaceNavy)
    ) {

        // Skip button (top right)
        if (!isLastPage) {
            TextButton(
                onClick = onFinish,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(end = 16.dp, top = 8.dp)
            ) {
                Text("Skip", style = NDropTypography.labelLarge, color = NDropColors.WhiteDim)
            }
        }

        // Pages
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { index ->
            OnboardingPageContent(page = pages[index])
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 32.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Dot indicators
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pages.forEachIndexed { index, page ->
                    val isActive = index == pagerState.currentPage
                    val width by animateDpAsState(
                        targetValue = if (isActive) 24.dp else 8.dp,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "dot_width"
                    )
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(
                                if (isActive) pages[pagerState.currentPage].accentColor
                                else NDropColors.WhiteDim
                            )
                    )
                }
            }

            // CTA button
            Button(
                onClick = {
                    if (isLastPage) {
                        onFinish()
                    } else {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = pages[pagerState.currentPage].accentColor
                )
            ) {
                Text(
                    text = if (isLastPage) "Get Started" else "Next",
                    style = NDropTypography.labelLarge,
                    color = if (pages[pagerState.currentPage].accentColor == NDropColors.Mint ||
                                pages[pagerState.currentPage].accentColor == NDropColors.Amber)
                        NDropColors.SpaceNavy else NDropColors.White
                )
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    val scale = remember { Animatable(0.7f) }
    LaunchedEffect(page) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness    = Spring.StiffnessMedium
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Floating emoji orb
        Box(
            modifier = Modifier
                .size(140.dp)
                .scale(scale.value)
                .clip(CircleShape)
                .background(page.accentColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(page.accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(page.emoji, fontSize = 52.sp)
            }
        }

        Spacer(Modifier.height(48.dp))

        Text(
            text = page.title,
            style = NDropTypography.displayMedium,
            color = NDropColors.White,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = page.body,
            style = NDropTypography.bodyLarge,
            color = NDropColors.WhiteMuted,
            textAlign = TextAlign.Center,
            lineHeight = 26.sp
        )

        Spacer(Modifier.height(160.dp)) // Space for bottom controls
    }
}
