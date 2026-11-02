package com.shterneregen.securelan.androidclient.lantools.network

import com.shterneregen.securelan.androidclient.network.discovery.*
import com.shterneregen.securelan.common.net.address.IpLiteral
import com.shterneregen.securelan.lantools.discovery.*
import com.shterneregen.securelan.lantools.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import java.time.Clock
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

/** Android requires an explicit network when several LANs exist; no process-wide network is selected. */
fun interface AndroidDeviceBrowser {
    suspend fun browse(bindingKey: String?, update: suspend (LanDiscoverySnapshot) -> Unit): LanResult<LanDiscoverySnapshot>
}

/** Testable NSD port. The tool never owns or closes the room facade. */
fun interface LanNsdBrowser {
    fun open(type: String, networkKey: String?, resolved: (NsdFacade.Resolved) -> Unit, lost: (String) -> Unit, failed: (NsdFailure) -> Unit): NsdFacade.Browser
}

/**
 * One manual thirty-second browse, six fixed types and one explicitly chosen LAN. Legacy APIs cannot
 * reliably scope discovery: multiple LANs/VPN are unsupported there. NSD through compileSdk 35 has
 * no SRV hostname getter; hostname stays Unknown instead of reverse-resolving every address.
 */
class AndroidLanDeviceDiscovery(
    private val platform: AndroidLanPlatform,
    private val nsd: LanNsdBrowser,
    private val multicast: MulticastLeaseOwner,
    private val capabilities: NsdApiCapabilities,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val clock: Clock = Clock.systemUTC(),
    private val nanoTime: () -> Long = System::nanoTime,
    private val durationMillis: Long = 30_000,
) : AndroidDeviceBrowser {
    init { require(durationMillis in 1..30_000) }
    private val gate = Mutex()
    private var retiring: List<NsdFacade.Browser> = emptyList()
    private data class Incoming(val type: String, val value: NsdFacade.Resolved?, val lost: String?, val seen: Instant, val nanos: Long)

    override suspend fun browse(bindingKey: String?, update: suspend (LanDiscoverySnapshot) -> Unit): LanResult<LanDiscoverySnapshot> = withContext(io) {
        if (!gate.tryLock()) return@withContext LanResult.Failure(LanError.RESOURCE_LIMIT)
        val handles = mutableListOf<NsdFacade.Browser>()
        val queue = Channel<Incoming>(256)
        val accepting = AtomicBoolean(true)
        val limited = AtomicBoolean()
        val denied = AtomicBoolean()
        val registry = LanDeviceRegistry()
        var networkChanged = false
        try {
            // A timed-out stop acknowledgement must not permit another generation of native handles.
            if (retiring.isNotEmpty()) {
                if (withTimeoutOrNull(1) { retiring.forEach { it.awaitClosed() }; true } != true) return@withContext LanResult.Failure(LanError.RESOURCE_LIMIT)
                retiring = emptyList()
            }
            val links = platform.links().filter { it.binding.up && it.binding.address.isIpv4 && it.binding.address.isUnicast }
            val networks = links.map { it.binding.interfaceKey }.distinct()
            val selected = if (bindingKey != null) links.singleOrNull { it.binding.key == bindingKey }
                ?: return@withContext LanResult.Failure(LanError.INTERFACE_UNAVAILABLE)
            else if (networks.size > 1) return@withContext LanResult.Failure(LanError.INTERFACE_SELECTION_REQUIRED)
            else links.firstOrNull() ?: return@withContext LanResult.Failure(LanError.NETWORK_UNAVAILABLE)
            if (!capabilities.networkScoped && (networks.size != 1 || platform.hasVpn())) return@withContext LanResult.Failure(LanError.UNSUPPORTED)
            update(registry.snapshot(nanoTime()))
            multicast.acquire()
            for (type in LanServiceTypes.supported) {
                currentCoroutineContext().ensureActive()
                handles += nsd.open(type.removeSuffix("local."), selected.binding.interfaceKey.takeIf { capabilities.networkScoped }, { service ->
                    if (accepting.get() && queue.trySend(Incoming(type, service, null, clock.instant(), nanoTime())).isFailure) limited.set(true)
                }, { name ->
                    if (accepting.get() && queue.trySend(Incoming(type, null, name, clock.instant(), nanoTime())).isFailure) limited.set(true)
                }, { failure -> if (accepting.get()) { limited.set(true); if (failure == NsdFailure.PERMISSION_DENIED) denied.set(true) } })
            }
            withTimeoutOrNull(durationMillis) {
                while (true) {
                    currentCoroutineContext().ensureActive()
                    if (denied.get()) return@withTimeoutOrNull
                    if (platform.links().none { it.binding == selected.binding }) {
                        networkChanged = true
                        registry.resetScope(selected.binding.key)
                        update(registry.snapshot(nanoTime()))
                        return@withTimeoutOrNull
                    }
                    repeat(256) {
                        val event = queue.tryReceive().getOrNull() ?: return@repeat
                        val key = LanServiceKey(selected.binding.key, event.type, event.value?.name ?: event.lost.orEmpty())
                        if (event.lost != null) registry.remove(key)
                        else event.value?.let { value ->
                            if (value.routeKey != null && value.routeKey != selected.binding.interfaceKey) { registry.markPartial(); return@let }
                            val addresses = value.addresses.take(16).mapNotNull(IpLiteral::parse).filter {
                                it.isIpv4 && it.isUnicast && selected.binding.prefix.contains(it) && !selected.binding.prefix.isDirectedBroadcast(it)
                            }
                            if (value.addresses.size > 16) registry.markPartial()
                            if (addresses.isEmpty()) registry.remove(key)
                            else registry.upsert(LanServiceObservation(key, null, addresses, value.port, event.seen, event.nanos))
                        }
                    }
                    if (limited.getAndSet(false)) registry.markPartial()
                    update(registry.snapshot(nanoTime()))
                    delay(250)
                }
            }
            currentCoroutineContext().ensureActive()
            // A network can disappear between the final polling tick and the deadline.
            if (platform.links().none { it.binding == selected.binding }) {
                networkChanged = true
                registry.resetScope(selected.binding.key)
            }
            if (limited.get()) registry.markPartial()
            if (denied.get()) LanResult.Failure(LanError.PERMISSION_DENIED)
            else if (networkChanged) {
                update(registry.snapshot(nanoTime(), stale = true))
                LanResult.Failure(LanError.NETWORK_CHANGED)
            }
            else registry.snapshot(nanoTime(), stale = true).let { update(it); LanResult.Success(it) }
        } catch (cancel: CancellationException) { throw cancel }
        catch (_: SecurityException) { LanResult.Failure(LanError.PERMISSION_DENIED) }
        catch (_: Exception) { LanResult.Failure(LanError.IO) }
        finally {
            accepting.set(false); queue.close()
            withContext(NonCancellable) {
                handles.forEach { runCatching { it.close() } }
                // Await all stop acknowledgements under one cleanup budget; late callbacks remain invalid.
                if (handles.isNotEmpty() && runCatching { withTimeoutOrNull(2_000) { handles.forEach { it.awaitClosed() }; true } }.getOrNull() != true) retiring = handles.toList()
                runCatching { multicast.close() }
            }
            gate.unlock()
        }
    }
}
