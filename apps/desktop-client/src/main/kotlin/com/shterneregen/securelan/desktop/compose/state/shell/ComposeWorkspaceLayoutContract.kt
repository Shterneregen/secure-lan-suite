package com.shterneregen.securelan.desktop.compose.state.shell

import androidx.compose.ui.unit.Dp

data class ComposeWorkspaceLayoutContract(
    val composerMinHeight: Dp = ComposeShellMetadata.COMPOSER_MIN_HEIGHT,
    val composerSafeVerticalSpace: Dp = ComposeShellMetadata.COMPOSER_SAFE_VERTICAL_SPACE,
    val connectionHubExpandedMaxFraction: Float = ComposeShellMetadata.CONNECTION_HUB_EXPANDED_MAX_FRACTION,
    val minChatSurfaceHeight: Dp = ComposeShellMetadata.MIN_CHAT_SURFACE_HEIGHT,
    val centerColumnSpacing: Dp = ComposeShellMetadata.CENTER_COLUMN_SPACING,
    val advancedPaneMaxHeight: Dp = ComposeShellMetadata.ADVANCED_PANE_MAX_HEIGHT,
) {
    val layoutSummary: String = "Composer ≥ ${composerMinHeight.value.toInt()} dp; safe space ≥ ${composerSafeVerticalSpace.value.toInt()} dp; " +
        "hub ≤ ${(connectionHubExpandedMaxFraction * 100).toInt()}% of center; chat ≥ ${minChatSurfaceHeight.value.toInt()} dp; " +
        "advanced ≤ ${advancedPaneMaxHeight.value.toInt()} dp"
}
