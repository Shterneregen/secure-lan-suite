package com.shterneregen.securelan.desktop.compose.logging

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.logging.*

/**
 * File logger for SecureLanSuite desktop application events.
 *
 * Replaces the on-screen Diagnostics panel. Runtime events are written to
 * [user-home]/.securelan/logs/securelan-%g.log and rotated so the latest
 * files are always available for troubleshooting.
 */
object SecureLanLogger {
    private const val MAX_LOG_BYTES: Int = 2 * 1024 * 1024
    private const val MAX_LOG_FILES: Int = 3
    private const val LOG_DIRECTORY: String = ".securelan/logs"
    private const val LOG_FILE_PATTERN: String = "securelan-%g.log"

    private val logger: Logger = Logger.getLogger("SecureLanSuite").apply {
        level = Level.INFO
        useParentHandlers = false
        addHandler(createFileHandler())
    }

    private fun createFileHandler(): Handler {
        val logDir = resolveLogDirectory()
        Files.createDirectories(logDir)
        val pattern = logDir.resolve(LOG_FILE_PATTERN).toString()
        return FileHandler(pattern, MAX_LOG_BYTES, MAX_LOG_FILES, true).apply {
            level = Level.INFO
            formatter = SecureLanLogFormatter()
        }
    }

    private fun resolveLogDirectory(): Path {
        val userHome = System.getProperty("user.home")
        return Paths.get(userHome, LOG_DIRECTORY)
    }

    fun logConnection(message: String) = log("CONNECTION", message)
    fun logTransfer(message: String) = log("TRANSFER", message)
    fun logQuickShare(message: String) = log("QUICKSHARE", message)
    fun logRealtime(message: String) = log("REALTIME", message)
    fun logWarning(message: String) = log("WARNING", message)
    fun logError(message: String, throwable: Throwable? = null) = log("ERROR", message, throwable)

    private fun log(tag: String, message: String, throwable: Throwable? = null) {
        val formatted = "[$tag] $message"
        if (throwable != null) {
            logger.log(Level.SEVERE, formatted, throwable)
        } else if (tag == "ERROR") {
            logger.log(Level.SEVERE, formatted)
        } else if (tag == "WARNING") {
            logger.log(Level.WARNING, formatted)
        } else {
            logger.log(Level.INFO, formatted)
        }
    }

    private class SecureLanLogFormatter : Formatter() {
        private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

        override fun format(record: LogRecord): String {
            val timestamp = LocalDateTime.now().format(formatter)
            val message = formatMessage(record)
            return if (record.thrown != null) {
                val stack = record.thrown.stackTraceToString()
                "[$timestamp] ${record.level}: $message\n$stack\n"
            } else {
                "[$timestamp] ${record.level}: $message\n"
            }
        }
    }
}
