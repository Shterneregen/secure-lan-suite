package com.shterneregen.securelan.desktop.lantools

import com.shterneregen.securelan.common.net.address.IpLiteral
import com.shterneregen.securelan.lantools.diagnostics.HostResolver
import com.shterneregen.securelan.lantools.model.LanError
import com.shterneregen.securelan.lantools.model.LanResult
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/**
 * System resolver with two daemon workers and no queue. JVM native lookups cannot reliably be
 * interrupted: cancellation releases the caller, while the occupied slot remains until native code
 * returns. Saturation fails promptly instead of accumulating jobs or replacement threads.
 */
class DesktopHostResolver(
    private val lookup: (String) -> List<IpLiteral> = { host ->
        InetAddress.getAllByName(host).mapNotNull { IpLiteral.parse(it.hostAddress) }
    },
) : HostResolver, AutoCloseable {
    private val closed = AtomicBoolean()
    private val pending = ConcurrentHashMap.newKeySet<CancellableContinuation<LanResult<List<IpLiteral>>>>()
    private val worker = ThreadPoolExecutor(2, 2, 0, TimeUnit.MILLISECONDS, SynchronousQueue(),
        { runnable -> Thread(runnable, "lan-tools-dns").apply { isDaemon = true } }, ThreadPoolExecutor.AbortPolicy())

    override suspend fun resolve(host: String): LanResult<List<IpLiteral>> = suspendCancellableCoroutine { continuation ->
        pending += continuation
        continuation.invokeOnCancellation { pending -= continuation }
        if (closed.get()) {
            pending -= continuation
            continuation.resume(LanResult.Failure(LanError.NETWORK_UNAVAILABLE))
            return@suspendCancellableCoroutine
        }
        try {
            worker.execute {
                try {
                    if (!continuation.isActive) return@execute
                    val result = try {
                        val addresses = lookup(host)
                        if (addresses.size > 32) LanResult.Failure(LanError.RESOURCE_LIMIT)
                        else if (addresses.isEmpty()) LanResult.Failure(LanError.RESOLUTION_FAILED)
                        else LanResult.Success(addresses.distinct().toList())
                    } catch (_: UnknownHostException) { LanResult.Failure(LanError.RESOLUTION_FAILED) }
                    catch (_: SecurityException) { LanResult.Failure(LanError.PERMISSION_DENIED) }
                    catch (_: Exception) { LanResult.Failure(LanError.IO) }
                    // A late answer belongs to an expired caller and must not update a new operation.
                    continuation.resume(result)
                } finally { pending -= continuation }
            }
        } catch (_: RejectedExecutionException) {
            pending -= continuation
            continuation.resume(LanResult.Failure(if (closed.get()) LanError.NETWORK_UNAVAILABLE else LanError.RESOURCE_LIMIT))
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            pending.toList().forEach { it.cancel() }
            worker.shutdownNow()
        }
    }
}
