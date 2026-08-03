package com.shterneregen.securelan.stego

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class BmpStegoInspectionServiceTest {
    private val service = StegoServices.createDefault().inspectionService()

    @Test
    fun `calculates average entropy and last-bit preview`() {
        val bmp = createBmp2x1(byteArrayOf(0, 0, 0, 1, 1, 1))
        val result = service.inspect(bmp, interval = 1)

        assertEquals(listOf(0.0, 7.0), result.movingAverage.map { it.value })
        assertEquals(listOf(0.0, 0.0), result.entropy.map { it.value })
        assertEquals(0xff000000.toInt(), result.lastBitArgb[0])
        assertEquals(0xffffffff.toInt(), result.lastBitArgb[1])
    }

    @Test
    fun `validates analysis range`() {
        val bmp = createBmp2x1(ByteArray(6))
        assertThrows(IllegalArgumentException::class.java) { service.inspect(bmp, intervalStart = 2) }
        assertThrows(IllegalArgumentException::class.java) { service.inspect(bmp, interval = 0) }
    }

    private fun createBmp2x1(channels: ByteArray): ByteArray {
        val bytes = ByteArray(62)
        val header = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        bytes[0] = 'B'.code.toByte(); bytes[1] = 'M'.code.toByte()
        header.putInt(2, bytes.size); header.putInt(10, 54); header.putInt(14, 40)
        header.putInt(18, 2); header.putInt(22, -1); header.putShort(26, 1); header.putShort(28, 24)
        channels.copyInto(bytes, 54)
        return bytes
    }
}
