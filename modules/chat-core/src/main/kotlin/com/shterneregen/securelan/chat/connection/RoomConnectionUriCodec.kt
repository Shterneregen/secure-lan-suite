package com.shterneregen.securelan.chat.connection

import com.shterneregen.securelan.chat.discovery.DiscoveryValidation
import com.shterneregen.securelan.common.net.address.IpLiteral
import com.shterneregen.securelan.common.net.address.LanAddressPolicy
import java.io.ByteArrayOutputStream
import java.net.URI
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

/** Bounded, side-effect-free URI v1 parser. Errors deliberately carry no external payload. */
object RoomConnectionUriCodec {
    private val keys = setOf("v", "proto", "host", "port", "filePort", "pid", "rid", "name", "alt", "password")
    private class Invalid(val reason: ConnectionUriError, val field: String? = null) : RuntimeException()
    private fun fail(error: ConnectionUriError, field: String? = null): Nothing = throw Invalid(error, field)

    fun decode(raw: String): ConnectionUriParseResult = try {
        ConnectionUriParseResult.Success(parse(raw))
    } catch (e: Invalid) { ConnectionUriParseResult.Invalid(e.reason, e.field) }
      catch (_: Exception) { ConnectionUriParseResult.Invalid(ConnectionUriError.InvalidUri) }

    private fun parse(raw: String): RoomConnectionDescriptor {
        if (raw.length > 4096) fail(ConnectionUriError.InputTooLong)
        if (raw.any { it.code !in 33..126 }) fail(ConnectionUriError.InvalidUri)
        val uri = try { URI(raw) } catch (_: Exception) { fail(ConnectionUriError.InvalidUri) }
        if (!uri.scheme.equals("securelan", true)) fail(ConnectionUriError.WrongScheme)
        if (uri.isOpaque || !uri.rawAuthority.equals("join", true) || !uri.rawPath.isNullOrEmpty() || uri.rawFragment != null)
            fail(ConnectionUriError.InvalidUri)
        val pairs = (uri.rawQuery ?: fail(ConnectionUriError.MissingField, "v")).split('&')
        if (pairs.size > 16) fail(ConnectionUriError.InputTooLong)
        val fields = linkedMapOf<String, MutableList<String>>()
        var total = 0
        var extensions = 0
        pairs.forEach { pair ->
            val index = pair.indexOf('=')
            if (index <= 0) fail(ConnectionUriError.InvalidUri)
            val key = pair.substring(0, index)
            if (key !in keys && !Regex("x-[a-z0-9-]{1,14}").matches(key)) fail(ConnectionUriError.UnknownField)
            if (key != "alt" && key in fields) fail(ConnectionUriError.DuplicateField, key.takeIf { it in keys })
            val value = decodeComponent(pair.substring(index + 1))
            val bytes = value.toByteArray(Charsets.UTF_8).size
            total += key.length + bytes
            if (total > 2048) fail(ConnectionUriError.InputTooLong)
            if (key.startsWith("x-") && (++extensions > 4 || bytes > 128)) fail(ConnectionUriError.InputTooLong)
            fields.getOrPut(key) { mutableListOf() }.add(value)
        }
        fun required(key: String): String = fields[key]?.singleOrNull() ?: fail(ConnectionUriError.MissingField, key)
        if (required("v") != "1") fail(ConnectionUriError.UnsupportedVersion, "v")
        if (required("proto") != "2") fail(ConnectionUriError.UnsupportedProtocol, "proto")
        fun address(value: String): IpLiteral {
            if ('%' in value) fail(ConnectionUriError.UnsupportedAddressScope, "host")
            val ip = IpLiteral.parse(value) ?: fail(ConnectionUriError.InvalidAddress, "host")
            if (!ip.isIpv4 && ip.isLinkLocal) fail(ConnectionUriError.UnsupportedAddressScope, "host")
            if (!LanAddressPolicy.portable(ip)) fail(ConnectionUriError.InvalidAddress, "host")
            return ip
        }
        val host = address(required("host"))
        val port = DiscoveryValidation.port(required("port")) ?: fail(ConnectionUriError.InvalidPort, "port")
        val fp = fields["filePort"]?.single()?.let { DiscoveryValidation.port(it) ?: fail(ConnectionUriError.InvalidPort, "filePort") }
        if (fp == port) fail(ConnectionUriError.InvalidPort, "filePort")
        fun uuid(key: String): String? = fields[key]?.single()?.also { if (!DiscoveryValidation.uuid(it)) fail(ConnectionUriError.InvalidText, key) }
        val name = fields["name"]?.single()?.also { if (!DiscoveryValidation.displayName(it)) fail(ConnectionUriError.InvalidText, "name") }
        val alternatives = fields["alt"].orEmpty()
        if (alternatives.size > 2) fail(ConnectionUriError.InputTooLong, "alt")
        val password = fields["password"]?.single()?.also {
            if (it.isEmpty() || it.any(Char::isWhitespace)) fail(ConnectionUriError.InvalidText, "password")
        }
        return RoomConnectionDescriptor(host, port, fp, uuid("pid"), uuid("rid"), name,
            alternatives.map(::address).distinct().filter { it != host }, password)
    }

    fun encode(value: RoomConnectionDescriptor): String {
        fun render(name: String?, alternatives: List<IpLiteral>): String {
            val fields = mutableListOf("v" to "1", "proto" to "2", "host" to value.primaryAddress.value, "port" to value.chatPort.toString())
            value.filePortHint?.let { fields += "filePort" to it.toString() }
            value.peerIdHint?.let { fields += "pid" to it }
            value.roomInstanceIdHint?.let { fields += "rid" to it }
            name?.let { fields += "name" to it }
            alternatives.forEach { fields += "alt" to it.value }
            value.password?.let { fields += "password" to it }
            return "securelan://join?" + fields.joinToString("&") { it.first + "=" + encodeComponent(it.second) }
        }
        var result = render(value.displayNameHint, value.alternativeAddresses)
        require(decode(result) is ConnectionUriParseResult.Success) { "Invalid connection descriptor" }
        if (result.length > 1024) result = render(null, emptyList())
        require(result.length <= 1024) { "Connection descriptor too large" }
        return result
    }

    private fun encodeComponent(value: String): String = buildString {
        value.toByteArray(Charsets.UTF_8).forEach { byte ->
            val c = byte.toInt() and 255
            if (c in 65..90 || c in 97..122 || c in 48..57 || c.toChar() in "-._~") append(c.toChar())
            else { append('%'); append("0123456789ABCDEF"[c shr 4]); append("0123456789ABCDEF"[c and 15]) }
        }
    }

    private fun decodeComponent(raw: String): String {
        val out = ByteArrayOutputStream()
        var i = 0
        while (i < raw.length) {
            if (raw[i] == '%') {
                if (i + 2 >= raw.length) fail(ConnectionUriError.InvalidUri)
                val a = raw[i + 1].digitToIntOrNull(16) ?: fail(ConnectionUriError.InvalidUri)
                val b = raw[i + 2].digitToIntOrNull(16) ?: fail(ConnectionUriError.InvalidUri)
                out.write(a * 16 + b); i += 3
            } else { out.write(raw[i].code); i++ }
        }
        val value = try { Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(out.toByteArray())).toString()
        } catch (_: Exception) { fail(ConnectionUriError.InvalidText) }
        if (value.any { Character.isISOControl(it) }) fail(ConnectionUriError.InvalidText)
        return value
    }
}
