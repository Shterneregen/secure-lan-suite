package com.shterneregen.securelan.androidclient.protocol

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.charset.StandardCharsets

class CryptoCompatTest {
    @Test
    fun aesPayloadRoundTripsWithEmbeddedIvFormat() {
        val key = CryptoCompat.generateAesKey()
        val plainText = "secure payload".toByteArray(StandardCharsets.UTF_8)

        val encrypted = CryptoCompat.aesEncrypt(plainText, key)
        val decrypted = CryptoCompat.aesDecrypt(encrypted, key)

        assertEquals(12, encrypted[0].toInt())
        assertArrayEquals(plainText, decrypted)
    }
}
