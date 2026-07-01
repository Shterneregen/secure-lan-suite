package com.shterneregen.securelan.desktop.compose.util

import java.awt.Dialog
import java.awt.FileDialog
import java.awt.Frame
import java.awt.KeyboardFocusManager
import java.io.FilenameFilter
import java.nio.file.Path

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
    val dialog = createNativeFileDialog(title, save).apply {
        filter?.let { chooserFilter ->
            filenameFilter = java.io.FilenameFilter { _, name -> chooserFilter.accepts(name) }
        }
        initialFile?.toAbsolutePath()?.normalize()?.let { path ->
            directory = path.parent?.toString()
            file = path.fileName?.toString()
        }
        isMultipleMode = false
    }

    return try {
        dialog.isVisible = true
        dialog.files.firstOrNull()?.toPath()?.toAbsolutePath()?.normalize()
    } finally {
        dialog.dispose()
    }
}

internal fun createNativeFileDialog(title: String, save: Boolean): FileDialog {
    val parentWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().activeWindow
    val mode = if (save) FileDialog.SAVE else FileDialog.LOAD
    return when (parentWindow) {
        is Frame -> FileDialog(parentWindow, title, mode)
        is Dialog -> FileDialog(parentWindow, title, mode)
        else -> FileDialog(null as Frame?, title, mode)
    }
}

internal fun ComposeFileChooserFilter.accepts(fileName: String): Boolean {
    if (extensions.isEmpty()) {
        return true
    }
    val extension = fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase()
    return extension.isNotBlank() && extensions.any { it.lowercase() == extension }
}
