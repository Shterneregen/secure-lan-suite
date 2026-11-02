package com.shterneregen.securelan.desktop.lantools

import com.shterneregen.securelan.lantools.diagnostics.*
import com.shterneregen.securelan.lantools.model.*

/** Enriches JVM addresses without changing the snapshots/equality checks used by Wake-on-LAN. */
class DesktopNetworkInfoProvider(
    private val base: LanNetworkInfoProvider,
    private val platform: WindowsNetworkInfoBackend = WindowsNetworkInfoBackend(),
) : DiagnosticsNetworkInfoProvider {
    override suspend fun snapshot(): LanResult<DiagnosticsNetworkSnapshot> {
        val local = when (val result = base.snapshot()) {
            is LanResult.Failure -> return result
            is LanResult.Success -> result.value
        }
        val metadata = platform.read()
        return LanResult.Success(DiagnosticsNetworkSnapshot(local.interfaces.map { source ->
            val details = metadata.records[source.binding.address.value]
            DiagnosticInterface(source, details?.gateways.orEmpty(), details?.dnsServers.orEmpty(),
                if (metadata.availability == MetadataAvailability.AVAILABLE && details == null) MetadataAvailability.UNAVAILABLE else metadata.availability)
        }, if (metadata.availability == MetadataAvailability.AVAILABLE) "JVM interfaces + Windows network configuration" else "JVM interfaces; gateway/DNS metadata: ${metadata.availability.name.lowercase()}"))
    }
}
