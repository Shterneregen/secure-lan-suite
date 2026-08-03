package com.shterneregen.securelan.desktop.compose.ui.steganography

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.shterneregen.securelan.chat.discovery.DiscoveredPeer
import com.shterneregen.securelan.desktop.compose.ComposeDesktopHostAdapter
import com.shterneregen.securelan.desktop.compose.state.steganography.ComposeSteganographyMode
import com.shterneregen.securelan.desktop.compose.util.ComposeBmpFiles
import com.shterneregen.securelan.desktop.compose.util.ComposeImageFiles
import com.shterneregen.securelan.desktop.compose.util.ComposeTextFiles
import com.shterneregen.securelan.desktop.compose.util.copyToSystemClipboard
import com.shterneregen.securelan.desktop.compose.util.openComposeFileChooser
import com.shterneregen.securelan.desktop.compose.util.openInFileManager
import com.shterneregen.securelan.desktop.compose.util.pasteImageFromSystemClipboard
import com.shterneregen.securelan.desktop.ui.DesktopMainViewHelpers
import java.nio.file.Files
import java.nio.file.Path

@Composable
internal fun LiveSteganographyCard(
    hostAdapter: ComposeDesktopHostAdapter,
    initialMode: ComposeSteganographyMode,
    recipient: DiscoveredPeer?,
) {
    var mode by remember(initialMode) { mutableStateOf(initialMode) }
    var coverPath by remember { mutableStateOf(hostAdapter.stegoState.coverPathText) }
    var inputPath by remember { mutableStateOf(hostAdapter.stegoState.inputPathText) }
    var outputPath by remember { mutableStateOf(hostAdapter.stegoState.outputPathText) }
    var message by remember { mutableStateOf(hostAdapter.stegoState.messageDraft) }
    var hidePassword by remember { mutableStateOf("") }
    var extractPassword by remember { mutableStateOf("") }
    var encrypt by remember { mutableStateOf(false) }
    var encryptedExtract by remember { mutableStateOf(false) }
    var savedOutput by remember { mutableStateOf<Path?>(null) }
    var localStatus by remember { mutableStateOf<String?>(null) }

    val stegoState = hostAdapter.stegoState.copy(
        coverPathText = coverPath,
        inputPathText = inputPath,
        outputPathText = outputPath,
        messageDraft = message,
        passwordDraft = if (mode == ComposeSteganographyMode.HIDE) hidePassword else extractPassword,
        encryptPayload = encrypt,
        encryptedExtract = encryptedExtract,
        statusText = localStatus ?: hostAdapter.stegoState.statusText,
    )

    fun selectCover(path: Path) {
        val normalized = path.toAbsolutePath().normalize()
        coverPath = normalized.toString()
        outputPath = DesktopMainViewHelpers.suggestedStegoOutputPath(normalized).toString()
        savedOutput = null
        localStatus = null
        hostAdapter.inspectStegoCover(normalized)
    }

    fun selectInput(path: Path) {
        inputPath = path.toAbsolutePath().normalize().toString()
        localStatus = null
    }

    fun pasteImage(onSelected: (Path) -> Unit) {
        val pasted = pasteImageFromSystemClipboard()
        if (pasted == null) {
            localStatus = "No supported image was found in the clipboard."
        } else {
            onSelected(pasted)
        }
    }

    SteganographyCardContent(
        state = stegoState,
        mode = mode,
        onModeChange = {
            mode = it
            localStatus = null
        },
        coverPath = coverPath,
        onChooseCover = {
            openComposeFileChooser("Choose cover image", ComposeImageFiles)?.let(::selectCover)
        },
        onPasteCover = { pasteImage(::selectCover) },
        onCoverSelected = ::selectCover,
        inputPath = inputPath,
        onChooseInput = {
            openComposeFileChooser("Choose image with hidden message", ComposeImageFiles)?.let(::selectInput)
        },
        onPasteInput = { pasteImage(::selectInput) },
        onInputSelected = ::selectInput,
        outputPath = outputPath,
        onOutputPathChange = {
            outputPath = it
            savedOutput = null
        },
        onChooseOutput = {
            val initial = coverPath.trim().takeIf(String::isNotEmpty)
                ?.let { DesktopMainViewHelpers.suggestedStegoOutputPath(Path.of(it)) }
            openComposeFileChooser(
                "Save stego BMP image",
                ComposeBmpFiles,
                save = true,
                initialFile = initial,
            )?.let { path ->
                outputPath = DesktopMainViewHelpers.ensureBmpExtension(path.toAbsolutePath().normalize()).toString()
                savedOutput = null
            }
        },
        message = message,
        onMessageChange = {
            message = it
            savedOutput = null
        },
        hidePassword = hidePassword,
        onHidePasswordChange = { hidePassword = it },
        extractPassword = extractPassword,
        onExtractPasswordChange = { extractPassword = it },
        encrypt = encrypt,
        onEncryptChange = { encrypt = it },
        encryptedExtract = encryptedExtract,
        onEncryptedExtractChange = { encryptedExtract = it },
        onHide = {
            val output = outputPath.ifBlank {
                coverPath.trim().takeIf(String::isNotEmpty)
                    ?.let(Path::of)
                    ?.let(DesktopMainViewHelpers::suggestedStegoOutputPath)
                    ?.toString()
                    .orEmpty()
            }
            if (coverPath.isNotBlank() && output.isNotBlank()) {
                localStatus = null
                savedOutput = hostAdapter.hideStegoMessage(
                    Path.of(coverPath),
                    Path.of(output),
                    message,
                    hidePassword.takeIf { encrypt },
                )
                savedOutput?.let {
                    outputPath = it.toString()
                    inputPath = it.toString()
                }
            }
        },
        savedOutput = savedOutput,
        onOpenOutputFolder = { savedOutput?.let(::openInFileManager) },
        onSendOutput = recipient?.let { target ->
            {
                savedOutput?.let { output ->
                    hostAdapter.sendFileToPeer(
                        output,
                        hostAdapter.statusState.nickname,
                        target,
                        hostAdapter.currentRoomPassword,
                    )
                    localStatus = "Sending ${output.fileName} to ${target.nickname}."
                }
            }
        },
        sendOutputLabel = recipient?.let { "Send to ${it.nickname}" },
        onExtract = {
            inputPath.trim().takeIf(String::isNotEmpty)?.let { path ->
                localStatus = null
                hostAdapter.extractStegoMessage(
                    Path.of(path),
                    extractPassword.takeIf { encryptedExtract },
                )
            }
        },
        onCopyResult = {
            copyToSystemClipboard(hostAdapter.stegoState.extractedMessage)
            localStatus = "Extracted message copied to the clipboard."
        },
        onSaveResult = {
            val selected = openComposeFileChooser(
                "Save extracted message",
                ComposeTextFiles,
                save = true,
                initialFile = Path.of("extracted-message.txt").toAbsolutePath().normalize(),
            )
            if (selected != null) {
                val output = if (selected.toString().endsWith(".txt", ignoreCase = true)) {
                    selected
                } else {
                    Path.of("$selected.txt")
                }.toAbsolutePath().normalize()
                runCatching { Files.writeString(output, hostAdapter.stegoState.extractedMessage) }
                    .onSuccess { localStatus = "Extracted message saved to ${output.fileName}." }
                    .onFailure { localStatus = "Could not save extracted text: ${it.message ?: "unknown error"}" }
            }
        },
        onClear = {
            coverPath = ""
            inputPath = ""
            outputPath = ""
            message = ""
            hidePassword = ""
            extractPassword = ""
            encrypt = false
            encryptedExtract = false
            savedOutput = null
            localStatus = null
            hostAdapter.clearSteganographyState()
        },
    )
}
