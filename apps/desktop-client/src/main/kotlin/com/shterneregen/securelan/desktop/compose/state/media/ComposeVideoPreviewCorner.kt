package com.shterneregen.securelan.desktop.compose.state.media

internal enum class ComposeVideoPreviewCorner(
    val atEnd: Boolean,
    val atBottom: Boolean,
) {
    TOP_START(atEnd = false, atBottom = false),
    TOP_END(atEnd = true, atBottom = false),
    BOTTOM_START(atEnd = false, atBottom = true),
    BOTTOM_END(atEnd = true, atBottom = true),
}

internal fun settleVideoPreviewCorner(
    current: ComposeVideoPreviewCorner,
    dragX: Float,
    dragY: Float,
    thresholdPx: Float = 32f,
): ComposeVideoPreviewCorner {
    val atEnd = when {
        dragX > thresholdPx -> true
        dragX < -thresholdPx -> false
        else -> current.atEnd
    }
    val atBottom = when {
        dragY > thresholdPx -> true
        dragY < -thresholdPx -> false
        else -> current.atBottom
    }
    return ComposeVideoPreviewCorner.entries.first { it.atEnd == atEnd && it.atBottom == atBottom }
}
