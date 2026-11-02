package com.shterneregen.securelan.lantools.diagnostics

import com.shterneregen.securelan.common.net.address.IpLiteral
import com.shterneregen.securelan.lantools.model.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.net.BindException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException

/**
 * Bounded checks, sharing four permits with no waiting queue. Name resolution counts toward the
 * whole-operation deadline. Only the first suitable resolved address is attempted, never a sweep.
 * Platform ports and the monotonic clock are injectable for deterministic failure/cancellation tests.
 */
class DefaultNetworkDiagnosticsService(
    private val resolver: HostResolver,
    private val networks: LanNetworkInfoProvider,
    private val connector: TcpConnector = JvmTcpConnector(),
    private val nanoTime: () -> Long = System::nanoTime,
) : NetworkDiagnosticsService {
    private val permits = Semaphore(4)

    override suspend fun tcp(request: TcpCheckRequest): LanResult<TcpCheckReceipt> {
        currentCoroutineContext().ensureActive()
        val host = DiagnosticHost.parse(request.host) ?: return LanResult.Failure(LanError.INVALID_INPUT, "host")
        if (IpLiteral.parse(host)?.let { !DiagnosticHost.validTarget(it) } == true) return LanResult.Failure(LanError.INVALID_INPUT, "host")
        if (request.port !in 1..65535) return LanResult.Failure(LanError.INVALID_INPUT, "port")
        if (request.bindingKey?.let { it.isBlank() || it.length > 256 } == true) return LanResult.Failure(LanError.INVALID_INPUT, "interface")
        return operation(request.timeoutMillis) {
            val binding = request.bindingKey?.let { key ->
                val snapshot = when (val result = networks.snapshot()) {
                    is LanResult.Failure -> return@operation result
                    is LanResult.Success -> result.value
                }
                val matches = snapshot.interfaces.filter { it.binding.key == key }
                if (matches.size != 1) return@operation LanResult.Failure(LanError.INTERFACE_UNAVAILABLE)
                matches.single().binding.also {
                    if (!it.up || !DiagnosticHost.validTarget(it.address)) return@operation LanResult.Failure(LanError.INTERFACE_UNSUITABLE)
                }
            }
            val addresses = when (val result = addresses(host)) {
                is LanResult.Failure -> return@operation result
                is LanResult.Success -> result.value
            }
            val target = addresses.firstOrNull { DiagnosticHost.validTarget(it) && (binding == null || it.isIpv4 == binding.address.isIpv4) }
                ?: return@operation LanResult.Failure(LanError.RESOLUTION_FAILED)
            if (!target.isIpv4 && target.isLinkLocal) return@operation LanResult.Failure(LanError.UNSUPPORTED, "scope")
            if (binding != null) {
                val fresh = when (val result = networks.snapshot()) {
                    is LanResult.Failure -> return@operation result
                    is LanResult.Success -> result.value
                }
                if (fresh.interfaces.none { it.binding == binding }) return@operation LanResult.Failure(LanError.NETWORK_CHANGED)
                if (binding.prefix.isDirectedBroadcast(target)) return@operation LanResult.Failure(LanError.INVALID_INPUT, "host")
            }
            val started = nanoTime()
            connector.connect(target, request.port, binding?.address)
            currentCoroutineContext().ensureActive()
            LanResult.Success(TcpCheckReceipt(target, request.port, binding?.address, elapsed(started)))
        }
    }

    override suspend fun lookup(request: DnsLookupRequest): LanResult<DnsLookupReceipt> {
        currentCoroutineContext().ensureActive()
        val host = DiagnosticHost.parse(request.host) ?: return LanResult.Failure(LanError.INVALID_INPUT, "host")
        return operation(request.timeoutMillis) {
            val started = nanoTime()
            val result = addresses(host)
            if (result is LanResult.Failure) return@operation result
            val addresses = (result as LanResult.Success).value.filter {
                when (request.family) { AddressFamily.ALL -> true; AddressFamily.IPV4 -> it.isIpv4; AddressFamily.IPV6 -> !it.isIpv4 }
            }
            if (addresses.isEmpty()) LanResult.Failure(LanError.RESOLUTION_FAILED)
            else LanResult.Success(DnsLookupReceipt(host, addresses.toList(), elapsed(started), IpLiteral.parse(host) != null))
        }
    }

    private suspend fun addresses(host: String): LanResult<List<IpLiteral>> {
        IpLiteral.parse(host)?.let { return LanResult.Success(listOf(it)) }
        return when (val result = resolver.resolve(host)) {
            is LanResult.Failure -> result
            is LanResult.Success -> if (result.value.size > 32) LanResult.Failure(LanError.RESOURCE_LIMIT)
                else LanResult.Success(result.value.distinct().toList())
        }
    }

    private fun elapsed(started: Long): Long = (nanoTime() - started).coerceAtLeast(0)

    private suspend fun <T> operation(timeout: Long, block: suspend () -> LanResult<T>): LanResult<T> {
        currentCoroutineContext().ensureActive()
        if (timeout !in 1..30_000) return LanResult.Failure(LanError.INVALID_INPUT, "timeout")
        if (!permits.tryAcquire()) return LanResult.Failure(LanError.RESOURCE_LIMIT)
        try {
            return withTimeoutOrNull(timeout) {
                try { block() }
                catch (_: SecurityException) { currentCoroutineContext().ensureActive(); LanResult.Failure(LanError.PERMISSION_DENIED) }
                catch (error: IOException) {
                    // Translate inside this deadline's context so cancellation wins over a close error.
                    currentCoroutineContext().ensureActive()
                    LanResult.Failure(when (error) {
                        is BindException -> LanError.NETWORK_CHANGED
                        is NoRouteToHostException -> LanError.NETWORK_UNAVAILABLE
                        is SocketTimeoutException -> LanError.TIMEOUT
                        is ConnectException -> LanError.CONNECTION_FAILED
                        else -> LanError.IO
                    })
                }
            } ?: LanResult.Failure(LanError.TIMEOUT)
        } finally { permits.release() }
    }
}
