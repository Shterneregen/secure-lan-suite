package com.shterneregen.securelan.desktop.compose

import com.shterneregen.securelan.webrtc.event.RtcVideoFrameEvent
import com.shterneregen.securelan.webrtc.service.RtcMediaDeviceService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

internal data class PreviewUpdate(val running: Boolean, val status: String? = null, val frame: RtcVideoFrameEvent? = null)

/** Owns preview handles, native startup, generation and latest-frame delivery independently of rooms/tools. */
internal class DesktopMediaController(
    private val service: RtcMediaDeviceService?,
    private val dispatch: (() -> Unit) -> Unit,
    private val publish: (PreviewUpdate) -> Unit,
) : AutoCloseable {
    private val generation = AtomicLong()
    private val closed = AtomicBoolean()
    private val opening = AtomicBoolean()
    private val session = AtomicReference<RtcMediaDeviceService.CameraPreviewSession?>()
    private val worker = Executors.newSingleThreadExecutor { work -> Thread(work, "desktop-camera-preview").apply { isDaemon = true } }
    private val frames = LatestUiValue<Pair<Long, RtcVideoFrameEvent>>(dispatch) { (token, frame) ->
        if (!closed.get() && generation.get() == token) publish(PreviewUpdate(true, frame = frame))
    }

    fun start(deviceId: String?) {
        stop()
        if (closed.get()) return
        val token = generation.get()
        if (service == null) { publish(PreviewUpdate(false, "Camera preview unavailable.")); return }
        if (!opening.compareAndSet(false, true)) { publish(PreviewUpdate(false, "Camera is still closing. Retry preview shortly.")); return }
        publish(PreviewUpdate(true, "Starting camera preview…"))
        worker.execute {
            try {
                if (closed.get() || generation.get() != token) return@execute
                val created = service.startVideoPreview(deviceId) { frames.offer(token to it) }
                if (closed.get() || generation.get() != token) { created.close(); return@execute }
                session.set(created)
                // Stop may race with native startup before the handle was published.
                if (closed.get() || generation.get() != token) { session.compareAndSet(created, null); created.close(); return@execute }
                dispatch {
                    if (!closed.get() && generation.get() == token) {
                        val status = created.statusMessage()
                        val failed = status.startsWith("Camera preview failed", true)
                        if (failed) { session.compareAndSet(created, null); created.close(); generation.incrementAndGet(); frames.clear() }
                        publish(PreviewUpdate(!failed, status))
                    }
                }
            } catch (_: Exception) {
                dispatch { if (!closed.get() && generation.get() == token) publish(PreviewUpdate(false, "Camera preview failed. Check the camera and retry.")) }
            } finally { opening.set(false) }
        }
    }

    fun stop() {
        generation.incrementAndGet()
        frames.clear()
        session.getAndSet(null)?.let { runCatching { it.close() } }
        if (!closed.get()) publish(PreviewUpdate(false, "Camera preview stopped."))
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        stop()
        worker.shutdown()
        service?.close()
    }
}
