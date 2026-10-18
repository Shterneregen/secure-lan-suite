package com.shterneregen.securelan.androidclient.network.discovery

import android.annotation.SuppressLint
import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.shterneregen.securelan.chat.discovery.HostedRoomAdvertisement
import com.shterneregen.securelan.chat.discovery.mdns.DnsSdRecordCodec
import java.util.UUID
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

enum class NsdFailure { PERMISSION_DENIED, PLATFORM, LIMIT }

interface NsdBackend {
    fun browse(networkKey: String?, resolved: (NsdFacade.Resolved) -> Unit, lost: (String) -> Unit, failed: (NsdFailure) -> Unit): NsdFacade.Browser
    fun register(value: HostedRoomAdvertisement, networkKey: String?, registered: () -> Unit, failed: (NsdFailure) -> Unit): NsdFacade.Registration
    fun close()
}

/** Serializes legacy resolution and shields callers from platform callback/thread ownership. */
@Suppress("DEPRECATION")
class NsdFacade(private val context: Context) : NsdBackend {
    data class Resolved(val name: String, val port: Int, val addresses: List<String>, val txt: Map<String, ByteArray>, val routeKey: String?)
    private val manager = context.getSystemService(NsdManager::class.java)
    // Legacy resolve cannot be cancelled: this gate survives browse generations until the terminal callback.
    private var legacyBusy = false
    private val owner: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { Thread(it, "android-nsd").apply { isDaemon = true } }
    private val queuedCallbacks = java.util.concurrent.atomic.AtomicInteger()
    private val overflowed = java.util.concurrent.atomic.AtomicBoolean()
    private val overflowHandlers = java.util.concurrent.ConcurrentHashMap<Any, () -> Unit>()
    private fun dispatch(control: Boolean = false, block: () -> Unit) {
        if (!control && queuedCallbacks.incrementAndGet() > 512) {
            queuedCallbacks.decrementAndGet()
            if (overflowed.compareAndSet(false, true)) overflowHandlers.values.toList().forEach { it() }
            return
        }
        val task = { try { block() } finally { if (!control) queuedCallbacks.decrementAndGet() }; Unit }
        try { owner.execute(task) } catch (_: java.util.concurrent.RejectedExecutionException) { task() }
    }
    private val callbackExecutor = Executor { command -> dispatch { command.run() } }

    interface Browser : AutoCloseable
    interface Registration : AutoCloseable

