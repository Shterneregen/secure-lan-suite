package com.shterneregen.securelan.desktop.compose.settings

import java.nio.file.InvalidPathException
import java.nio.file.Path

/**
 * Resolves portable data locations from an explicit override, a jpackage launcher, or the working directory.
 * Resolution does not create directories or verify write access; the owning store or service does that.
 */
object DesktopAppPaths {
    /** System property overriding the base directory for portable application data. */
    const val APP_DIRECTORY_PROPERTY: String = "securelan.app.dir"
    /** Launcher path supplied by jpackage; its parent is the portable application directory. */
    const val JPACKAGE_APP_PATH_PROPERTY: String = "jpackage.app-path"
    /** Optional system property overriding only the log directory. */
    const val LOGS_DIRECTORY_PROPERTY: String = "securelan.logs.dir"

    /** Returns the normalized absolute data directory using the configured precedence. */
    fun applicationDirectory(): Path = resolveApplicationDirectory(System::getProperty)

    /** Returns the properties-file location under the portable config directory. */
    fun settingsPath(): Path = applicationDirectory().resolve("config").resolve("settings.properties")

    /** Returns the default received-file directory under the portable data directory. */
    fun downloadsDirectory(): Path = downloadsDirectory(applicationDirectory())

    internal fun downloadsDirectory(applicationDirectory: Path): Path =
        applicationDirectory.toAbsolutePath().normalize().resolve("downloads")

    internal fun isLegacyDefaultDownloadsDirectory(
        value: String,
        userHome: String = System.getProperty("user.home", "."),
    ): Boolean = runCatching {
        Path.of(value).toAbsolutePath().normalize() ==
            Path.of(userHome, "Downloads", "SecureLanSuite").toAbsolutePath().normalize()
    }.getOrDefault(false)

    /** Uses a valid log override, otherwise the logs directory under the portable data directory. */
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
