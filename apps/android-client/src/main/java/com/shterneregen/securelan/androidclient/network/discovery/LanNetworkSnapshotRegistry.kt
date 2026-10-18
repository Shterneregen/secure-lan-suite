package com.shterneregen.securelan.androidclient.network.discovery

import com.shterneregen.securelan.common.net.interfaces.NetworkBinding

internal data class LanNetworkSnapshot<N>(
    val network: N,
    val isLan: Boolean = false,
    val isVpn: Boolean = false,
    val bindings: List<NetworkBinding> = emptyList(),
)

/** Shares one callback registration between owners; readers never acquire an implicit subscription. */
internal class LanNetworkSnapshotRegistry<N>(private val register: (Events<N>) -> AutoCloseable) {
    interface Events<N> {
        fun update(network: N, transform: (LanNetworkSnapshot<N>) -> LanNetworkSnapshot<N>)
        fun remove(network: N)
    }
    private class Run<N> {
        val networks = linkedMapOf<N, LanNetworkSnapshot<N>>()
        val listeners = linkedMapOf<Any, () -> Unit>()
        var registration: AutoCloseable? = null
        var receivedEvents = false
    }
    private val lock = Any()
    private var active: Run<N>? = null

    fun snapshot(): List<LanNetworkSnapshot<N>> = synchronized(lock) { active?.networks?.values?.toList().orEmpty() }
    fun receivedEvents(): Boolean = synchronized(lock) { active?.receivedEvents == true }

    fun subscribe(changed: () -> Unit): AutoCloseable {
        val key = Any()
        val (run, first) = synchronized(lock) {
            val previous = active
            val run = previous ?: Run<N>().also { active = it }
            run.listeners[key] = changed
            run to (previous == null)
        }
        if (first) {
            val events = object : Events<N> {
                override fun update(network: N, transform: (LanNetworkSnapshot<N>) -> LanNetworkSnapshot<N>) {
                    val listeners = synchronized(lock) {
                        if (active !== run) return
                        run.receivedEvents = true
                        run.networks[network] = transform(run.networks[network] ?: LanNetworkSnapshot(network))
                        run.listeners.values.toList()
                    }
                    listeners.forEach { runCatching(it) }
                }
                override fun remove(network: N) {
                    val listeners = synchronized(lock) {
                        if (active !== run) return
                        run.receivedEvents = true
                        if (run.networks.remove(network) == null) return
                        run.listeners.values.toList()
                    }
                    listeners.forEach { runCatching(it) }
                }
            }
            val registration = try { register(events) } catch (error: Exception) {
                synchronized(lock) { if (active === run) active = null; run.networks.clear(); run.listeners.clear() }
                throw error
            }
            val cancelled = synchronized(lock) {
                if (active === run) { run.registration = registration; false } else true
            }
            if (cancelled) registration.close()
        }
        if (snapshot().isNotEmpty()) runCatching(changed)
        return AutoCloseable {
            val registration = synchronized(lock) {
                run.listeners.remove(key)
                if (active === run && run.listeners.isEmpty()) {
                    active = null
                    run.networks.clear()
                    run.registration.also { run.registration = null }
                } else null
            }
            registration?.close()
        }
    }
}
