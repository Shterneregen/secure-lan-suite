package com.shterneregen.securelan.desktop.compose.ui.steganography

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.ComposeSteganographyState
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
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
        SteganographyWorkflowStatus(
            label = when {
                state.extractedMessage.isNotBlank() -> "Completed"
                error -> "Error"
                state.canExtractMessage -> "Ready"
                else -> "Needs input"
            },
            detail = when {
                state.extractedMessage.isNotBlank() -> state.extractedSummary
                error -> state.statusText
                !state.hasInput -> "Choose, paste, or drop a stego BMP."
                state.passwordRequiredForExtract && !state.passwordReady -> "Enter the message password."
                else -> "Ready to extract the hidden message."
            },
            error = error,
            completed = state.extractedMessage.isNotBlank(),
            ready = state.canExtractMessage,
        )

        Button(
            onClick = onExtract,
            enabled = !previewOnly && state.canExtractMessage,
            modifier = Modifier.align(Alignment.End),
        ) {
            Text("Extract message")
        }

        if (state.extractedMessage.isNotBlank()) {
            OutlinedTextField(
                value = state.extractedMessage,
                onValueChange = {},
                readOnly = true,
                label = { Text("Extracted message") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                maxLines = 10,
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
