package com.shterneregen.securelan.desktop.network.discovery

import com.shterneregen.securelan.chat.discovery.HostedRoomAdvertisement
import com.shterneregen.securelan.chat.discovery.mdns.DnsSdRecordCodec
import com.shterneregen.securelan.common.net.interfaces.NetworkBinding
import java.net.InetAddress
import java.util.UUID
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceInfo
import javax.jmdns.ServiceListener

/** Library lifetimes are isolated here so provider races can be tested without multicast. */
interface JmDnsFacade : AutoCloseable {
    data class Resolved(val name: String, val port: Int, val addresses: List<String>, val txt: Map<String, ByteArray>)
    fun browse(resolved: (Resolved) -> Unit, removed: (String) -> Unit)
    fun advertise(value: HostedRoomAdvertisement?)
    override fun close()
}

internal class RealJmDnsFacade(binding: NetworkBinding) : JmDnsFacade {
    private val suffix = UUID.randomUUID().toString().replace("-", "").take(12)
    private val dns = JmDNS.create(InetAddress.getByAddress(binding.address.bytes()), "sl-$suffix")
    private val found = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private var registration: ServiceInfo? = null
    private var listener: ServiceListener? = null
    override fun browse(resolved: (JmDnsFacade.Resolved) -> Unit, removed: (String) -> Unit) {
        val callback = object : ServiceListener {
            override fun serviceAdded(event: ServiceEvent) {
                if (event.name.length > 255 || event.name !in found && found.size >= 512) return
                found += event.name
                dns.requestServiceInfo(event.type, event.name, true)
            }
            override fun serviceRemoved(event: ServiceEvent) { found -= event.name; removed(event.name) }
            override fun serviceResolved(event: ServiceEvent) {
                if (event.name !in found) return
                val info = event.info
                val names = info.propertyNames.toList().take(13)
                val txt = names.associateWith { info.getPropertyBytes(it) ?: byteArrayOf() }
                resolved(JmDnsFacade.Resolved(event.name, info.port, info.inetAddresses.map { it.hostAddress }, txt))
            }
        }
        listener = callback
        dns.addServiceListener(DnsSdRecordCodec.SERVICE_TYPE, callback)
    }
    override fun advertise(value: HostedRoomAdvertisement?) {
        registration?.let { dns.unregisterService(it) }
        registration = null
        if (value?.discoverable != true) return
        val name = "SecureLan-${value.roomInstanceId.replace("-", "").take(12)}-${suffix.takeLast(4)}"
        val info = ServiceInfo.create(DnsSdRecordCodec.SERVICE_TYPE, name, value.chatPort, 0, 0, DnsSdRecordCodec.encode(value))
        dns.registerService(info)
        registration = info
    }
    override fun close() {
        listener?.let { dns.removeServiceListener(DnsSdRecordCodec.SERVICE_TYPE, it) }
        found.clear()
        dns.unregisterAllServices()
        dns.close()
    }
}
