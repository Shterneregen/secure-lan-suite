package com.shterneregen.securelan.androidclient.lantools.network

import android.net.DnsResolver
import android.os.Build
import android.os.CancellationSignal
import androidx.annotation.RequiresApi
import com.shterneregen.securelan.common.net.address.IpLiteral
import com.shterneregen.securelan.lantools.diagnostics.HostResolver
import com.shterneregen.securelan.lantools.diagnostics.JvmHostResolver
import com.shterneregen.securelan.lantools.model.*
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Semaphore
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/** API 29+ cancels platform queries; API 26–28 shares Desktop's bounded native-worker policy. */
class AndroidHostResolver : HostResolver, AutoCloseable {
    private val legacy = JvmHostResolver()
    private val modern = if (Build.VERSION.SDK_INT >= 29) ModernResolver() else null
    override suspend fun resolve(host: String) = if (Build.VERSION.SDK_INT >= 29) requireNotNull(modern).resolve(host) else legacy.resolve(host)
    override fun close() { if (Build.VERSION.SDK_INT >= 29) modern?.close(); legacy.close() }
}

@RequiresApi(29)
private class ModernResolver : HostResolver, AutoCloseable {
    private val permits = Semaphore(4)
    private val closed = AtomicBoolean()
    private val pending = ConcurrentHashMap<CancellationSignal, CancellableContinuation<LanResult<List<IpLiteral>>>>()
    override suspend fun resolve(host: String): LanResult<List<IpLiteral>> {
        if (closed.get()) return LanResult.Failure(LanError.NETWORK_UNAVAILABLE)
        if (!permits.tryAcquire()) return LanResult.Failure(LanError.RESOURCE_LIMIT)
        try {
            return suspendCancellableCoroutine { continuation ->
                val signal = CancellationSignal()
                val completed = AtomicBoolean()
                pending[signal] = continuation
                continuation.invokeOnCancellation { completed.set(true); signal.cancel(); pending.remove(signal) }
                if (closed.get()) { continuation.cancel(); return@suspendCancellableCoroutine }
                fun complete(result: LanResult<List<IpLiteral>>) { pending.remove(signal); if (completed.compareAndSet(false, true)) continuation.resume(result) }
                try {
                    // null Network means system default, independently of the TCP source picker.
                    DnsResolver.getInstance().query(null, host, DnsResolver.FLAG_EMPTY, { it.run() }, signal,
                        object : DnsResolver.Callback<List<InetAddress>> {
                            override fun onAnswer(answer: List<InetAddress>, rcode: Int) {
                                val addresses = answer.mapNotNull { IpLiteral.parse(it.hostAddress.orEmpty()) }.distinct()
                                complete(if (answer.size > 32) LanResult.Failure(LanError.RESOURCE_LIMIT)
                                    else if (rcode != 0 || addresses.isEmpty()) LanResult.Failure(LanError.RESOLUTION_FAILED)
                                    else LanResult.Success(addresses))
                            }
                            override fun onError(error: DnsResolver.DnsException) { complete(LanResult.Failure(LanError.RESOLUTION_FAILED)) }
                        })
                } catch (_: SecurityException) { complete(LanResult.Failure(LanError.PERMISSION_DENIED)) }
                catch (_: Exception) { complete(LanResult.Failure(LanError.IO)) }
            }
        } finally { permits.release() }
    }
    override fun close() { closed.set(true); pending.values.toList().forEach { it.cancel() }; pending.clear() }
}
