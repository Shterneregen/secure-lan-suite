package com.shterneregen.securelan.desktop.compose.settings

import java.nio.file.InvalidPathException
import java.nio.file.Path

/** Resolves writable portable data directories next to the packaged application launcher. */
object DesktopAppPaths {
    const val APP_DIRECTORY_PROPERTY: String = "securelan.app.dir"
    const val JPACKAGE_APP_PATH_PROPERTY: String = "jpackage.app-path"
    const val LOGS_DIRECTORY_PROPERTY: String = "securelan.logs.dir"

    fun applicationDirectory(): Path = resolveApplicationDirectory(System::getProperty)

    fun settingsPath(): Path = applicationDirectory().resolve("config").resolve("settings.properties")

    fun logsDirectory(): Path {
        val configured = validPath(System.getProperty(LOGS_DIRECTORY_PROPERTY))
        return configured ?: applicationDirectory().resolve("logs")
    }

    internal fun resolveApplicationDirectory(property: (String) -> String?): Path {
        validPath(property(APP_DIRECTORY_PROPERTY))?.let { return it }

        validPath(property(JPACKAGE_APP_PATH_PROPERTY))?.parent?.let { return it }

        return validPath(property("user.dir")) ?: Path.of(".").toAbsolutePath().normalize()
    }

    private fun validPath(value: String?): Path? {
        val normalizedValue = value?.trim()?.takeIf(String::isNotEmpty) ?: return null
        return try {
            Path.of(normalizedValue).toAbsolutePath().normalize()
        } catch (_: InvalidPathException) {
            null
        }
    }
}
