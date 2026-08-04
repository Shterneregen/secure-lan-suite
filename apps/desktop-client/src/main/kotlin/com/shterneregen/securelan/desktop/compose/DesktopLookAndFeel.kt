package com.shterneregen.securelan.desktop.compose

import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.FlatLaf
import com.formdev.flatlaf.FlatLightLaf
import java.awt.Color
import java.awt.GraphicsEnvironment
import javax.swing.JFrame
import javax.swing.SwingUtilities
import javax.swing.UIManager

/** Keeps Swing dialogs and the client-decorated desktop title bar aligned with the Compose theme. */
internal object DesktopLookAndFeel {
    @Volatile
    private var installedMode: SecureLanThemeMode? = null

    fun install(mode: SecureLanThemeMode) {
        if (GraphicsEnvironment.isHeadless()) return
        runOnEventDispatchThread {
            JFrame.setDefaultLookAndFeelDecorated(true)
            FlatLaf.setUseNativeWindowDecorations(true)
            applyMode(mode, updateExistingWindows = false)
        }
    }

    fun update(mode: SecureLanThemeMode) {
        if (GraphicsEnvironment.isHeadless() || installedMode == mode) return
        runOnEventDispatchThread { applyMode(mode, updateExistingWindows = true) }
    }

    private fun applyMode(mode: SecureLanThemeMode, updateExistingWindows: Boolean) {
        when (mode) {
            SecureLanThemeMode.LIGHT -> FlatLightLaf.setup()
            SecureLanThemeMode.DARK, SecureLanThemeMode.INTERMEDIATE -> FlatDarkLaf.setup()
        }

        val palette = DesktopPalette.forMode(mode)
        UIManager.put("Component.arc", 12)
        UIManager.put("Button.arc", 12)
        UIManager.put("TextComponent.arc", 10)
        UIManager.put("TitlePane.background", palette.surface)
        UIManager.put("TitlePane.inactiveBackground", palette.surface)
        UIManager.put("TitlePane.foreground", palette.text)
        UIManager.put("TitlePane.inactiveForeground", palette.secondaryText)
        UIManager.put("Panel.background", palette.surface)
        UIManager.put("FileChooser.background", palette.surface)
        // JDK's Windows shell integration may expose virtual folders as CLSID strings.
        // FlatLaf's shortcuts panel then asks FileSystemView to resolve them as files,
        // producing a noisy FileNotFoundException warning. The regular directory tree,
        // address combo, navigation buttons, and themed chooser remain available.
        UIManager.put("FileChooser.noPlacesBar", isWindows())
        UIManager.put("List.background", palette.surfaceRaised)
        UIManager.put("List.foreground", palette.text)
        UIManager.put("List.selectionBackground", palette.accent)
        UIManager.put("List.selectionForeground", Color.WHITE)
        UIManager.put("TextField.background", palette.surfaceRaised)
        UIManager.put("TextField.foreground", palette.text)
        UIManager.put("TextField.caretForeground", palette.accent)
        UIManager.put("Button.background", palette.surfaceRaised)
        UIManager.put("Button.foreground", palette.text)
        UIManager.put("Component.focusColor", palette.accent)
        installedMode = mode

        if (updateExistingWindows) FlatLaf.updateUI()
    }

    private fun runOnEventDispatchThread(block: () -> Unit) {
        if (SwingUtilities.isEventDispatchThread()) block() else SwingUtilities.invokeAndWait(block)
    }

    private fun isWindows(): Boolean =
        System.getProperty("os.name", "").startsWith("Windows", ignoreCase = true)
}

private data class DesktopPalette(
    val surface: Color,
    val surfaceRaised: Color,
    val accent: Color,
    val text: Color,
    val secondaryText: Color,
) {
    companion object {
        fun forMode(mode: SecureLanThemeMode): DesktopPalette = when (mode) {
            SecureLanThemeMode.DARK -> DesktopPalette(
                surface = Color(0x10, 0x18, 0x24),
                surfaceRaised = Color(0x16, 0x22, 0x33),
                accent = Color(0x3B, 0x82, 0xF6),
                text = Color(0xF0, 0xF6, 0xFF),
                secondaryText = Color(0xB7, 0xC4, 0xD8),
            )
            SecureLanThemeMode.INTERMEDIATE -> DesktopPalette(
                surface = Color(0x29, 0x32, 0x41),
                surfaceRaised = Color(0x33, 0x3E, 0x50),
                accent = Color(0x4B, 0x8B, 0xF5),
                text = Color(0xF3, 0xF6, 0xFA),
                secondaryText = Color(0xC2, 0xCB, 0xD8),
            )
            SecureLanThemeMode.LIGHT -> DesktopPalette(
                surface = Color(0xFF, 0xFF, 0xFF),
                surfaceRaised = Color(0xEF, 0xF4, 0xFA),
                accent = Color(0x25, 0x63, 0xEB),
                text = Color(0x17, 0x20, 0x33),
                secondaryText = Color(0x47, 0x55, 0x69),
            )
        }
    }
}
