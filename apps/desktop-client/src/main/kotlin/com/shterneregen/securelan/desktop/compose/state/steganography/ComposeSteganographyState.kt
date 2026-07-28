package com.shterneregen.securelan.desktop.compose.state.steganography

import com.shterneregen.securelan.desktop.ui.DesktopMainViewHelpers
import com.shterneregen.securelan.stego.model.BmpCapacity
import java.nio.charset.StandardCharsets

public data class ComposeSteganographyState(
    val coverPathText: String = "",
    val inputPathText: String = "",
    val outputPathText: String = "",
    val messageDraft: String = "Hidden Compose message",
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
    val capacityText: String = capacity?.let(DesktopMainViewHelpers::formatStegoCapacity) ?: "Capacity unavailable until a BMP is inspected."
    val canInspectCover: Boolean = hasCover
    val canHideMessage: Boolean = hasCover && hasOutput && hasMessage && messageFitsCapacity &&
        (!passwordRequiredForHide || passwordReady)
    val canExtractMessage: Boolean = hasInput && (!passwordRequiredForExtract || passwordReady)
    val hideLabel: String = if (canHideMessage) "Hide message ready" else "Hide message blocked"
    val extractLabel: String = if (canExtractMessage) "Extract ready" else "Extract blocked"
    val extractedSummary: String = if (extractedMessage.isBlank()) "No extracted message yet." else "Extracted ${extractedMessage.length} characters."
    val extractedEmptyHint: String = "Choose a BMP input image and press Extract to reveal a hidden message."
    val blockedReasons: List<String> = buildList {
        if (!hasCover) add("Choose a cover image before hiding or inspecting capacity.")
        if (!hasOutput) add("Choose an output BMP path before hiding a message.")
        if (!hasMessage) add("Enter non-empty text before hiding a message.")
        if (!messageFitsCapacity) add("The message is larger than the selected image capacity.")
        if (!hasInput) add("Choose an input BMP before extracting a message.")
        if ((passwordRequiredForHide || passwordRequiredForExtract) && !passwordReady) add("Enter a password for encrypted steganography workflows.")
    }
    val readinessSummary: String = if (blockedReasons.isEmpty()) {
        "Steganography controls are ready for BMP hide/extract workflows."
    } else {
        blockedReasons.joinToString(" · ")
    }
}
