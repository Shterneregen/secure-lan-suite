package com.shterneregen.securelan.lantools.diagnostics

import com.shterneregen.securelan.common.net.address.IpLiteral
import java.net.IDN
import java.util.Locale

/** Strict numeric/IDN parsing before any resolver or socket operation. No URLs or shell syntax. */
object DiagnosticHost {
    /** Canonical numeric address or ASCII hostname; null denotes invalid input. */
    fun parse(raw: String): String? {
        if (raw.length !in 1..253 || raw.any { it.isWhitespace() || it.isISOControl() }) return null
        IpLiteral.parse(raw)?.let { return it.value }
        // Reject malformed IP literals and legacy numeric shorthand that JVM resolvers reinterpret.
        if (':' in raw || '%' in raw || raw.all { it.isDigit() || it == '.' }) return null
        val ascii = try { IDN.toASCII(raw.removeSuffix("."), IDN.USE_STD3_ASCII_RULES) }
        catch (_: IllegalArgumentException) { return null }
        if (ascii.length !in 1..253 || ascii.split('.').any { it.length !in 1..63 }) return null
        return ascii.lowercase(Locale.ROOT) + if (raw.endsWith('.')) "." else ""
    }

    /** Loopback is useful for local diagnostics. Unspecified, multicast and limited broadcast are not. */
    fun validTarget(address: IpLiteral): Boolean = address.isUnicast || address.value == "::1" ||
        address.isIpv4 && address.bytes()[0].toInt() and 255 == 127
}
