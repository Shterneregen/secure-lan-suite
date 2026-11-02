package com.shterneregen.securelan.lantools.discovery

import com.shterneregen.securelan.lantools.diagnostics.DiagnosticHost
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.UUID

/**
 * Single-owner reducer with monotonic leases and bounded untrusted input. It never merges saved
 * UUIDs or observations across network scopes. Hostname claims alone are insufficient: different
 * services must also share an address to be grouped. IP equality alone never merges hostnames.
 */
class LanDeviceRegistry(private val session: String = UUID.randomUUID().toString()) {
    private val observations = linkedMapOf<LanServiceKey, LanServiceObservation>()
    private var rowIds = emptyMap<LanServiceKey, String>()
    private var nextId = 0
    var partial: Boolean = false
        private set

    fun markPartial() { partial = true }

    /** A resolved observation replaces the backend's full current address set for that service. */
    fun upsert(value: LanServiceObservation) {
        if (!valid(value)) { partial = true; return }
        val key = value.key.copy(type = value.key.type.lowercase(Locale.ROOT), instance = value.key.instance.lowercase(Locale.ROOT))
        if (key !in observations && observations.size >= 512) { partial = true; return }
        observations[key] = value.copy(key = value.key.copy(type = key.type), hostname = value.hostname?.lowercase(Locale.ROOT)?.trimEnd('.'), addresses = value.addresses.distinct().toList())
    }

    fun remove(key: LanServiceKey) { observations.remove(key.copy(type = key.type.lowercase(Locale.ROOT), instance = key.instance.lowercase(Locale.ROOT))) }
    fun resetScope(scope: String) {
        observations.entries.removeAll { it.key.scope == scope }
        rowIds = rowIds.filterKeys { it.scope != scope }
    }

    /** Backend goodbyes remove records immediately; this local lease bounds missed/lost notifications. */
    fun snapshot(nowNanos: Long, stale: Boolean = false): LanDiscoverySnapshot {
        observations.entries.removeAll { nowNanos - it.value.seenNanos >= it.value.leaseMillis * 1_000_000 }
        val groups = mutableListOf<MutableList<LanServiceObservation>>()
        for (record in observations.values) {
            val matching = groups.filter { group -> record.hostname != null && group.any {
                it.key.scope == record.key.scope && it.hostname == record.hostname && it.addresses.any(record.addresses::contains)
            } }
            val merged = mutableListOf(record)
            matching.forEach { merged += it; groups.remove(it) }
            groups += merged
        }
        if (groups.size > 256) partial = true
        val assigned = mutableMapOf<LanServiceKey, String>()
        val usedIds = mutableSetOf<String>()
        val rows = groups.take(256).map { group ->
            val ordered = group.sortedWith(compareBy({ it.key.type }, { it.key.instance }))
            val first = ordered.first()
            val keys = ordered.map { it.key.copy(instance = it.key.instance.lowercase(Locale.ROOT)) }
            val id = keys.mapNotNull(rowIds::get).sorted().firstOrNull { it !in usedIds } ?: "$session:${++nextId}"
            usedIds += id
            keys.forEach { assigned[it] = id }
            DiscoveredLanDevice(id, first.key.scope, first.key.instance, first.hostname,
                ordered.flatMap { it.addresses }.distinct().toList(),
                ordered.map { LanAdvertisedService(it.key.instance, it.key.type, it.port, it.addresses.toList()) },
                ordered.map { it.source }.toSet(), ordered.maxOf { it.seenAt }, stale)
        }
        rowIds = assigned.toMap()
        return LanDiscoverySnapshot(rows, partial)
    }

    private fun valid(value: LanServiceObservation): Boolean =
        value.key.scope.length in 1..512 && value.key.scope.none(Char::isISOControl) &&
            value.key.type.lowercase(Locale.ROOT) in LanServiceTypes.supported &&
            value.key.instance.isNotBlank() && value.key.instance.toByteArray(StandardCharsets.UTF_8).size <= 63 &&
            value.key.instance.none(Char::isISOControl) && value.source == "mDNS/DNS-SD" &&
            value.port in 1..65535 && value.leaseMillis in 1..120_000 &&
            value.addresses.size in 1..16 && value.addresses.all { it.isUnicast } &&
            (value.hostname == null || DiagnosticHost.parse(value.hostname)?.trimEnd('.')?.endsWith(".local") == true)
}
