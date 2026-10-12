package com.shterneregen.securelan.desktop.compose.ui.steganography

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.state.steganography.ComposeSteganographyState
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButton
import java.nio.file.Path

@Composable
internal fun SteganographyHidePanel(
    state: ComposeSteganographyState,
    coverPath: String,
    onChooseCover: () -> Unit,
    onPasteCover: () -> Unit,
    onCoverSelected: (Path) -> Unit,
    outputPath: String,
    onOutputPathChange: (String) -> Unit,
    onChooseOutput: () -> Unit,
    message: String,
    onMessageChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    onEncryptChange: (Boolean) -> Unit,
    onHide: () -> Unit,
    savedOutput: Path?,
    onOpenOutputFolder: () -> Unit,
    onSendOutput: (() -> Unit)?,
    sendOutputLabel: String?,
    previewOnly: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        SteganographyImageDropZone(
            label = "Cover image",
            value = coverPath,
            emptyHint = "Choose an image to hide a message in",
            onChoose = onChooseCover,
            onPaste = onPasteCover,
            onImageSelected = onCoverSelected,
            enabled = !previewOnly,
        )

        state.capacity?.let { capacity -> SteganographyCapacityBar(capacity.payloadCapacityBytes, capacity.width, capacity.height, capacity.bitsPerPixel) }

        SteganographyMultilineTextField(
            value = message,
            onValueChange = onMessageChange,
            label = "Message to hide",
            placeholder = "Type or paste the text you want to hide",
            isError = !state.messageFitsCapacity,
            height = 136.dp,
        )
        Text(
            text = state.messageCapacityLabel,
            modifier = Modifier.align(Alignment.End),
            style = MaterialTheme.typography.caption,
            color = if (state.messageFitsCapacity) {
                MaterialTheme.colors.onSurface.copy(alpha = 0.56f)
            } else {
                MaterialTheme.colors.error
            },
        )

        SteganographyPasswordField(
            password = password,
            placeholder = "Leave empty for no password",
            emptyHint = "No password protection",
            protectedHint = "Message will be password-protected",
            onPasswordChange = { value ->
                onPasswordChange(value)
                onEncryptChange(value.isNotBlank())
            },
        )

        SteganographyFileRow(
            value = outputPath,
            onValueChange = onOutputPathChange,
            label = "Output BMP",
            placeholder = "Suggested after the cover is selected",
            buttonText = "Choose location",
            onChoose = onChooseOutput,
            enabled = !previewOnly,
        )

        val error = steganographyStatusIsError(state.statusText)
        val hint = hideReadinessHint(state)
        if (error) {
            SteganographyWorkflowStatus(
                label = "Error",
                detail = hint,
                error = true,
                completed = false,
                ready = false,
            )
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = if (state.canHideMessage) "Ready to create the stego BMP" else hint,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.60f),
            )
            Button(
                onClick = onHide,
                enabled = !previewOnly && state.canHideMessage,
            ) {
                Text("Hide message")
            }
        }

        if (savedOutput != null) {
            val tokens = LocalSecureLanDesignTokens.current
            Surface(
                modifier = Modifier.align(Alignment.End).widthIn(max = 720.dp),
                shape = RoundedCornerShape(tokens.radius.medium),
                border = BorderStroke(1.dp, MaterialTheme.colors.primary.copy(alpha = 0.30f)),
                color = tokens.colors.surfaceLevel2.copy(alpha = 0.70f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text("Stego BMP created", style = MaterialTheme.typography.subtitle2)
                        Text(
                            savedOutput.fileName?.toString() ?: savedOutput.toString(),
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.68f),
                        )
                    }
                    CompactButton(onClick = onOpenOutputFolder) { Text("Open folder") }
                    if (onSendOutput != null && !sendOutputLabel.isNullOrBlank()) {
                        CompactButton(onClick = onSendOutput) { Text(sendOutputLabel) }
                    }
                }
            }
        }

    }
}

@Composable
private fun SteganographyCapacityBar(capacity: Int, width: Int, height: Int, bitsPerPixel: Int) {
    val tokens = LocalSecureLanDesignTokens.current
    Surface(
        shape = RoundedCornerShape(tokens.radius.small),
        border = BorderStroke(1.dp, tokens.colors.borderSubtle),
        color = tokens.colors.surfaceLevel2.copy(alpha = 0.55f),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Available capacity", style = MaterialTheme.typography.caption)
            Text(
                "$capacity bytes  ·  $width × $height  ·  $bitsPerPixel-bit BMP",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f),
            )
        }
    }
}

private fun hideReadinessHint(state: ComposeSteganographyState): String = when {
    steganographyStatusIsError(state.statusText) -> state.statusText
    !state.hasCover -> "Choose, paste, or drop a cover image."
    !state.hasMessage -> "Enter a message to hide."
    !state.messageFitsCapacity -> "Shorten the message or select a larger image."
    state.passwordRequiredForHide && !state.passwordReady -> "Enter a password."
    !state.hasOutput -> "Choose where to save the stego BMP."
    else -> "Ready to create a BMP containing the hidden message."
}
