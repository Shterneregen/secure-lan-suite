package com.shterneregen.securelan.chat.discovery.impl

import com.shterneregen.securelan.chat.discovery.*

/** Serial-owner reducer. Monotonic receive time is assigned locally, never taken from a packet. */
class DiscoveryRegistry(private val nanoTime: () -> Long = System::nanoTime) {
    private data class Entry(val value: DiscoveryObservation, val seen: Long)
    private val observations = linkedMapOf<ObservationKey, Entry>()
    private val states = linkedMapOf<DiscoveryProviderId, DiscoveryProviderState>()
    private var previous = emptyList<Pair<DiscoveredRoom, Set<ObservationKey>>>()
    private var nextRow = 0L
    private val admissions = mutableMapOf<DiscoveryProviderId, Pair<Long, Double>>()

    fun accept(event: DiscoveryEvent): DiscoverySnapshot {
        when (event) {
            is DiscoveryEvent.Upsert -> {
                val o = event.observation
                val now = nanoTime()
                if (o.key !in observations) {
                    val budget = admissions[o.key.provider]
                    val tokens = if (budget == null) 100.0 else (budget.second + (now - budget.first).coerceAtLeast(0) / 1_000_000_000.0 * 50).coerceAtMost(100.0)
                    if (tokens < 1) {
                        states[o.key.provider] = DiscoveryProviderState(o.key.provider, ProviderAvailability.RUNNING, true)
                        return snapshot()
                    }
                    admissions[o.key.provider] = now to tokens - 1
                    if (observations.size >= 1024) {
                        observations.remove(observations.minBy { it.value.seen }.key)
                        states[o.key.provider] = DiscoveryProviderState(o.key.provider, ProviderAvailability.RUNNING, true)
                    }
                }
                observations[o.key] = Entry(o.copy(endpoints = o.endpoints.toList()), now)
            }
            is DiscoveryEvent.Remove -> observations.remove(event.key)
            is DiscoveryEvent.ResetSource -> observations.keys.removeAll { it.provider == event.provider &&
                (event.generation == null || event.generation == it.generation) && (event.networkKey == null || event.networkKey == it.networkKey) }
            is DiscoveryEvent.ProviderState -> {
                states[event.state.provider] = event.state
                if (event.state.availability in setOf(ProviderAvailability.FAILED, ProviderAvailability.UNAVAILABLE, ProviderAvailability.STOPPED))
                    observations.keys.removeAll { it.provider == event.state.provider }
            }
        }
        return snapshot()
    }

    fun snapshot(): DiscoverySnapshot {
        val now = nanoTime()
        observations.entries.removeAll { (_, entry) -> entry.value.leaseNanos?.let { now - entry.seen >= it } == true }
        val groups = mutableListOf<MutableList<DiscoveryObservation>>()
        for ((_, entries) in observations.values.map { it.value }.groupBy { it.peerId }) {
            val mdns = entries.filter { it.roomInstanceId != null }.groupBy { it.roomInstanceId }.values.map { it.toMutableList() }
            groups.addAll(mdns)
            val udp = entries.filter { it.roomInstanceId == null }
            if (udp.isNotEmpty()) {
                val udpEndpoints = udp.flatMap { it.endpoints }.map { it.address to it.chatPort }.toSet()
                if (mdns.size == 1 && mdns.single().flatMap { it.endpoints }.any { (it.address to it.chatPort) in udpEndpoints }) mdns.single().addAll(udp)
                else groups += udp.toMutableList()
            }
        }
        // Keep the newest bounded groups; observations outside that budget cannot accumulate invisibly.
        val admitted = groups.sortedByDescending { group -> group.maxOf { observations.getValue(it.key).seen } }.take(256)
        val retained = admitted.flatten().map { it.key }.toSet()
        if (admitted.size < groups.size) {
            observations.keys.retainAll(retained)
            states.replaceAll { _, state -> state.copy(limited = true) }
        }
        val usedRows = mutableSetOf<String>()
        val current = admitted.map { group ->
            val keys = group.map { it.key }.toSet()
            val prior = previous.filter { it.first.rowId !in usedRows }.maxByOrNull { (_, oldKeys) -> oldKeys.intersect(keys).size }
                ?.takeIf { it.second.any(keys::contains) }
            val row = prior?.first?.rowId ?: "room-${++nextRow}"
            usedRows += row
            val first = group.firstOrNull { it.roomInstanceId != null } ?: group.first()
            val endpoints = group.flatMap { it.endpoints }.distinct().sortedWith(compareBy<DiscoveryEndpoint> { !it.address.isIpv4 }.thenBy { it.host }).take(8)
            DiscoveredRoom(row, first.peerId, first.roomInstanceId, first.displayName, endpoints,
                group.firstNotNullOfOrNull { it.filePortHint }, group.map { it.key.provider }.toSet()) to keys
        }
        val endpointOwners = current.flatMap { (room, _) -> room.endpoints.map { (it.address to it.chatPort) to room.rowId } }
            .groupBy({ it.first }, { it.second })
        previous = current.map { (room, keys) -> room.copy(metadataConflict = room.endpoints.any { endpointOwners[it.address to it.chatPort].orEmpty().distinct().size > 1 } ||
            current.any { it.first.rowId != room.rowId && it.first.peerId == room.peerId }) to keys }
        return DiscoverySnapshot(previous.map { it.first }.sortedBy { it.rowId }, states.values.toList())
    }
}
