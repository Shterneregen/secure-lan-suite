package com.shterneregen.securelan.desktop.compose

import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Typography
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val SecureLanDarkColors: Colors = darkColors(
    primary = Color(0xFF2563EB),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF7FB7FF),
    onSecondary = Color(0xFF06152D),
    background = Color(0xFF0C1118),
    onBackground = Color(0xFFE7EEF9),
    surface = Color(0xFF141D2A),
    onSurface = Color(0xFFE7EEF9),
    error = Color(0xFFFFB4B4),
)

private val SecureLanLightColors: Colors = lightColors(
    primary = Color(0xFF2563EB),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF0F766E),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFF6F8FB),
    onBackground = Color(0xFF223047),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF223047),
    error = Color(0xFFB91C1C),
)

private val SecureLanDesktopTypography = Typography(
    h6 = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold),
    subtitle1 = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold),
    subtitle2 = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold),
    body1 = TextStyle(fontSize = 13.sp),
    body2 = TextStyle(fontSize = 12.sp),
    button = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold),
    caption = TextStyle(fontSize = 11.sp),
)

@Composable
fun SecureLanTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colors = if (darkTheme) SecureLanDarkColors else SecureLanLightColors,
        typography = SecureLanDesktopTypography,
        content = content,
    )
}
