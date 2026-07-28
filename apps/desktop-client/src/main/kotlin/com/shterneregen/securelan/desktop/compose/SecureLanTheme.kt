package com.shterneregen.securelan.desktop.compose

import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Typography
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Immutable
data class SecureLanColorTokens(
    val background: Color,
    val surfaceLevel1: Color,
    val surfaceLevel2: Color,
    val surfaceLevel3: Color,
    val accent: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val borderSubtle: Color,
    val borderFocus: Color,
)

@Immutable
data class SecureLanSpacingTokens(
    val xxs: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val sm: Dp = 12.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 20.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val xxxl: Dp = 40.dp,
)

@Immutable
data class SecureLanRadiusTokens(
    val small: Dp = 8.dp,
    val medium: Dp = 12.dp,
    val large: Dp = 20.dp,
    val pill: Dp = 999.dp,
)

@Immutable
data class SecureLanTypographyTokens(
    val fontFamily: FontFamily = FontFamily.Default,
    val titleMin: TextUnit = 24.sp,
    val titleMax: TextUnit = 32.sp,
    val bodySmall: TextUnit = 13.sp,
    val bodyDefault: TextUnit = 14.sp,
    val bodyLarge: TextUnit = 15.sp,
    val caption: TextUnit = 11.sp,
    val overline: TextUnit = 10.sp,
)

@Immutable
data class SecureLanElevationTokens(
    val flat: Dp = 0.dp,
    val popover: Dp = 6.dp,
    val modal: Dp = 12.dp,
)

@Immutable
data class SecureLanBorderTokens(
    val subtle: Dp = 1.dp,
    val focus: Dp = 2.dp,
)

@Immutable
data class SecureLanDensityTokens(
    val buttonMinHeight: Dp = 36.dp,
    val inputMinHeight: Dp = 38.dp,
    val sidebarRowMinHeight: Dp = 48.dp,
    val sidebarRowMaxHeight: Dp = 60.dp,
    val composerMinHeight: Dp = 52.dp,
)

@Immutable
data class SecureLanMotionTokens(
    val durationFast: Int = 150,
    val durationDefault: Int = 200,
    val durationSlow: Int = 250,
    val durationInstant: Int = 0,
)

@Immutable
data class SecureLanDesignTokens(
    val colors: SecureLanColorTokens,
    val spacing: SecureLanSpacingTokens = SecureLanSpacingTokens(),
    val radius: SecureLanRadiusTokens = SecureLanRadiusTokens(),
    val typography: SecureLanTypographyTokens = SecureLanTypographyTokens(),
    val elevation: SecureLanElevationTokens = SecureLanElevationTokens(),
    val border: SecureLanBorderTokens = SecureLanBorderTokens(),
    val density: SecureLanDensityTokens = SecureLanDensityTokens(),
    val motion: SecureLanMotionTokens = SecureLanMotionTokens(),
)

object SecureLanThemeTokens {
    val DarkColors: SecureLanColorTokens = SecureLanColorTokens(
        background = Color(0xFF0B111A),
        surfaceLevel1 = Color(0xFF101824),
        surfaceLevel2 = Color(0xFF162233),
        surfaceLevel3 = Color(0xFF1D2B40),
        accent = Color(0xFF3B82F6),
        success = Color(0xFF4ADE80),
        warning = Color(0xFFF59E0B),
        error = Color(0xFFF87171),
        textPrimary = Color(0xFFF0F6FF),
        textSecondary = Color(0xFFB7C4D8),
        textTertiary = Color(0xFF8191A8),
        borderSubtle = Color(0x243E536F),
        borderFocus = Color(0xFF60A5FA),
    )

    val LightColors: SecureLanColorTokens = SecureLanColorTokens(
        background = Color(0xFFF4F7FB),
        surfaceLevel1 = Color(0xFFFFFFFF),
        surfaceLevel2 = Color(0xFFEFF4FA),
        surfaceLevel3 = Color(0xFFE4ECF6),
        accent = Color(0xFF2563EB),
        success = Color(0xFF15803D),
        warning = Color(0xFFB45309),
        error = Color(0xFFB91C1C),
        textPrimary = Color(0xFF172033),
        textSecondary = Color(0xFF475569),
        textTertiary = Color(0xFF64748B),
        borderSubtle = Color(0x2633445C),
        borderFocus = Color(0xFF2563EB),
    )

