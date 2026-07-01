package com.shterneregen.securelan.desktop.compose.state.transfer

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.state.shell.ComposeShellMetadata

public data class ComposeAttachmentMenuLayoutContract(
    val minWidth: Dp = ComposeShellMetadata.ATTACHMENT_MENU_MIN_WIDTH,
    val maxWidth: Dp = ComposeShellMetadata.ATTACHMENT_MENU_MAX_WIDTH,
    val maxHeight: Dp = ComposeShellMetadata.ATTACHMENT_MENU_MAX_HEIGHT,
    val preferredVerticalDirection: String = "Above composer when there is room; otherwise below without leaving the window",
    val focusReturnTarget: String = "composer input or Attach button",
) {
    val boundsSummary: String =
        "Attach menu ${minWidth.value.toInt()}–${maxWidth.value.toInt()} dp wide, ≤ ${maxHeight.value.toInt()} dp high, kept inside the window."
}
