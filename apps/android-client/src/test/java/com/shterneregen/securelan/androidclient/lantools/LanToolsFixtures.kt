package com.shterneregen.securelan.androidclient.lantools

import com.shterneregen.securelan.androidclient.lantools.network.*
import com.shterneregen.securelan.common.net.address.IpLiteral
import com.shterneregen.securelan.common.net.interfaces.NetworkBinding
import com.shterneregen.securelan.lantools.diagnostics.*
import com.shterneregen.securelan.lantools.model.*
import java.net.DatagramSocket
import java.net.Socket

internal fun ip(value: String) = requireNotNull(IpLiteral.parse(value))
internal fun link(key: String = "wifi:192.168.1.2", network: String = "wifi", address: String = "192.168.1.2") = AndroidLanLink(
    NetworkBinding(key, network, ip(address), 24), network, ip("192.168.1.255"), listOf(ip("192.168.1.1")), listOf("192.168.1.1"))

internal class FakeLanPlatform(var entries: List<AndroidLanLink> = listOf(link())) : AndroidLanPlatform {
    var starts = 0
    var closes = 0
    var vpn = false
    var changed: () -> Unit = {}
    var socketBinding: LanSocketBinding? = object : LanSocketBinding {
        override fun bind(socket: Socket) = Unit
        override fun bind(socket: DatagramSocket) = Unit
    }
    override fun links() = entries
    override fun route(binding: NetworkBinding) = socketBinding.takeIf { entries.any { it.binding == binding } }
    override fun start(changed: () -> Unit) { starts++; this.changed = changed }
    override fun hasVpn() = vpn
    override fun close() { closes++ }
    fun snapshot() = DiagnosticsNetworkSnapshot(entries.map { DiagnosticInterface(LanInterfaceSnapshot(it.binding, it.name, true, it.broadcast),
        it.gateways, it.dns, MetadataAvailability.AVAILABLE) }, "test")
}

internal class FakeLanStore : LanDeviceStore {
    var devices: List<SavedLanDevice> = emptyList()
    var failLoad = false
    var failSave = false
    var saves = 0
    override fun load(): List<SavedLanDevice> { check(!failLoad); return devices }
    override fun save(devices: List<SavedLanDevice>) { saves++; check(!failSave); this.devices = devices }
}
