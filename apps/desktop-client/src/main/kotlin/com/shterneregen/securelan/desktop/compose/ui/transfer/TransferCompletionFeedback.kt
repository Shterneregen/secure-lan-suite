package com.shterneregen.securelan.desktop.compose.ui.transfer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import com.shterneregen.securelan.desktop.compose.state.transfer.ComposeFileTransferState
import com.shterneregen.securelan.desktop.compose.motionTween
import com.shterneregen.securelan.desktop.compose.ui.components.MicroFeedbackPill
import com.shterneregen.securelan.desktop.compose.util.MicrointeractionTone

@Composable
internal fun TransferCompletionFeedback(transferState: ComposeFileTransferState) {
    val latest = transferState.recentEntryRows.lastOrNull { it.completed || it.failed } ?: return
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(motionTween()) + expandVertically(motionTween(), expandFrom = Alignment.Top),
        exit = shrinkVertically(motionTween(), shrinkTowards = Alignment.Top) + fadeOut(motionTween()),
    ) {
        MicroFeedbackPill(
            text = if (latest.completed) "Transfer completed: ${latest.fileName}" else "Transfer failed: ${latest.fileName}",
            tone = if (latest.completed) MicrointeractionTone.SUCCESS else MicrointeractionTone.FAILURE,
        )
    }
}
