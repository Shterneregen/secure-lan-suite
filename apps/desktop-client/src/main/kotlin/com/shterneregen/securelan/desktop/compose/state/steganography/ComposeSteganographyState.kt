package com.shterneregen.securelan.desktop.compose.state.steganography

import com.shterneregen.securelan.stego.model.BmpCapacity
import java.nio.charset.StandardCharsets

/**
 * In-memory tool draft and result with derived readiness hints; services perform actual BMP validation.
 * Contains sensitive message and password drafts and must not be persisted or written to diagnostics.
 */
data class ComposeSteganographyState(
    val busy: Boolean = false,
    val coverPathText: String = "",
    val inputPathText: String = "",
    val outputPathText: String = "",
    val messageDraft: String = "",
    val passwordDraft: String = "",
    val encryptPayload: Boolean = false,
    val encryptedExtract: Boolean = false,
    val capacity: BmpCapacity? = null,
    val extractedMessage: String = "",
    val statusText: String = "Choose a BMP cover or input image to start.",
) {
    val title: String = "Steganography"
    val coverPath = coverPathText.trim()
    val inputPath = inputPathText.trim()
    val outputPath = outputPathText.trim()
    val hasCover: Boolean = coverPath.isNotEmpty()
    val hasInput: Boolean = inputPath.isNotEmpty()
    val hasOutput: Boolean = outputPath.isNotEmpty()
    val hasMessage: Boolean = messageDraft.trim().isNotEmpty()
    val messageByteCount: Int = messageDraft.toByteArray(StandardCharsets.UTF_8).size
    val messageCapacityBytes: Int? = capacity?.payloadCapacityBytes
    val messageFitsCapacity: Boolean = messageCapacityBytes?.let { messageByteCount <= it } ?: true
    val messageCapacityLabel: String = messageCapacityBytes?.let { "$messageByteCount / $it bytes" }
        ?: "$messageByteCount bytes · choose a cover to check capacity"
    val passwordRequiredForHide: Boolean = encryptPayload
    val passwordRequiredForExtract: Boolean = encryptedExtract
    val passwordReady: Boolean = passwordDraft.isNotEmpty()
    val canHideMessage: Boolean = !busy && hasCover && hasOutput && hasMessage && messageFitsCapacity &&
        (!passwordRequiredForHide || passwordReady)
    val canExtractMessage: Boolean = !busy && hasInput && (!passwordRequiredForExtract || passwordReady)
    val extractedSummary: String = if (extractedMessage.isBlank()) "No extracted message yet." else "Extracted ${extractedMessage.length} characters."
    val blockedReasons: List<String> = buildList {
        if (!hasCover) add("Choose a cover image before hiding or inspecting capacity.")
        if (!hasOutput) add("Choose an output BMP path before hiding a message.")
        if (!hasMessage) add("Enter non-empty text before hiding a message.")
        if (!messageFitsCapacity) add("The message is larger than the selected image capacity.")
        if (!hasInput) add("Choose an input BMP before extracting a message.")
        if ((passwordRequiredForHide || passwordRequiredForExtract) && !passwordReady) add("Enter a password for encrypted steganography workflows.")
    }
}
