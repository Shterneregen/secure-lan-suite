package com.shterneregen.securelan.desktop.compose.util

import java.awt.KeyboardFocusManager
import java.nio.file.Path
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

internal data class ComposeFileChooserFilter(
    val description: String,
    val extensions: Array<String>,
)

internal val ComposeImageFiles = ComposeFileChooserFilter("Image files", arrayOf("bmp", "png", "jpg", "jpeg", "gif"))
internal val ComposeBmpFiles = ComposeFileChooserFilter("BMP images", arrayOf("bmp"))
internal val ComposeTextFiles = ComposeFileChooserFilter("Text files", arrayOf("txt"))

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
            fileFilter = FileNameExtensionFilter(chooserFilter.description, *chooserFilter.extensions)
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

internal fun ComposeFileChooserFilter.accepts(fileName: String): Boolean {
    if (extensions.isEmpty()) {
        return true
    }
    val extension = fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase()
    return extension.isNotBlank() && extensions.any { it.lowercase() == extension }
}
