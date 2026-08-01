package com.shterneregen.securelan.desktop.compose.settings

import java.util.concurrent.CompletableFuture
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine
import kotlin.math.PI
import kotlin.math.sin

/** Plays a short, best-effort application notification without blocking the UI thread. */
internal object DesktopNotificationSound {
    fun play(volumePercent: Int) {
        val volume = volumePercent.coerceIn(0, 100)
        if (volume == 0) return
        CompletableFuture.runAsync { runCatching { playTone(volume) } }
    }

    private fun playTone(volumePercent: Int) {
        val format = AudioFormat(SAMPLE_RATE.toFloat(), 8, 1, true, false)
        val line = AudioSystem.getLine(DataLine.Info(SourceDataLine::class.java, format)) as SourceDataLine
        val samples = ByteArray(SAMPLE_COUNT) { index ->
            val envelope = 1.0 - index.toDouble() / SAMPLE_COUNT
            val wave = sin(2.0 * PI * FREQUENCY_HZ * index / SAMPLE_RATE)
            (wave * envelope * Byte.MAX_VALUE * volumePercent / 100.0).toInt().toByte()
        }
        try {
            line.open(format)
            line.start()
            line.write(samples, 0, samples.size)
            line.drain()
        } finally {
            line.stop()
            line.close()
        }
    }

    private const val SAMPLE_RATE = 8_000
    private const val FREQUENCY_HZ = 660
    private const val SAMPLE_COUNT = 720
}
