package com.shterneregen.securelan.desktop.lantools.discovery

import com.shterneregen.securelan.common.net.interfaces.NetworkBinding
import com.shterneregen.securelan.lantools.discovery.LanServiceTypes
import java.net.InetAddress
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceInfo
import javax.jmdns.ServiceListener

/** Library-specific data is copied and bounded before crossing the callback boundary. No TXT escapes. */
data class DnsSdService(val type: String, val instance: String, val hostname: String?, val addresses: List<String>, val port: Int)

sealed interface DnsSdEvent {
    data class Resolved(val service: DnsSdService) : DnsSdEvent
    data class Removed(val type: String, val instance: String) : DnsSdEvent
    data object Limited : DnsSdEvent
}

/** One interface handle, independent of room mDNS handles and their advertisements. */
interface LanDnsSdBrowser : AutoCloseable {
    fun start(emit: (DnsSdEvent) -> Unit)
    override fun close()
}

/**
 * Uses JmDNS's built-in asynchronous service collector, without extra blocking requestServiceInfo
 * calls or per-device threads. Library cache/packet scheduling is internal to JmDNS; application
 * callbacks, accepted records, subscribed types, interfaces and browse duration are bounded above it.
 */
internal class JmDnsLanBrowser(binding: NetworkBinding) : LanDnsSdBrowser {
    private val dns = JmDNS.create(InetAddress.getByAddress(binding.address.bytes()), "lan-tools-${UUID.randomUUID().toString().take(8)}")
    private val closed = AtomicBoolean()
    private val listeners = mutableMapOf<String, ServiceListener>()

    override fun start(emit: (DnsSdEvent) -> Unit) {
        for (type in LanServiceTypes.supported) {
            val listener = object : ServiceListener {
                override fun serviceAdded(event: ServiceEvent) = Unit // JmDNS's own collector resolves SRV/A records.
                override fun serviceRemoved(event: ServiceEvent) {
                    if (!closed.get() && event.name.length <= 255) emit(DnsSdEvent.Removed(type, event.name))
                }
                override fun serviceResolved(event: ServiceEvent) {
                    if (closed.get()) return
                    emit(copyDnsSdEvent(type, event.name, event.info))
                }
            }
            listeners[type] = listener
            dns.addServiceListener(type, listener)
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            try { listeners.forEach { (type, listener) -> dns.removeServiceListener(type, listener) } }
            finally { listeners.clear(); dns.close() }
        }
    }
}

/** TXT can contain credentials/binary garbage: inspect its size only, never parse, retain or expose it. */
internal fun copyDnsSdEvent(type: String, instance: String, info: ServiceInfo): DnsSdEvent {
    val addresses = info.inetAddresses
    if (instance.length > 255 || (info.server?.length ?: 0) > 253 || addresses.size > 16 || info.textBytes.size > 4096) return DnsSdEvent.Limited
    return DnsSdEvent.Resolved(DnsSdService(type, instance, info.server, addresses.map { it.hostAddress }, info.port))
}
