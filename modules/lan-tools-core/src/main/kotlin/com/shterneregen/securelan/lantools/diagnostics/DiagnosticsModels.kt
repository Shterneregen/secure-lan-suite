package com.shterneregen.securelan.lantools.diagnostics

import com.shterneregen.securelan.common.net.address.IpLiteral
import com.shterneregen.securelan.lantools.model.LanInterfaceSnapshot
import com.shterneregen.securelan.lantools.model.LanResult

/** Filter applied to the system resolver's returned addresses, not a promise about DNS wire queries. */
enum class AddressFamily { ALL, IPV4, IPV6 }

/** A single explicit port check. A null binding uses OS routing; no subnet/port scanning occurs. */
data class TcpCheckRequest(val host: String, val port: Int, val bindingKey: String? = null, val timeoutMillis: Long = 5_000)

/** Only this TCP endpoint accepted a connection. Duration excludes name resolution and is not ICMP RTT. */
data class TcpCheckReceipt(val address: IpLiteral, val port: Int, val source: IpLiteral?, val connectNanos: Long)

/** System name resolution, including its cache/hosts configuration; no custom DNS server is implied. */
data class DnsLookupRequest(val host: String, val family: AddressFamily = AddressFamily.ALL, val timeoutMillis: Long = 5_000)

/** Elapsed lookup time is independent of device reachability; answers may come from the OS cache. */
data class DnsLookupReceipt(val host: String, val addresses: List<IpLiteral>, val lookupNanos: Long, val numericInput: Boolean = false)

/** Availability of platform route/DNS metadata, distinct from an available but empty list. */
enum class MetadataAvailability { AVAILABLE, UNSUPPORTED, UNAVAILABLE, PERMISSION_DENIED, TIMEOUT, RESOURCE_LIMIT }

/** Optional OS facts for one source. DNS strings are validated numeric addresses, preserving scope IDs. */
data class DiagnosticInterface(
    val source: LanInterfaceSnapshot,
    val gateways: List<IpLiteral> = emptyList(),
    val dnsServers: List<String> = emptyList(),
    val metadata: MetadataAvailability = MetadataAvailability.UNSUPPORTED,
)

/** Local observations only; no remote probing is performed while gathering this snapshot. */
data class DiagnosticsNetworkSnapshot(val interfaces: List<DiagnosticInterface>, val provenance: String)

/** Implementations own platform resources and must bound time and output size. */
fun interface DiagnosticsNetworkInfoProvider {
    suspend fun snapshot(): LanResult<DiagnosticsNetworkSnapshot>
}

/** Resolver ports may use platform-specific APIs. Cancellation must promptly release the caller. */
fun interface HostResolver {
    suspend fun resolve(host: String): LanResult<List<IpLiteral>>
}

/** The transport owns and closes a new connection on every call; callers provide an operation deadline. */
fun interface TcpConnector {
    suspend fun connect(address: IpLiteral, port: Int, source: IpLiteral?)
}

/** Room-independent checks. ICMP is explicitly unavailable until a reliable platform backend exists. */
interface NetworkDiagnosticsService {
    suspend fun tcp(request: TcpCheckRequest): LanResult<TcpCheckReceipt>
    suspend fun lookup(request: DnsLookupRequest): LanResult<DnsLookupReceipt>
}
