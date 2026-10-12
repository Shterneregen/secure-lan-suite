package com.shterneregen.securelan.desktop.compose.util

import java.awt.KeyboardFocusManager
import java.nio.file.Path
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

internal data class ComposeFileChooserFilter(
    val description: String,
    val extensions: List<String>,
)

internal val ComposeImageFiles = ComposeFileChooserFilter("Image files", listOf("bmp", "png", "jpg", "jpeg", "gif"))
internal val ComposeBmpFiles = ComposeFileChooserFilter("BMP images", listOf("bmp"))
internal val ComposeTextFiles = ComposeFileChooserFilter("Text files", listOf("txt"))

/**
 * Opens a modal Swing file picker anchored to the active window and returns a normalized selection.
 * Call on the Swing event-dispatch thread; cancellation returns `null` and leaves the caller's draft intact.
 */
internal fun openComposeFileChooser(
    title: String,
    filter: ComposeFileChooserFilter? = null,
    save: Boolean = false,
    initialFile: Path? = null,
): Path? {
    val chooser = JFileChooser(initialFile?.toFile()?.parentFile).apply {
        dialogTitle = title
        dialogType = if (save) JFileChooser.SAVE_DIALOG else JFileChooser.OPEN_DIALOG
        fileSelectionMode = JFileChooser.FILES_ONLY
        isAcceptAllFileFilterUsed = filter == null
        filter?.let { chooserFilter ->
            fileFilter = FileNameExtensionFilter(chooserFilter.description, *chooserFilter.extensions.toTypedArray())
        }
        initialFile?.toAbsolutePath()?.normalize()?.let { path ->
            currentDirectory = path.parent?.toFile()
            selectedFile = path.toFile()
        }
        isMultiSelectionEnabled = false
    }
    val parent = KeyboardFocusManager.getCurrentKeyboardFocusManager().activeWindow
    val result = if (save) chooser.showSaveDialog(parent) else chooser.showOpenDialog(parent)
    return chooser.selectedFile?.toPath()?.toAbsolutePath()?.normalize()
        ?.takeIf { result == JFileChooser.APPROVE_OPTION }
}

/** Transfer-specific multi-file picker. Other tools keep the single-file [openComposeFileChooser]. */
internal fun openComposeTransferFileChooser(title: String): List<Path> {
    val chooser = JFileChooser().apply {
        dialogTitle = title
        dialogType = JFileChooser.OPEN_DIALOG
        fileSelectionMode = JFileChooser.FILES_ONLY
        isAcceptAllFileFilterUsed = true
        isMultiSelectionEnabled = true
    }
    val parent = KeyboardFocusManager.getCurrentKeyboardFocusManager().activeWindow
    if (chooser.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) {
        return emptyList()
    }
    return chooser.selectedFiles.map { it.toPath().toAbsolutePath().normalize() }
}

/** Opens a modal directory picker on the Swing event-dispatch thread; cancellation returns `null`. */
internal fun openComposeDirectoryChooser(title: String, initialDirectory: Path? = null): Path? {
    val chooser = JFileChooser(initialDirectory?.toFile()).apply {
        dialogTitle = title
        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        isAcceptAllFileFilterUsed = false
    }
    val parent = KeyboardFocusManager.getCurrentKeyboardFocusManager().activeWindow
    return if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
        chooser.selectedFile?.toPath()?.toAbsolutePath()?.normalize()
    } else {
        null
    }
}
