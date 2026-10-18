package com.shterneregen.securelan.chat.discovery.impl

import com.shterneregen.securelan.chat.discovery.*
import java.time.Instant
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Each run owns one serial reducer. Upserts coalesce; removal/reset have a separate bounded channel. */
class CompositePeerDiscoveryService(private val providers: List<DiscoveryProvider>) : RoomDiscoveryService {
    private class Run(val request: DiscoveryStartRequest, val listener: PeerDiscoveryListener, val publish: (DiscoverySnapshot) -> Unit) {
        val owner = Executors.newSingleThreadScheduledExecutor { Thread(it, "room-discovery").apply { isDaemon = true } }
        val registry = DiscoveryRegistry()
        val pending = linkedMapOf<ObservationKey, DiscoveryEvent.Upsert>()
        val removals = linkedSetOf<ObservationKey>()
        val resets = linkedMapOf<DiscoveryProviderId, DiscoveryEvent.ResetSource>()
        val statuses = linkedMapOf<DiscoveryProviderId, DiscoveryEvent.ProviderState>()
        var previous = emptyList<DiscoveredPeer>()
        val retries = mutableMapOf<DiscoveryProviderId, Pair<Int, Long>>()
        val failed = mutableSetOf<DiscoveryProviderId>()
    }
    @Volatile private var active: Run? = null
    private val lifecycle = java.util.concurrent.atomic.AtomicLong()
    @Volatile private var detailed = DiscoverySnapshot()
    @Volatile private var advertised: HostedRoomAdvertisement? = null
    override fun detailedSnapshot(): DiscoverySnapshot = detailed
    override fun isRunning(): Boolean = active != null
    override fun snapshot(): List<DiscoveredPeer> = legacy(detailed)

    override fun start(config: PeerDiscoveryConfig, listener: PeerDiscoveryListener) {
        val host = if (config.chatPort in 1..65535 && config.filePort in 1..65535 && DiscoveryValidation.uuid(config.peerId))
            HostedRoomAdvertisement(config.peerId, UUID.randomUUID().toString(), config.nickname.takeIf(DiscoveryValidation::displayName), config.chatPort, config.filePort, config.announceEnabled) else null
        startSession(DiscoveryStartRequest(config, host), listener) {}
    }

    @Synchronized override fun startSession(request: DiscoveryStartRequest, listener: PeerDiscoveryListener, onSnapshot: (DiscoverySnapshot) -> Unit) {
        stop()
        lifecycle.incrementAndGet()
        val run = Run(request, listener, onSnapshot)
        active = run
        advertised = request.hostedRoom
        run.owner.scheduleWithFixedDelay({ drain(run) }, 0, 100, TimeUnit.MILLISECONDS)
        providers.forEach { provider ->
            admit(run, DiscoveryEvent.ProviderState(DiscoveryProviderState(provider.id, ProviderAvailability.STARTING)))
            try { provider.start(request) { admit(run, it) } }
            catch (_: Exception) { admit(run, DiscoveryEvent.ProviderState(DiscoveryProviderState(provider.id, ProviderAvailability.FAILED))) }
        }
    }

    private fun admit(run: Run, event: DiscoveryEvent) {
        if (active !== run) return
        synchronized(run) {
            when (event) {
                is DiscoveryEvent.Upsert -> {
                    val key = event.observation.key
                    if (key in run.pending || run.pending.size < 512) {
                        run.pending[key] = event; run.removals.remove(key)
                    } else {
                        run.pending.keys.removeAll { it.provider == key.provider }
                        run.resets[key.provider] = DiscoveryEvent.ResetSource(key.provider)
                        run.statuses[key.provider] = DiscoveryEvent.ProviderState(DiscoveryProviderState(key.provider, ProviderAvailability.FAILED, true))
                    }
                }
                is DiscoveryEvent.Remove -> {
                    run.pending.remove(event.key)
                    if (run.removals.size < 1024) run.removals += event.key else run.resets[event.key.provider] = DiscoveryEvent.ResetSource(event.key.provider)
                }
                is DiscoveryEvent.ResetSource -> {
                    run.pending.keys.removeAll { it.provider == event.provider && (event.networkKey == null || it.networkKey == event.networkKey) }
                    // A full provider reset safely subsumes any number of network resets.
                    run.resets[event.provider] = if (run.resets.containsKey(event.provider)) DiscoveryEvent.ResetSource(event.provider) else event
                }
                is DiscoveryEvent.ProviderState -> run.statuses[event.state.provider] = event
            }
        }
    }

