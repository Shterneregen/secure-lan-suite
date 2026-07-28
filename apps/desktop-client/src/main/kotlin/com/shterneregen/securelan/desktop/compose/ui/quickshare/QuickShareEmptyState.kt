package com.shterneregen.securelan.desktop.compose.ui.quickshare

import androidx.compose.runtime.Composable
import com.shterneregen.securelan.desktop.compose.state.quickshare.ComposeQuickShareState
import com.shterneregen.securelan.desktop.compose.ui.components.InlineEmptyState

@Composable
internal fun QuickShareEmptyState(state: ComposeQuickShareState) {
    InlineEmptyState(
        situation = state.emptySharesSituation,
        explanation = state.emptySharesExplanation,
        nextAction = state.emptySharesNextAction,
    )
}
