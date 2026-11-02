package com.shterneregen.securelan.desktop.lantools.discovery

import com.shterneregen.securelan.chat.discovery.HostedRoomAdvertisement
import com.shterneregen.securelan.chat.discovery.mdns.DnsSdRecordCodec
import com.shterneregen.securelan.common.net.interfaces.NetworkBindingSelector
import com.shterneregen.securelan.desktop.network.discovery.DesktopNetworkMonitor
import com.shterneregen.securelan.desktop.network.discovery.RealJmDnsFacade
import kotlinx.coroutines.*
import java.net.InetAddress
import java.util.UUID
import javax.jmdns.JmDNS
import javax.jmdns.ServiceInfo

/**
 * Explicit integration entry point, excluded from unit/headless checks. Advertises local test
 * services on one actual LAN binding; no TCP connections or remote service actions are performed.
 * Separate room facades must continue resolving a room update after Explorer has closed.
 */
object LanExplorerMdnsProbe {
    @JvmStatic fun main(args: Array<String>) = runBlocking {
        val binding = NetworkBindingSelector.select(DesktopNetworkMonitor().snapshot()).firstOrNull()
            ?: error("NO_ELIGIBLE_MULTICAST_LAN")
        val suffix = UUID.randomUUID().toString().take(8)
        val advertised = CompletableDeferred<Unit>()
        val withdrawn = CompletableDeferred<Unit>()
        val roomSeen = CompletableDeferred<Unit>()
        val roomUpdated = CompletableDeferred<Unit>()
        val room = HostedRoomAdvertisement(UUID.randomUUID().toString(), UUID.randomUUID().toString(), "Explorer probe room", 5098, null, true)
        var publisher: JmDNS? = null
        var roomHost: RealJmDnsFacade? = null
        var roomBrowser: RealJmDnsFacade? = null
        var run: Job? = null
        try {
            withTimeout(90_000) {
                val services = withContext(Dispatchers.IO) { JmDNS.create(InetAddress.getByAddress(binding.address.bytes()), "explorer-probe-$suffix") }
                publisher = services
                val host = withContext(Dispatchers.IO) { RealJmDnsFacade(binding) }
                roomHost = host
                val listener = withContext(Dispatchers.IO) { RealJmDnsFacade(binding) }
                roomBrowser = listener
                listener.browse({ event ->
                    val record = DnsSdRecordCodec.decode(event.txt, event.port)
                    if (record?.roomInstanceId == room.roomInstanceId) {
                        if (record.displayName == "Explorer probe updated") roomUpdated.complete(Unit) else roomSeen.complete(Unit)
                    }
                }, {})
                withContext(Dispatchers.IO) {
                    host.advertise(room)
                    services.registerService(ServiceInfo.create("_http._tcp.local.", "Explorer HTTP $suffix", 18181, "probe=local"))
                    services.registerService(ServiceInfo.create("_ssh._tcp.local.", "Explorer SSH $suffix", 18182, "probe=local"))
                }
                withTimeout(20_000) { roomSeen.await() }
                println("Independent room advertisement resolved")
                val discovery = DesktopLanDeviceDiscovery({ listOf(binding) })
                val browsing = launch {
                    discovery.browse { snapshot ->
                        check(snapshot.devices.none { row -> row.services.any { it.type == DnsSdRecordCodec.SERVICE_TYPE } })
                        val rows = snapshot.devices.filter { row -> row.services.any { it.instance.endsWith(suffix) } }
                        if (rows.size == 1 && rows.single().services.count { it.instance.endsWith(suffix) } == 2) advertised.complete(Unit)
                        if (advertised.isCompleted && rows.isEmpty()) withdrawn.complete(Unit)
                    }
                }
                run = browsing
                withTimeout(20_000) { advertised.await() }
                println("HTTP and SSH resolved and merged on one SRV target")
                withContext(Dispatchers.IO) { services.unregisterAllServices() }
                withTimeout(10_000) { withdrawn.await() }
                println("DNS-SD goodbyes removed Explorer observations")
                browsing.cancelAndJoin()
                withContext(Dispatchers.IO) { host.advertise(room.copy(displayName = "Explorer probe updated")) }
                withTimeout(15_000) { roomUpdated.await() }
                println("Room discovery still resolves updates after Explorer cleanup: passed")
            }
        } finally {
            run?.cancelAndJoin()
            withContext(Dispatchers.IO + NonCancellable) {
                runCatching { publisher?.close() }; runCatching { roomHost?.close() }; runCatching { roomBrowser?.close() }
            }
            println("All probe-owned mDNS handles closed")
        }
    }
}
