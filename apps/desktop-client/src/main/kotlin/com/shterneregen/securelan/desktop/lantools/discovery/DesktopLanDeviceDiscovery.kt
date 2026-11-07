package com.shterneregen.securelan.desktop.lantools.discovery

import com.shterneregen.securelan.common.net.address.IpLiteral
import com.shterneregen.securelan.common.net.interfaces.NetworkBinding
import com.shterneregen.securelan.common.net.interfaces.NetworkBindingSelector
import com.shterneregen.securelan.desktop.network.discovery.DesktopNetworkMonitor
import com.shterneregen.securelan.lantools.discovery.*
import com.shterneregen.securelan.lantools.model.LanError
import com.shterneregen.securelan.lantools.model.LanResult
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import java.time.Clock
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.milliseconds

/**
 * One explicit browse owns at most four IPv4 LAN handles. It rechecks topology while listening,
 * serializes callbacks through a bounded queue and rejects events from retired handles. No room
 * provider, advertisement, peer identity or room discovery port participates in this service.
 * A permit remains occupied through blocking JmDNS cleanup, preventing cancel/retry resource growth.
 */
class DesktopLanDeviceDiscovery(
    private val networks: () -> List<NetworkBinding> = DesktopNetworkMonitor()::snapshot,
    private val create: (NetworkBinding) -> LanDnsSdBrowser = ::JmDnsLanBrowser,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clock: Clock = Clock.systemUTC(),
    private val nanoTime: () -> Long = System::nanoTime,
    private val durationMillis: Long = 30_000,
) : LanDeviceDiscovery {
    init { require(durationMillis in 1..30_000) }
    private val gate = Mutex()
    private class Handle(val binding: NetworkBinding, val browser: LanDnsSdBrowser)
    private data class Incoming(val handle: Handle, val event: DnsSdEvent, val seenAt: Instant, val seenNanos: Long)

    override suspend fun browse(update: suspend (LanDiscoverySnapshot) -> Unit): LanResult<LanDiscoverySnapshot> = withContext(dispatcher) {
        if (!gate.tryLock()) return@withContext LanResult.Failure(LanError.RESOURCE_LIMIT)
        val queue = Channel<Incoming>(256)
        val dropped = AtomicBoolean()
        val accepting = AtomicBoolean(true)
        val handles = linkedMapOf<String, Handle>()
        val registry = LanDeviceRegistry()
        var everOpened = false
        try {
            update(registry.snapshot(nanoTime()))
            withTimeoutOrNull(durationMillis.milliseconds) {
                var pollAt = Long.MIN_VALUE
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val now = nanoTime()
                    if (pollAt == Long.MIN_VALUE || now - pollAt >= 2_000_000_000) {
                        pollAt = now
                        val all = networks()
                        val selected = NetworkBindingSelector.select(all).distinctBy { it.interfaceKey }.take(4)
                        if (selected.isEmpty() && !everOpened) break
                        if (all.count { it.up && it.multicast && it.address.isIpv4 && it.address.isUnicast && it.kind == com.shterneregen.securelan.common.net.interfaces.NetworkKind.LAN } > selected.size) registry.markPartial()
                        // Same key with changed prefix/source is a new scope incarnation. Old callbacks are ignored.
                        handles.values.toList().filter { it.binding !in selected }.forEach { old ->
                            registry.markPartial()
                            registry.resetScope(old.binding.key)
                            old.browser.close()
                            handles.remove(old.binding.key)
                        }
                        for (binding in selected) {
                            if (binding.key in handles) continue
                            currentCoroutineContext().ensureActive()
                            var browser: LanDnsSdBrowser? = null
                            try {
                                browser = create(binding)
                                val handle = Handle(binding, browser)
                                handles[binding.key] = handle // Ownership precedes callback registration/cancellation.
                                browser.start { event ->
                                    if (accepting.get() && queue.trySend(Incoming(handle, event, clock.instant(), nanoTime())).isFailure) dropped.set(true)
                                }
                                everOpened = true
                            } catch (cancel: CancellationException) { throw cancel }
                            catch (_: Exception) {
                                browser?.close()
                                handles.remove(binding.key)
                                registry.markPartial()
                            }
                        }
                    }
                    // Bound callback work per UI update so event floods cannot starve deadlines or topology reads.
                    repeat(256) {
                        val incoming = queue.tryReceive().getOrNull() ?: return@repeat
                        if (handles[incoming.handle.binding.key] !== incoming.handle) return@repeat
                        val binding = incoming.handle.binding
                        when (val event = incoming.event) {
                            DnsSdEvent.Limited -> registry.markPartial()
                            is DnsSdEvent.Removed -> registry.remove(LanServiceKey(binding.key, event.type, event.instance))
                            is DnsSdEvent.Resolved -> {
                                val service = event.service
                                val addresses = service.addresses.take(17).mapNotNull(IpLiteral::parse)
                                    .filter { it.isIpv4 && it.isUnicast && binding.prefix.contains(it) && !binding.prefix.isDirectedBroadcast(it) }
                                if (service.addresses.size > 16 || addresses.size != service.addresses.size) registry.markPartial()
                                val key = LanServiceKey(binding.key, service.type, service.instance)
                                if (addresses.isNotEmpty()) registry.upsert(LanServiceObservation(
                                    key, service.hostname?.takeIf(String::isNotBlank), addresses, service.port, incoming.seenAt, incoming.seenNanos))
                                else registry.remove(key)
                            }
                        }
                    }
                    if (dropped.getAndSet(false)) registry.markPartial()
                    update(registry.snapshot(nanoTime()))
                    delay(250.milliseconds)
                }
            }
            currentCoroutineContext().ensureActive()
            if (everOpened) {
                // The final deadline can precede the next periodic topology poll.
                val current = NetworkBindingSelector.select(networks()).distinctBy { it.interfaceKey }.take(4)
                handles.values.filter { it.binding !in current }.forEach {
                    registry.markPartial()
                    registry.resetScope(it.binding.key)
                }
                val completed = registry.snapshot(nanoTime(), stale = true)
                update(completed) // Listening is over even if library cleanup has not finished yet.
                if (handles.values.none { it.binding in current }) LanResult.Failure(LanError.NETWORK_CHANGED)
                else LanResult.Success(completed)
            } else LanResult.Failure(LanError.NETWORK_UNAVAILABLE)
        } catch (cancel: CancellationException) { throw cancel }
        catch (_: SecurityException) { LanResult.Failure(LanError.PERMISSION_DENIED) }
        catch (_: Exception) { LanResult.Failure(LanError.IO) }
        finally {
            accepting.set(false)
            queue.close()
            // JmDNS close is synchronous (including library timeout); never run it on the UI thread.
            // Do not free the gate until all owned handles have actually closed.
            withContext(NonCancellable) { handles.values.forEach { runCatching { it.browser.close() } }; handles.clear() }
            gate.unlock()
        }
    }
}
