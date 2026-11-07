package com.shterneregen.securelan.desktop.compose.state.lantools

import com.shterneregen.securelan.lantools.model.LanNetworkSnapshot
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val knownServiceLabels = mapOf(
    "_http._tcp.local." to "HTTP",
    "_https._tcp.local." to "HTTPS",
    "_ipp._tcp.local." to "IPP",
    "_ipps._tcp.local." to "IPPS",
    "_ssh._tcp.local." to "SSH",
    "_smb._tcp.local." to "SMB",
)

internal fun formatLanServiceType(type: String): String = knownServiceLabels[type] ?: type

internal fun formatLanObservedTime(
    value: Instant,
    now: Instant = Instant.now(),
    zone: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault(),
): String {
    val date = value.atZone(zone).toLocalDate()
    val today = now.atZone(zone).toLocalDate()
    val time = DateTimeFormatter.ofPattern("HH:mm", locale).withZone(zone).format(value)
    return when (date) {
        today -> "Today, $time"
        today.minusDays(1) -> "Yesterday, $time"
        else -> DateTimeFormatter.ofPattern("d MMM, HH:mm", locale).withZone(zone).format(value)
    }
}

/** Prefer the current interface label; the raw scope remains available in technical details. */
internal fun formatLanNetwork(scope: String, networks: LanNetworkSnapshot): String =
    networks.interfaces.firstOrNull { it.binding.key == scope }
        ?.let { "${it.displayName} · ${it.binding.address.value}" }
        ?: scope
