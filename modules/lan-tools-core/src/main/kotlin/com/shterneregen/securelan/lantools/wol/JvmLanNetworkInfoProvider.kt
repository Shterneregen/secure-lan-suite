package com.shterneregen.securelan.lantools.wol

import com.shterneregen.securelan.common.net.address.IpLiteral
import com.shterneregen.securelan.common.net.interfaces.NetworkBinding
import com.shterneregen.securelan.common.net.interfaces.NetworkKind
import com.shterneregen.securelan.lantools.model.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.NetworkInterface
import java.net.SocketException
import java.util.Locale

/**
 * One-shot IPv4 enumeration; no sockets, polling, DNS, room discovery or platform handles retained.
 *
 * Blocking OS enumeration runs on [dispatcher]. The optional [defaultRouteInterfaceKey] callback
 * belongs to the application and must be bounded; this shared module never reads OS route tables.
 * Recommendations are refreshed on every call and do not influence the WoL selection policy.
 */
class JvmLanNetworkInfoProvider internal constructor(
    private val dispatcher: CoroutineDispatcher,
    private val defaultRouteInterfaceKey: () -> String?,
    private val enumerate: () -> Sequence<LanInterfaceSnapshot>,
) : LanNetworkInfoProvider {
    /** Creates an OS-backed reader with optional application-owned default-route hints. */
    constructor(
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        defaultRouteInterfaceKey: () -> String? = { null },
    ) : this(dispatcher, defaultRouteInterfaceKey, ::enumerateInterfaces)

    override suspend fun snapshot(): LanResult<LanNetworkSnapshot> = withContext(dispatcher) {
        try {
            val result = mutableListOf<LanInterfaceSnapshot>()
            for (entry in enumerate()) {
                currentCoroutineContext().ensureActive()
                if (result.size >= 512) return@withContext LanResult.Failure(LanError.RESOURCE_LIMIT)
                result += entry
            }
            // A missing/denied hint must not make otherwise usable local interfaces unavailable.
            val hint = try { defaultRouteInterfaceKey() } catch (_: IOException) { null } catch (_: SecurityException) { null }
            val availableHint = hint?.takeIf { key ->
                key.length in 1..256 && key.none { it.isWhitespace() || it.isISOControl() } && result.any {
                    it.binding.interfaceKey == key && it.binding.up && it.binding.kind == NetworkKind.LAN &&
                        it.binding.address.isIpv4 && it.binding.address.isUnicast &&
                        it.binding.prefixLength in 1..30 && it.broadcastCapable
                }
            }
            currentCoroutineContext().ensureActive()
            LanResult.Success(LanNetworkSnapshot(result, availableHint))
        } catch (_: SnapshotLimitException) {
            LanResult.Failure(LanError.RESOURCE_LIMIT)
        } catch (_: SecurityException) {
            LanResult.Failure(LanError.PERMISSION_DENIED)
        } catch (_: SocketException) {
            LanResult.Failure(LanError.NETWORK_UNAVAILABLE)
        }
    }
}

private class SnapshotLimitException : Exception()

private fun enumerateInterfaces(): Sequence<LanInterfaceSnapshot> = sequence {
    val interfaces = NetworkInterface.getNetworkInterfaces()
    var count = 0
    while (interfaces != null && interfaces.hasMoreElements()) {
        if (++count > 256) throw SnapshotLimitException()
        val nic = interfaces.nextElement()
        if (nic.isLoopback) continue
        val kind = classify(nic)
        val up = nic.isUp
        for (address in nic.interfaceAddresses) {
            val ip = IpLiteral.parse(address.address.hostAddress.orEmpty()) ?: continue
            if (!ip.isIpv4) continue
            val broadcast = address.broadcast?.hostAddress?.let(IpLiteral::parse)
            yield(LanInterfaceSnapshot(
                NetworkBinding("${nic.name}:${ip.value}", nic.name, ip, address.networkPrefixLength.toInt(), kind,
                    up = up, multicast = nic.supportsMulticast()),
                nic.displayName ?: nic.name, broadcast != null, broadcast,
            ))
        }
    }
}

private fun classify(nic: NetworkInterface): NetworkKind {
    // Names are only heuristics; the caller still chooses a source binding before an ambiguous send.
    val name = "${nic.name} ${nic.displayName}".lowercase(Locale.ROOT)
    return when {
        nic.isPointToPoint || listOf("vpn", "tun", "tap", "wireguard", "tailscale", "ppp").any(name::contains) -> NetworkKind.VPN
        nic.isVirtual || listOf("virtual", "vmware", "hyper-v", "vbox", "wsl", "docker", "vethernet").any(name::contains) -> NetworkKind.VIRTUAL
        else -> NetworkKind.LAN
    }
}
