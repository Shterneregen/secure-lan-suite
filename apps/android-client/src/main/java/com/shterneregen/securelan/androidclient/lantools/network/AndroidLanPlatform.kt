package com.shterneregen.securelan.androidclient.lantools.network

import android.content.Context
import android.net.ConnectivityManager
import com.shterneregen.securelan.androidclient.network.discovery.AndroidLanNetworkMonitor
import com.shterneregen.securelan.common.net.address.IpLiteral
import com.shterneregen.securelan.common.net.interfaces.NetworkBinding
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.Socket

/** Opaque platform binding: reusable models never retain an Android Network handle. */
interface LanSocketBinding {
    fun bind(socket: Socket)
    fun bind(socket: DatagramSocket)
}

/** Local facts only; null broadcast and unavailable metadata never authorize inferred capabilities. */
data class AndroidLanLink(val binding: NetworkBinding, val name: String, val broadcast: IpLiteral?,
    val gateways: List<IpLiteral>, val dns: List<String>, val metadataAvailable: Boolean = true)

/** A subscription belongs to the tool, while the underlying callback registry is shared with rooms. */
interface AndroidLanPlatform : AutoCloseable {
    fun links(): List<AndroidLanLink>
    fun route(binding: NetworkBinding): LanSocketBinding?
    fun start(changed: () -> Unit)
    fun hasVpn(): Boolean
}

/** Owns one shared-registry subscription and resolves ephemeral Network handles for each operation. */
class SystemLanPlatform(context: Context) : AndroidLanPlatform {
    private val monitor = AndroidLanNetworkMonitor(context.applicationContext)
    private val manager = context.getSystemService(ConnectivityManager::class.java)
    override fun links(): List<AndroidLanLink> = monitor.snapshot().filter { it.address.isIpv4 }.map { binding ->
        val network = monitor.network(binding.interfaceKey)
        val properties = network?.let(manager::getLinkProperties)
        // OEM/privacy restrictions on interface details must not hide LinkProperties diagnostics.
        val broadcast = runCatching {
            NetworkInterface.getByInetAddress(InetAddress.getByAddress(binding.address.bytes()))?.interfaceAddresses
                ?.firstOrNull { it.address.address.contentEquals(binding.address.bytes()) }?.broadcast?.hostAddress?.let(IpLiteral::parse)
        }.getOrNull()
        AndroidLanLink(binding, properties?.interfaceName ?: binding.interfaceKey, broadcast,
            properties?.routes.orEmpty().filter { it.isDefaultRoute }.mapNotNull { it.gateway?.hostAddress?.let(IpLiteral::parse)?.takeIf { ip -> ip.isIpv4 && ip.isUnicast } }.distinct(),
            properties?.dnsServers.orEmpty().mapNotNull { it.hostAddress }.distinct(), properties != null)
    }
    override fun route(binding: NetworkBinding): LanSocketBinding? {
        if (monitor.snapshot().none { it == binding }) return null
        val network = monitor.network(binding.interfaceKey) ?: return null
        // Recheck LinkProperties before binding. No process-wide routing change is made.
        if (manager.getLinkProperties(network)?.linkAddresses?.none { it.address.address.contentEquals(binding.address.bytes()) } != false) return null
        return object : LanSocketBinding {
            override fun bind(socket: Socket) = network.bindSocket(socket)
            override fun bind(socket: DatagramSocket) = network.bindSocket(socket)
        }
    }
    override fun start(changed: () -> Unit) = monitor.start(changed)
    override fun hasVpn(): Boolean = monitor.hasVpn()
    override fun close() = monitor.close()
}
