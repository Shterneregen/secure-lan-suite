package com.shterneregen.securelan.androidclient.transfer

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class TransferStreamsTest {
    @Test
    fun `accepts an exact completed length`() {
        validateTransferLength(expected = 10, actual = 10, complete = true)
    }

    @Test
    fun `accepts partial progress before completion`() {
        validateTransferLength(expected = 10, actual = 4, complete = false)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects an incomplete stream`() {
        validateTransferLength(expected = 10, actual = 5, complete = true)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects more bytes than declared`() {
        validateTransferLength(expected = 10, actual = 11, complete = false)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects negative lengths`() {
        validateTransferLength(expected = -1, actual = 0, complete = false)
    }

    @Test
    fun `legacy zero expected length means unknown and is not enforced`() {
        validateTransferLength(expected = 0, actual = 7, complete = true, legacyUnknown = true)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `zero expected length is enforced without the legacy flag`() {
        validateTransferLength(expected = 0, actual = 7, complete = true)
    }

    @Test
    fun `stageStream computes the exact staged length without buffering the whole file`() {
        val data = ByteArray(40_000) { index -> index.toByte() }
        val output = ByteArrayOutputStream()

        val length = stageStream(ByteArrayInputStream(data), output) { }

        assertEquals(40_000L, length)
        assertArrayEquals(data, output.toByteArray())
    }

    @Test
    fun `duplicate number is inserted before the final extension`() {
        assertEquals("photo-1.jpg", numberedTransferFileName("photo.jpg", 1))
        assertEquals("archive.tar-2.gz", numberedTransferFileName("archive.tar.gz", 2))
        assertEquals("README-3", numberedTransferFileName("README", 3))
        assertEquals(".nomedia-4", numberedTransferFileName(".nomedia", 4))
    }
}
