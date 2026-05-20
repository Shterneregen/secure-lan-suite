package com.shterneregen.securelan.webrtc.runtime.video

import com.shterneregen.securelan.webrtc.event.RtcEvent
import com.shterneregen.securelan.webrtc.event.RtcRuntimeWarningEvent
import com.shterneregen.securelan.webrtc.runtime.RtcFileLogger
import java.util.Objects
import java.util.function.Consumer

class RtcVideoDiagnostics(eventConsumer: Consumer<RtcEvent>) {
    private val eventConsumer: Consumer<RtcEvent> = Objects.requireNonNull(eventConsumer, "eventConsumer must not be null")

    fun warn(message: String) {
        consoleError(message)
        RtcFileLogger.warn(message)
        eventConsumer.accept(RtcRuntimeWarningEvent(message))
    }

    fun diag(message: String) {
        val diagnosticMessage = "[diag] $message"
        consoleInfo(diagnosticMessage)
        eventConsumer.accept(RtcRuntimeWarningEvent(diagnosticMessage))
    }

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
