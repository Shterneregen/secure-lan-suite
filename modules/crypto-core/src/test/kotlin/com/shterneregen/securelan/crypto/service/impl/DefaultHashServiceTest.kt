package com.shterneregen.securelan.crypto.service.impl

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

class DefaultHashServiceTest {
    private val service = DefaultHashService()

    @Test
    fun emptyInputHasCanonicalLowercaseDigest() {
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            service.sha256Hex(byteArrayOf()),
        )
    }

    @Test
    fun abcInputPreservesLeadingZerosAndUnsignedBytes() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            service.sha256Hex("abc".toByteArray(StandardCharsets.UTF_8)),
        )
    }
}
