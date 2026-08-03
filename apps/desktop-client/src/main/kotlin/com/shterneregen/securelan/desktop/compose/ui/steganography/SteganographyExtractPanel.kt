package com.shterneregen.securelan.desktop.compose.ui.steganography

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.state.steganography.ComposeSteganographyState
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButton
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButtonTone
import java.nio.file.Path

@Composable
internal fun SteganographyExtractPanel(
    state: ComposeSteganographyState,
    inputPath: String,
    onChooseInput: () -> Unit,
    onPasteInput: () -> Unit,
    onInputSelected: (Path) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    encryptedExtract: Boolean,
    onEncryptedExtractChange: (Boolean) -> Unit,
    onExtract: () -> Unit,
    onCopyResult: () -> Unit,
    onSaveResult: () -> Unit,
    previewOnly: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        SteganographyImageDropZone(
            label = "Stego image",
            value = inputPath,
            emptyHint = "Choose the BMP containing a hidden message",
            onChoose = onChooseInput,
            onPaste = onPasteInput,
            onImageSelected = onInputSelected,
            enabled = !previewOnly,
        )

        SteganographyPasswordRow(
            checked = encryptedExtract,
            onCheckedChange = onEncryptedExtractChange,
            label = "Message is password-protected",
            password = password,
            onPasswordChange = onPasswordChange,
        )

        val error = steganographyStatusIsError(state.statusText)
        val readinessHint = when {
            !state.hasInput -> "Choose, paste, or drop a stego BMP."
            state.passwordRequiredForExtract && !state.passwordReady -> "Enter the message password."
            else -> "Ready to extract the hidden message."
        }
        if (error) {
            SteganographyWorkflowStatus(
                label = "Error",
                detail = state.statusText,
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
                text = if (state.extractedMessage.isNotBlank()) state.extractedSummary else readinessHint,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.60f),
            )
            Button(
                onClick = onExtract,
                enabled = !previewOnly && state.canExtractMessage,
            ) {
                Text(if (state.extractedMessage.isBlank()) "Extract message" else "Extract again")
            }
        }

        if (state.extractedMessage.isNotBlank()) {
            SteganographyMultilineTextField(
                value = state.extractedMessage,
                onValueChange = {},
                readOnly = true,
                label = "Extracted message",
                height = 210.dp,
            )
            FlowRow(
                modifier = Modifier.align(Alignment.End),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CompactButton(onClick = onCopyResult) { Text("Copy") }
                CompactButton(
                    onClick = onSaveResult,
                    tone = CompactButtonTone.TERTIARY,
                ) {
                    Text("Save as text")
                }
            }
        }
    }
}
