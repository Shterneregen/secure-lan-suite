package com.shterneregen.securelan.chat.discovery.mdns

import com.shterneregen.securelan.chat.discovery.DiscoveryValidation
import com.shterneregen.securelan.chat.discovery.HostedRoomAdvertisement
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.util.Locale

/** Validates application TXT strings, never DNS packets or network addresses. */
object DnsSdRecordCodec {
    const val SERVICE_TYPE = "_securelan._tcp.local."
    const val ANDROID_SERVICE_TYPE = "_securelan._tcp."
    data class Record(val peerId: String, val roomInstanceId: String, val displayName: String?, val filePort: Int?)

    fun encode(value: HostedRoomAdvertisement): Map<String, ByteArray> = linkedMapOf(
        "v" to "1", "proto" to "2", "pid" to value.peerId, "rid" to value.roomInstanceId,
    ).apply {
        value.activeFilePort?.let { put("fp", it.toString()) }
        value.displayName?.let { put("name", it) }
        put("caps", if (value.activeFilePort == null) "chat" else "chat,files")
    }.mapValues { it.value.toByteArray(Charsets.UTF_8) }

    fun decode(entries: List<Pair<String, ByteArray>>, chatPort: Int): Record? {
        if (chatPort !in 1..65535 || entries.size !in 4..12) return null
        if (entries.sumOf { 1 + it.first.length + 1 + it.second.size } > 512) return null
        val fields = linkedMapOf<String, String>()
        for ((key, bytes) in entries) {
            val lower = key.lowercase(Locale.ROOT)
            if (!Regex("[a-z][a-z0-9-]{0,15}").matches(lower) || fields.containsKey(lower) || key.length + 1 + bytes.size > 255) return null
            val value = try {
                Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString()
            } catch (_: Exception) { return null }
            fields[lower] = value
        }
        if (fields["v"] != "1" || fields["proto"] != "2") return null
        val pid = fields["pid"]?.takeIf(DiscoveryValidation::uuid) ?: return null
        val rid = fields["rid"]?.takeIf(DiscoveryValidation::uuid) ?: return null
        val name = fields["name"]
        if (name != null && !DiscoveryValidation.displayName(name)) return null
        val fp = fields["fp"]?.let { DiscoveryValidation.port(it) ?: return null }
        if (fp == chatPort) return null
        if (fields["caps"]?.let { it.length > 64 || it.split(',').size > 8 } == true) return null
        return Record(pid, rid, name, fp)
    }

    fun decode(entries: Map<String, ByteArray>, chatPort: Int): Record? = decode(entries.entries.map { it.key to it.value.copyOf() }, chatPort)
}