    val Dark: SecureLanDesignTokens = SecureLanDesignTokens(colors = DarkColors)
    val Light: SecureLanDesignTokens = SecureLanDesignTokens(colors = LightColors)
}

val LocalSecureLanDesignTokens = staticCompositionLocalOf { SecureLanThemeTokens.Dark }

val LocalReducedMotion = staticCompositionLocalOf { false }

@Composable
internal fun <T> motionTween(
    durationMillis: Int = LocalSecureLanDesignTokens.current.motion.durationDefault,
    delayMillis: Int = 0,
    easing: androidx.compose.animation.core.Easing = androidx.compose.animation.core.FastOutSlowInEasing,
): androidx.compose.animation.core.TweenSpec<T> = motionTween(
    reducedMotion = LocalReducedMotion.current,
    durationMillis = durationMillis,
    delayMillis = delayMillis,
    easing = easing,
)

internal fun <T> motionTween(
    reducedMotion: Boolean,
    durationMillis: Int = 200,
    delayMillis: Int = 0,
    easing: androidx.compose.animation.core.Easing = androidx.compose.animation.core.FastOutSlowInEasing,
): androidx.compose.animation.core.TweenSpec<T> {
    return androidx.compose.animation.core.tween(
        durationMillis = if (reducedMotion) 0 else durationMillis,
        delayMillis = delayMillis,
        easing = easing,
    )
}

private fun SecureLanColorTokens.toMaterialDarkColors(): Colors = darkColors(
    primary = accent,
    onPrimary = Color.White,
    secondary = success,
    onSecondary = Color(0xFF06152D),
    background = background,
    onBackground = textPrimary,
    surface = surfaceLevel1,
    onSurface = textPrimary,
    error = error,
)

private fun SecureLanColorTokens.toMaterialLightColors(): Colors = lightColors(
    primary = accent,
    onPrimary = Color.White,
    secondary = success,
    onSecondary = Color.White,
    background = background,
    onBackground = textPrimary,
    surface = surfaceLevel1,
    onSurface = textPrimary,
    error = error,
)

private fun secureLanDesktopTypography(tokens: SecureLanTypographyTokens): Typography = Typography(
    h4 = TextStyle(fontSize = tokens.titleMax, fontWeight = FontWeight.Bold, fontFamily = tokens.fontFamily),
    h5 = TextStyle(fontSize = tokens.titleMin, fontWeight = FontWeight.Bold, fontFamily = tokens.fontFamily),
    h6 = TextStyle(fontSize = tokens.bodyLarge, fontWeight = FontWeight.Bold, fontFamily = tokens.fontFamily),
    subtitle1 = TextStyle(fontSize = tokens.bodySmall, fontWeight = FontWeight.Bold, fontFamily = tokens.fontFamily),
    subtitle2 = TextStyle(fontSize = tokens.bodyDefault, fontWeight = FontWeight.Bold, fontFamily = tokens.fontFamily),
    body1 = TextStyle(fontSize = tokens.bodySmall, fontFamily = tokens.fontFamily),
    body2 = TextStyle(fontSize = tokens.bodyDefault, fontFamily = tokens.fontFamily),
    button = TextStyle(fontSize = tokens.bodyDefault, fontWeight = FontWeight.Bold, fontFamily = tokens.fontFamily),
    caption = TextStyle(fontSize = tokens.caption, fontFamily = tokens.fontFamily),
    overline = TextStyle(fontSize = tokens.overline, fontWeight = FontWeight.Bold, fontFamily = tokens.fontFamily, letterSpacing = 0.5.sp),
)

@Composable
fun SecureLanTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val tokens = if (darkTheme) SecureLanThemeTokens.Dark else SecureLanThemeTokens.Light

    CompositionLocalProvider(LocalSecureLanDesignTokens provides tokens) {
        MaterialTheme(
            colors = if (darkTheme) tokens.colors.toMaterialDarkColors() else tokens.colors.toMaterialLightColors(),
            typography = secureLanDesktopTypography(tokens.typography),
            content = content,
        )
    }
}
