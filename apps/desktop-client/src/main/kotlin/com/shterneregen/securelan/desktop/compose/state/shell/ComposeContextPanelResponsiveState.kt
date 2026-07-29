package com.shterneregen.securelan.desktop.compose.state.shell

data class ComposeContextPanelResponsiveState(
    val widthPx: Int,
    val mode: ComposeContextPanelResponsiveMode,
) {
    val inlinePanelVisible: Boolean = mode != ComposeContextPanelResponsiveMode.DRAWER
    val drawerMode: Boolean = mode == ComposeContextPanelResponsiveMode.DRAWER
    val drawerEntryVisible: Boolean = drawerMode
    val drawerContentDescription: String = "Context Assistant drawer"
    val drawerOpenContentDescription: String = "Open Context Assistant drawer"
    val drawerCloseContentDescription: String = "Close Context Assistant drawer"
    val collapseSecondaryCards: Boolean = mode == ComposeContextPanelResponsiveMode.COLLAPSED_SECONDARY
    val collapseHistory: Boolean = mode == ComposeContextPanelResponsiveMode.COLLAPSED_HISTORY || drawerMode
    val preservesConversationFirst: Boolean = true
    val summary: String = when (mode) {
        ComposeContextPanelResponsiveMode.FULL_PANEL -> "Context Assistant visible."
        ComposeContextPanelResponsiveMode.COLLAPSED_SECONDARY -> "Secondary cards collapsed to preserve conversation."
        ComposeContextPanelResponsiveMode.COLLAPSED_HISTORY -> "History cards collapsed to preserve conversation space."
        ComposeContextPanelResponsiveMode.DRAWER -> "Context Assistant in drawer; conversation remains primary."
    }

    companion object {
        fun forWidth(widthPx: Int): ComposeContextPanelResponsiveState {
            val normalizedWidth = widthPx.coerceAtLeast(0)
            val mode = when {
                normalizedWidth >= 1600 -> ComposeContextPanelResponsiveMode.FULL_PANEL
                normalizedWidth >= 1440 -> ComposeContextPanelResponsiveMode.COLLAPSED_SECONDARY
                normalizedWidth >= 1280 -> ComposeContextPanelResponsiveMode.COLLAPSED_HISTORY
                else -> ComposeContextPanelResponsiveMode.DRAWER
            }
            return ComposeContextPanelResponsiveState(normalizedWidth, mode)
        }
    }
}