    private fun drain(run: Run) {
        if (active !== run) return
        val events = synchronized(run) {
            (run.resets.values.toList() + run.removals.map(DiscoveryEvent::Remove) + run.statuses.values + run.pending.values).also {
                run.resets.clear(); run.removals.clear(); run.statuses.clear(); run.pending.clear()
            }
        }
        events.forEach { event ->
            if (event is DiscoveryEvent.ProviderState) {
                val id = event.state.provider
                if (event.state.availability == ProviderAvailability.FAILED) {
                    run.failed += id
                    if (id !in run.retries) run.retries[id] = 0 to (System.nanoTime() + 1_000_000_000L)
                } else if (event.state.availability == ProviderAvailability.RUNNING) {
                    run.failed -= id
                    run.retries.remove(id)
                }
            }
            if (event !is DiscoveryEvent.Upsert || event.observation.key.provider !in run.failed) run.registry.accept(event)
        }
        run.retries.toMap().forEach { (id, retry) ->
            if (System.nanoTime() < retry.second || active !== run) return@forEach
            val provider = providers.first { it.id == id }
            runCatching { provider.stop() }
            run.registry.accept(DiscoveryEvent.ResetSource(id))
            val attempt = retry.first + 1
            val delay = listOf(1, 2, 5, 15, 30)[attempt.coerceAtMost(4)]
            run.retries[id] = attempt to (System.nanoTime() + delay * 1_000_000_000L + java.util.concurrent.ThreadLocalRandom.current().nextLong(200_000_000L))
            runCatching { provider.start(run.request.copy(hostedRoom = advertised)) { admit(run, it) } }
        }
        val next = run.registry.snapshot()
        synchronized(this) {
            if (active !== run || next == detailed) return
            detailed = next
        }
        if (active === run) runCatching { run.publish(next) }
        val peers = legacy(next)
        run.previous.filter { old -> peers.none { it.peerId == old.peerId && it.host == old.host && it.chatPort == old.chatPort } }
            .forEach { if (active === run) runCatching { run.listener.onPeerExpired(it) } }
        peers.forEach { if (active === run) runCatching { run.listener.onPeerDiscovered(it) } }
        run.previous = peers
    }

    override fun updateHostedRoom(value: HostedRoomAdvertisement?) {
        advertised = value
        providers.forEach { provider -> runCatching { provider.updateAdvertisement(value) }.onFailure {
            active?.let { admit(it, DiscoveryEvent.ProviderState(DiscoveryProviderState(provider.id, ProviderAvailability.FAILED))) }
        } }
    }
    override fun setAnnounceEnabled(announceEnabled: Boolean) { updateHostedRoom(advertised?.copy(discoverable = announceEnabled)) }
    @Synchronized override fun stop() {
        val run = active ?: return
        active = null
        val stoppedGeneration = lifecycle.incrementAndGet()
        advertised = null
        providers.forEach { runCatching { it.stop() } }
        detailed = DiscoverySnapshot(providers = providers.map { DiscoveryProviderState(it.id, ProviderAvailability.STOPPED) })
        val empty = detailed
        run.owner.execute {
            if (lifecycle.get() == stoppedGeneration) {
                runCatching { run.publish(empty) }
                run.previous.forEach { runCatching { run.listener.onPeerExpired(it) } }
            }
        }
        run.owner.shutdown()
    }

    override fun close() { stop(); providers.forEach { runCatching { it.close() } } }

    private fun legacy(snapshot: DiscoverySnapshot): List<DiscoveredPeer> = snapshot.rooms.mapNotNull { room ->
        val endpoint = room.endpoints.firstOrNull() ?: return@mapNotNull null
        val file = room.filePortHint ?: return@mapNotNull null
        DiscoveredPeer(room.peerId, room.displayName ?: "Room", endpoint.host, endpoint.chatPort, file, Instant.EPOCH)
    }
}
