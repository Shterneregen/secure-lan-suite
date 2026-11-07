package com.shterneregen.securelan.desktop.compose.util

import androidx.compose.ui.graphics.Color

internal fun Color.copy(alpha: Float): Color = Color(
    red = red,
    green = green,
    blue = blue,
    alpha = alpha,
)
