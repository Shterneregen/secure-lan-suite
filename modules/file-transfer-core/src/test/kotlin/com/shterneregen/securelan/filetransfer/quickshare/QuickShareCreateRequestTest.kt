package com.shterneregen.securelan.filetransfer.quickshare

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.time.Duration

class QuickShareCreateRequestTest {
    @Test
    fun shouldCreateValidFileRequest() {
        val tempFile = Files.createTempFile("quick-req", ".txt")
        Files.writeString(tempFile, "sample")

        val request = QuickShareCreateRequest.file(tempFile, "Custom Label", Duration.ofMinutes(10), 5)

        assertEquals(QuickShareType.FILE, request.type())
        assertEquals("Custom Label", request.displayName())
        assertEquals(tempFile, request.file())
        assertEquals("", request.text())
        assertEquals(Duration.ofMinutes(10), request.expiresAfter())
        assertEquals(5, request.accessLimit())
    }

    @Test
    fun shouldDefaultFileNameWhenDisplayNameIsBlank() {
        val tempFile = Files.createTempFile("sample-doc", ".txt")

        val request = QuickShareCreateRequest.file(tempFile, "   ", null, null)

        assertEquals(tempFile.fileName.toString(), request.displayName())
        assertNull(request.expiresAfter())
        assertNull(request.accessLimit())
    }

    @Test
    fun shouldCreateValidTextRequest() {
        val request = QuickShareCreateRequest.text("WiFi password is 123", "WiFi", Duration.ofHours(1), 3)

        assertEquals(QuickShareType.TEXT, request.type())
        assertEquals("WiFi", request.displayName())
        assertEquals("WiFi password is 123", request.text())
        assertNull(request.file())
        assertEquals(Duration.ofHours(1), request.expiresAfter())
        assertEquals(3, request.accessLimit())
    }

    @Test
    fun shouldRejectInvalidLimitsAndDurations() {
        val tempFile = Files.createTempFile("test-limit", ".txt")

        assertThrows(IllegalArgumentException::class.java) {
            QuickShareCreateRequest.file(tempFile, "Test", Duration.ZERO, 1)
        }

        assertThrows(IllegalArgumentException::class.java) {
            QuickShareCreateRequest.file(tempFile, "Test", Duration.ofMinutes(-5), 1)
        }

        assertThrows(IllegalArgumentException::class.java) {
            QuickShareCreateRequest.file(tempFile, "Test", Duration.ofMinutes(5), 0)
        }

        assertThrows(IllegalArgumentException::class.java) {
            QuickShareCreateRequest.text("   ", "Test", null, null)
        }
    }
}