    @SuppressLint("NewApi") // Centralized extension capability check also covers backported NSD methods.
    override fun browse(networkKey: String?, resolved: (Resolved) -> Unit, lost: (String) -> Unit, failed: (NsdFailure) -> Unit): Browser {
        val network = networkKey?.let { requireNotNull(AndroidLanNetworkMonitor(context).network(it)) { "Selected LAN is unavailable" } }
        val capabilities = NsdApiCapabilities.current()
        val candidates = linkedMapOf<String, NsdServiceInfo>()
        val subscriptions = linkedMapOf<String, NsdManager.ServiceInfoCallback>()
        val queue = ArrayDeque<String>()
        var closed = false
        var activeLegacy: NsdManager.ResolveListener? = null
        var watchdog: java.util.concurrent.ScheduledFuture<*>? = null
        val overflowKey = Any()
        overflowed.set(false)
        overflowHandlers[overflowKey] = { failed(NsdFailure.PLATFORM) }
        fun publish(info: NsdServiceInfo) {
            if (closed || info.serviceName !in candidates) return
            val addresses = if (capabilities.serviceUpdates) info.hostAddresses else listOfNotNull(info.host)
            resolved(Resolved(info.serviceName, info.port, addresses.take(8).mapNotNull { it.hostAddress },
                info.attributes.entries.take(13).associate { it.key to it.value.copyOf() },
                (if (capabilities.networkScoped) info.network else null)?.toString() ?: network?.toString()))
        }
        fun pump() {
            if (closed || legacyBusy || queue.isEmpty()) return
            val name = queue.removeFirst()
            val candidate = candidates[name] ?: return
            legacyBusy = true
            val listener = object : NsdManager.ResolveListener {
                override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) { dispatch(control = true) { legacyBusy = false; activeLegacy = null; watchdog?.cancel(false); if (!closed) failed(NsdFailure.PLATFORM) } }
                override fun onServiceResolved(serviceInfo: NsdServiceInfo) { dispatch(control = true) {
                    legacyBusy = false; activeLegacy = null; watchdog?.cancel(false)
                    if (candidates[name] === candidate) publish(serviceInfo)
                } }
            }
            activeLegacy = listener
            watchdog = owner.schedule({ if (activeLegacy === listener && !closed) failed(NsdFailure.PLATFORM) }, 5, TimeUnit.SECONDS)
            try { manager.resolveService(candidate, listener) } catch (error: Exception) {
                legacyBusy = false; activeLegacy = null; watchdog?.cancel(false)
                failed(if (error is SecurityException) NsdFailure.PERMISSION_DENIED else NsdFailure.PLATFORM)
            }
        }
        val discovery = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) = Unit
            override fun onDiscoveryStopped(serviceType: String) = Unit
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) { dispatch { if (!closed) failed(NsdFailure.PLATFORM) } }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) { dispatch { if (!closed) failed(NsdFailure.PLATFORM) } }
            override fun onServiceFound(info: NsdServiceInfo) { dispatch {
                if (closed || info.serviceName in candidates || info.serviceName.length > 255) return@dispatch
                if (candidates.size >= 32) {
                    failed(NsdFailure.LIMIT)
                    val old = candidates.keys.first()
                    candidates.remove(old); queue.remove(old)
                    subscriptions.remove(old)?.let { runCatching { manager.unregisterServiceInfoCallback(it) } }
                    lost(old)
                }
                candidates[info.serviceName] = info
                if (capabilities.serviceUpdates) {
                    val callback = object : NsdManager.ServiceInfoCallback {
                        override fun onServiceInfoCallbackRegistrationFailed(errorCode: Int) { subscriptions.remove(info.serviceName); if (!closed) failed(NsdFailure.PLATFORM) }
                        override fun onServiceUpdated(serviceInfo: NsdServiceInfo) { if (subscriptions[info.serviceName] === this) publish(serviceInfo) }
                        override fun onServiceLost() {
                            if (subscriptions[info.serviceName] !== this) return
                            candidates.remove(info.serviceName); subscriptions.remove(info.serviceName)
                            runCatching { manager.unregisterServiceInfoCallback(this) }
                            if (!closed) lost(info.serviceName)
                        }
                        override fun onServiceInfoCallbackUnregistered() = Unit
                    }
                    subscriptions[info.serviceName] = callback
                    runCatching { manager.registerServiceInfoCallback(info, callbackExecutor, callback) }.onFailure {
                        subscriptions.remove(info.serviceName)
                        failed(if (it is SecurityException) NsdFailure.PERMISSION_DENIED else NsdFailure.PLATFORM)
                    }
                } else { queue += info.serviceName; pump() }
            } }
            override fun onServiceLost(info: NsdServiceInfo) { dispatch {
                candidates.remove(info.serviceName); queue.remove(info.serviceName)
                subscriptions.remove(info.serviceName)?.let { runCatching { manager.unregisterServiceInfoCallback(it) } }
                if (!closed) lost(info.serviceName)
            } }
        }
        val ticker = owner.scheduleWithFixedDelay({ if (!closed && !capabilities.serviceUpdates) pump() }, 100, 100, TimeUnit.MILLISECONDS)
        val refresh = owner.scheduleWithFixedDelay({ if (!closed && !capabilities.serviceUpdates) candidates.keys.forEach { if (it !in queue) queue += it } }, 60, 60, TimeUnit.SECONDS)
        dispatch {
            try {
                if (capabilities.networkScoped) manager.discoverServices(DnsSdRecordCodec.ANDROID_SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, network, callbackExecutor, discovery)
                else manager.discoverServices(DnsSdRecordCodec.ANDROID_SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discovery)
            } catch (error: Exception) { failed(if (error is SecurityException) NsdFailure.PERMISSION_DENIED else NsdFailure.PLATFORM) }
        }
        return object : Browser {
            override fun close() { dispatch(control = true) {
                if (closed) return@dispatch
                closed = true; ticker.cancel(false); refresh.cancel(false); watchdog?.cancel(false)
                overflowHandlers.remove(overflowKey)
                runCatching { manager.stopServiceDiscovery(discovery) }
                subscriptions.values.forEach { runCatching { manager.unregisterServiceInfoCallback(it) } }
                subscriptions.clear(); candidates.clear(); queue.clear()
                if (capabilities.serviceUpdates) activeLegacy?.let { runCatching { manager.stopServiceResolution(it) } }
            } }
        }
    }

    @SuppressLint("NewApi")
    override fun register(value: HostedRoomAdvertisement, networkKey: String?, registered: () -> Unit, failed: (NsdFailure) -> Unit): Registration {
        val network = networkKey?.let { requireNotNull(AndroidLanNetworkMonitor(context).network(it)) { "Selected LAN is unavailable" } }
        lateinit var listener: NsdManager.RegistrationListener
        val lease = NsdRegistrationLease({ runCatching { manager.unregisterService(listener) } }, registered)
        val info = NsdServiceInfo().apply {
            serviceName = "SecureLan-${value.roomInstanceId.replace("-", "").take(12)}-${UUID.randomUUID().toString().take(4)}"
            serviceType = DnsSdRecordCodec.ANDROID_SERVICE_TYPE
            port = value.chatPort
            if (NsdApiCapabilities.current().networkScoped) setNetwork(network)
            DnsSdRecordCodec.encode(value).forEach { (key, bytes) -> setAttribute(key, bytes.toString(Charsets.UTF_8)) }
        }
        listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) { dispatch(control = true) {
                lease.registered()
            } }
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) { dispatch(control = true) { if (!lease.isClosed) failed(NsdFailure.PLATFORM) } }
            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) = Unit
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = Unit
        }
        dispatch { if (!lease.isClosed) runCatching { manager.registerService(info, NsdManager.PROTOCOL_DNS_SD, listener) }.onFailure { failed(if (it is SecurityException) NsdFailure.PERMISSION_DENIED else NsdFailure.PLATFORM) } }
        return object : Registration {
            override fun close() { dispatch(control = true) { lease.close() } }
        }
    }

    /** Called only when the owning ViewModel/service ends, after closing its leases. */
    override fun close() { owner.shutdown() }
}
