package com.shterneregen.securelan.webrtc.runtime.video

import com.shterneregen.securelan.webrtc.event.RtcEvent
import com.shterneregen.securelan.webrtc.event.RtcRuntimeWarningEvent
import com.shterneregen.securelan.webrtc.runtime.RtcFileLogger
import java.util.Objects
import java.util.function.Consumer

/**
 * Diagnostic utility for video capture and preview pipelines.
 *
 * Dispatches diagnostic messages to standard console output, the persistent RTC log file,
 * and higher-level UI listeners via an [eventConsumer].
 *
 * @param eventConsumer sink receiving [RtcRuntimeWarningEvent] instances for user/UI visibility.
 */
class RtcVideoDiagnostics(eventConsumer: Consumer<RtcEvent>) {
    private val eventConsumer: Consumer<RtcEvent> = Objects.requireNonNull(eventConsumer, "eventConsumer must not be null")

    /**
     * Records a non-fatal warning condition to stderr, log file, and event consumer.
     *
     * @param message warning message text.
     */
    fun warn(message: String) {
        consoleError(message)
        RtcFileLogger.warn(message)
        eventConsumer.accept(RtcRuntimeWarningEvent(message))
    }

    /**
     * Records an informational diagnostic event to stdout and event consumer.
     *
     * @param message diagnostic detail string.
     */
    fun diag(message: String) {
        val diagnosticMessage = "[diag] $message"
        consoleInfo(diagnosticMessage)
        eventConsumer.accept(RtcRuntimeWarningEvent(diagnosticMessage))
    }

    /**
     * Records an error condition with an exception to stderr and log file.
     *
     * @param message error description.
     * @param error throwable associated with the error, or null.
     */
    fun error(message: String, error: Throwable?) {
        consoleError(message, error)
        RtcFileLogger.error(message, error)
    }

    companion object {
        private fun consoleInfo(message: String) {
            println("[rtc] $message")
        }

        private fun consoleError(message: String) {
            System.err.println("[rtc] $message")
        }

        private fun consoleError(message: String, error: Throwable?) {
            consoleError(message)
            error?.printStackTrace(System.err)
        }
    }
}
