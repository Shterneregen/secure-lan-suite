package com.shterneregen.securelan.webrtc.runtime

import java.io.PrintWriter
import java.io.StringWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Instant

/**
 * Thread-safe file logger specifically dedicated to recording WebRTC lifecycle events,
 * native driver warnings, device initialization failures, and media diagnostics.
 *
 * Log messages are appended to `logs/rtc.log` by default, or to the path specified
 * by the `securelan.rtc.logFile` system property. Write failures are silently swallowed
 * to ensure file I/O errors never disrupt realtime media processing.
 */
object RtcFileLogger {
    private val lock = Any()
    private const val LOG_FILE_PROPERTY = "securelan.rtc.logFile"
    private val DEFAULT_LOG_FILE: Path = Path.of("logs", "rtc.log")

    /**
     * Records a warning message to the RTC log file.
     *
     * @param message warning text to log.
     */
    @JvmStatic
    fun warn(message: String?) {
        write("WARN", message, null)
    }

    /**
     * Records an error message to the RTC log file.
     *
     * @param message error text to log.
     */
    @JvmStatic
    fun error(message: String?) {
        write("ERROR", message, null)
    }

    /**
     * Records an error message along with an exception stack trace to the RTC log file.
     *
     * @param message error text to log.
     * @param error throwable whose stack trace should be included, or null.
     */
    @JvmStatic
    fun error(message: String?, error: Throwable?) {
        write("ERROR", message, error)
    }

    private fun write(level: String, message: String?, error: Throwable?) {
        synchronized(lock) {
            try {
                val logFile = logFile()
                val parent = logFile.parent
                if (parent != null) {
                    Files.createDirectories(parent)
                }

                Files.writeString(
                    logFile,
                    format(level, message, error),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND,
                )
            } catch (_: Throwable) {
                // Logging must never break realtime media flow.
            }
        }
    }

    private fun logFile(): Path {
        val configuredPath = System.getProperty(LOG_FILE_PROPERTY)
        if (configuredPath.isNullOrBlank()) {
            return DEFAULT_LOG_FILE
        }
        return Path.of(configuredPath.trim())
    }

    private fun format(level: String, message: String?, error: Throwable?): String {
        val builder = StringBuilder()
            .append(Instant.now())
            .append(' ')
            .append(level)
            .append(" [rtc] ")
            .append(message ?: "")
            .append(System.lineSeparator())

        if (error != null) {
            val buffer = StringWriter()
            error.printStackTrace(PrintWriter(buffer))
            builder.append(buffer)
        }

        return builder.toString()
    }
}
