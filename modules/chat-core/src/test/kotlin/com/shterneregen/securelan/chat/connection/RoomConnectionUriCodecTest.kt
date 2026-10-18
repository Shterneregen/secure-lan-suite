package com.shterneregen.securelan.chat.connection

import com.shterneregen.securelan.common.net.address.IpLiteral
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.random.Random

class RoomConnectionUriCodecTest {
    private val minimal = "securelan://join?v=1&proto=2&host=192.168.1.42&port=5050"
    @Test fun `canonical full unicode IPv6 round trip`() {
        val descriptor = RoomConnectionDescriptor(IpLiteral.parse("fd12::42")!!, 5050, 5051,
            "2da1e450-798a-401f-990b-cb1cc4f61d92", "ab817e84-78af-40b4-994d-41bfa5ff20fb", "Комната + &", listOf(IpLiteral.parse("192.168.1.42")!!))
        val encoded = RoomConnectionUriCodec.encode(descriptor)
        assertTrue("host=fd12%3A%3A42" in encoded)
        assertTrue("%20%2B%20%26" in encoded)
        assertEquals(descriptor, (RoomConnectionUriCodec.decode(encoded) as ConnectionUriParseResult.Success).descriptor)
        assertNull((RoomConnectionUriCodec.decode(minimal) as ConnectionUriParseResult.Success).descriptor.filePortHint)
    }
    @Test fun `rejects malicious input with payload free typed errors`() {
        val invalid = listOf(minimal + "&token=secret", minimal + "&port=5051", minimal.replace("v=1", "v=2"),
            minimal.replace("proto=2", "proto=1"), minimal.replace("5050", "05050"), minimal.replace("5050", "+5050"),
            minimal.replace("192.168.1.42", "example.com"), minimal.replace("192.168.1.42", "::ffff:127.0.0.1"),
            minimal.replace("192.168.1.42", "fe80%3A%3A1%257"), minimal + "&name=%C0%AF", minimal + "&name=%00", minimal + "&name=%E2%80%AE",
            minimal.replace("join?", "join/?"), minimal.replace("join?", "user@join?"), minimal + "#fragment", minimal + "&name=%", "x".repeat(4097))
        invalid.forEach { assertTrue(RoomConnectionUriCodec.decode(it) is ConnectionUriParseResult.Invalid, it) }
        assertEquals(ConnectionUriParseResult.Invalid(ConnectionUriError.UnsupportedVersion, "v"), RoomConnectionUriCodec.decode(minimal.replace("v=1", "v=3")))
    }
    @Test fun `percent decoding happens once and plus remains plus`() {
        val value = (RoomConnectionUriCodec.decode(minimal + "&name=A+%2526B&x-extra=%26port%3D1") as ConnectionUriParseResult.Success).descriptor
        assertEquals("A+%26B", value.displayNameHint)
        assertEquals(5050, value.chatPort)
    }
    @Test fun `malformed deterministic corpus never escapes parser`() {
        val random = Random(52)
        repeat(3000) {
            val raw = buildString { repeat(random.nextInt(160)) { append(random.nextInt(256).toChar()) } }
            assertNotNull(RoomConnectionUriCodec.decode(raw))
        }
    }

    @Test fun `password is encoded once preserved exactly and redacted from diagnostics`() {
        val password = "\u043f\u0430\u0440\u043e\u043b\u044c+&=%25#?"
        val descriptor = RoomConnectionDescriptor(IpLiteral.parse("192.168.1.42")!!, 5050, password = password)
        val uri = RoomConnectionUriCodec.encode(descriptor)
        assertTrue("%2B%26%3D%2525%23%3F" in uri)
        val result = RoomConnectionUriCodec.decode(uri) as ConnectionUriParseResult.Success
        assertEquals(password, result.descriptor.password)
        assertEquals(descriptor, result.descriptor)
        assertFalse(result.toString().contains(password))
        assertTrue(result.toString().contains("<redacted>"))
        assertNull((RoomConnectionUriCodec.decode(minimal) as ConnectionUriParseResult.Success).descriptor.password)
    }

    @Test fun `password cannot be empty duplicated contain whitespace or exceed URI bounds`() {
        listOf("", "a%20b", "a+b&password=other", "a%09b", "a%C2%A0b", "a%0Ab", "a".repeat(4096)).forEach {
            assertTrue(RoomConnectionUriCodec.decode("$minimal&password=$it") is ConnectionUriParseResult.Invalid)
        }
        val invalid = RoomConnectionUriCodec.decode("$minimal&password=synthetic%20secret")
        assertEquals(ConnectionUriParseResult.Invalid(ConnectionUriError.InvalidText, "password"), invalid)
        assertFalse(invalid.toString().contains("synthetic"))
    }

    @Test fun `compact encoding drops advisory metadata without dropping the password`() {
        val descriptor = RoomConnectionDescriptor(IpLiteral.parse("192.168.1.42")!!, 5050, 5051,
            "2da1e450-798a-401f-990b-cb1cc4f61d92", "ab817e84-78af-40b4-994d-41bfa5ff20fb", "Room".repeat(16),
            listOf(IpLiteral.parse("fd12:3456:789a:bcde::42")!!, IpLiteral.parse("192.168.2.42")!!), "a".repeat(760))
        val uri = RoomConnectionUriCodec.encode(descriptor)
        assertTrue(uri.length <= 1024)
        val decoded = (RoomConnectionUriCodec.decode(uri) as ConnectionUriParseResult.Success).descriptor
        assertEquals(descriptor.password, decoded.password)
        assertNull(decoded.displayNameHint)
        assertTrue(decoded.alternativeAddresses.isEmpty())
    }
}
