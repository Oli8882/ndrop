package com.olii.ndrop.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * NDrop — Design System
 * Signature: Olii-8882
 */

object NDropColors {
    val SpaceNavy       = Color(0xFF0A0E1A)
    val NavyElevated    = Color(0xFF111828)
    val NavySubtle      = Color(0xFF1A2235)

    val Indigo          = Color(0xFF5B5FEF)
    val IndigoLight     = Color(0xFF7B7FF5)
    val IndigoDim       = Color(0xFF2D3080)

    val Mint            = Color(0xFF3DFFC0)
    val MintDim         = Color(0xFF0D5C44)
    val Amber           = Color(0xFFFFB547)
    val AmberDim        = Color(0xFF5C3D0D)
    val Rose            = Color(0xFFFF6B8A)

    val White           = Color(0xFFF0F2FF)
    val WhiteMuted      = Color(0xFFADB5D1)
    val WhiteDim        = Color(0xFF4A5270)

    // Light theme surfaces
    val LightBackground = Color(0xFFF4F4F8)
    val LightSurface    = Color(0xFFFFFFFF)
    val LightSubtle     = Color(0xFFEEEEF6)
    val LightTextPrim   = Color(0xFF0E0E12)
    val LightTextSec    = Color(0xFF5A5A72)
    val LightTextDim    = Color(0xFFAAAAAC)
}

val NDropTypography = Typography(
    displayLarge  = TextStyle(fontWeight = FontWeight.Bold,     fontSize = 36.sp, letterSpacing = (-1).sp),
    displayMedium = TextStyle(fontWeight = FontWeight.Bold,     fontSize = 28.sp, letterSpacing = (-0.5).sp),
    titleLarge    = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, letterSpacing = (-0.3).sp),
    titleMedium   = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, letterSpacing = 0.sp),
    bodyLarge     = TextStyle(fontWeight = FontWeight.Normal,   fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium    = TextStyle(fontWeight = FontWeight.Normal,   fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge    = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.5.sp),
    labelSmall    = TextStyle(fontWeight = FontWeight.Medium,   fontSize = 11.sp, letterSpacing = 0.8.sp)
)

private val DarkColorScheme = darkColorScheme(
    primary             = NDropColors.Indigo,
    onPrimary           = NDropColors.White,
    primaryContainer    = NDropColors.IndigoDim,
    onPrimaryContainer  = NDropColors.IndigoLight,
    secondary           = NDropColors.Mint,
    onSecondary         = NDropColors.SpaceNavy,
    tertiary            = NDropColors.Amber,
    onTertiary          = NDropColors.SpaceNavy,
    background          = NDropColors.SpaceNavy,
    onBackground        = NDropColors.White,
    surface             = NDropColors.NavyElevated,
    onSurface           = NDropColors.White,
    surfaceVariant      = NDropColors.NavySubtle,
    onSurfaceVariant    = NDropColors.WhiteMuted,
    error               = NDropColors.Rose,
    onError             = NDropColors.White,
    outline             = NDropColors.WhiteDim
)

private val LightColorScheme = lightColorScheme(
    primary             = NDropColors.Indigo,
    onPrimary           = Color.White,
    primaryContainer    = Color(0xFFE8E8FF),
    onPrimaryContainer  = NDropColors.IndigoDim,
    secondary           = Color(0xFF00A37A),
    onSecondary         = Color.White,
    tertiary            = Color(0xFFCC8800),
    onTertiary          = Color.White,
    background          = NDropColors.LightBackground,
    onBackground        = NDropColors.LightTextPrim,
    surface             = NDropColors.LightSurface,
    onSurface           = NDropColors.LightTextPrim,
    surfaceVariant      = NDropColors.LightSubtle,
    onSurfaceVariant    = NDropColors.LightTextSec,
    error               = NDropColors.Rose,
    onError             = Color.White,
    outline             = NDropColors.LightTextDim
)

@Composable
fun NDropTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography  = NDropTypography,
        content     = content
    )
}
