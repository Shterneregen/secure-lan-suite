package com.shterneregen.securelan.androidclient.lantools

import android.annotation.SuppressLint
import android.content.Context
import com.shterneregen.securelan.common.net.address.IpLiteral
import com.shterneregen.securelan.lantools.model.*
import java.io.IOException
import java.io.StringReader
import java.io.StringWriter
import java.util.Properties
import java.util.UUID

/** App-owned persistence boundary; callers dispatch blocking reads and commits away from Compose. */
interface LanDeviceStore {
    fun load(): List<SavedLanDevice>
    fun save(devices: List<SavedLanDevice>)
}

/** A separate preference file, bounded versioned payload and synchronous commit on the caller's IO dispatcher. */
class PreferencesSavedLanDeviceStore(private val read: () -> String?, private val write: (String?) -> Boolean) : LanDeviceStore {
    @SuppressLint("UseKtx") // KTX edit returns Unit; the Boolean commit result must reach the ViewModel.
    constructor(context: Context) : this(context.getSharedPreferences("lan-tools-devices", Context.MODE_PRIVATE).let { preferences ->
        { preferences.getString("payload", null) }
    }, context.getSharedPreferences("lan-tools-devices", Context.MODE_PRIVATE).let { preferences ->
        { value: String? -> preferences.edit().putString("payload", value).commit() }
    })
    private var loaded = false
    private var expected: String? = null
    private var recoveryPending = false
    private var failedPayload: String? = null

    @Synchronized override fun load(): List<SavedLanDevice> {
        loaded = false
        recover()
        val raw = read()
        val devices = if (raw == null) emptyList() else SavedLanDeviceCodec.decode(raw)
        expected = raw; loaded = true
        return devices
    }
    @Synchronized override fun save(devices: List<SavedLanDevice>) {
        check(loaded) { "A valid reload is required" }
        recover()
        if (read() != expected) throw IOException("Device preferences changed")
        val encoded = SavedLanDeviceCodec.encode(devices)
        if (!runCatching { write(encoded) }.getOrDefault(false)) {
            // Android commits to memory before disk. Restore the last acknowledged payload so a
            // failed save cannot appear successful on reload or poison the external-change guard.
            recoveryPending = true
            failedPayload = encoded
            runCatching { recover() }
            throw IOException("Preference commit failed")
        }
        expected = encoded
    }

    /** A failed rollback remains unconfirmed; do not expose the process cache as durable storage. */
    private fun recover() {
        if (!recoveryPending) return
        val current = read()
        if (current != expected && current != failedPayload) throw IOException("Device preferences changed during recovery")
        if (!write(expected)) throw IOException("Preference recovery failed")
        recoveryPending = false
        failedPayload = null
    }
}

/** Pure decoder: invalid/future data is read-only until repaired; it is never replaced by an empty list. */
object SavedLanDeviceCodec {
    private const val MAX_BYTES = 1_048_576
    fun decode(raw: String): List<SavedLanDevice> {
        require(raw.length <= MAX_BYTES)
        require(raw.toByteArray(Charsets.UTF_8).size <= MAX_BYTES)
        val data = Properties().apply { load(StringReader(raw)) }
        require(data.getProperty("schema") == "1")
        val count = data.getProperty("count").toInt().also { require(it in 0..256) }
        val devices = (0 until count).map { index ->
            fun field(key: String): String? = data.getProperty("$index.$key")
            fun ip(key: String) = field(key)?.let { requireNotNull(IpLiteral.parse(it)) }
            val mac = field("mac")?.let { requireNotNull(MacAddress.parse(it)).also { value -> require(value.isWakeTarget) } }
            require(mac != null || listOf("broadcast", "port", "interface").all { field(it) == null })
            val wake = mac?.let { WakeConfiguration(it, ip("broadcast"), field("interface"), requireNotNull(field("port")).toInt()) }
            SavedLanDevice(UUID.fromString(field("id")), requireNotNull(field("name")), ip("address"), field("hostname"), wake).also(::validate)
        }
        require(devices.map { it.id }.distinct().size == devices.size)
        return devices
    }
    fun encode(devices: List<SavedLanDevice>): String {
        require(devices.size <= 256 && devices.map { it.id }.distinct().size == devices.size)
        val data = Properties().apply { setProperty("schema", "1"); setProperty("count", devices.size.toString()) }
        devices.forEachIndexed { index, device ->
            validate(device)
            fun put(key: String, value: String?) { if (value != null) data.setProperty("$index.$key", value) }
            put("id", device.id.toString()); put("name", device.displayName); put("address", device.addressHint?.value); put("hostname", device.hostnameHint)
            device.wake?.let { put("mac", it.mac.value); put("broadcast", it.broadcast?.value); put("port", it.port.toString()); put("interface", it.preferredInterfaceKey) }
        }
        return StringWriter().also { data.store(it, "LAN devices") }.toString().also { require(it.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) }
    }
    private fun validate(device: SavedLanDevice) {
        require(device.addressHint?.isUnicast != false)
        device.wake?.let { wake ->
            require(wake.mac.isWakeTarget && wake.port in 1..65535 && wake.broadcast?.isIpv4 != false)
            require(wake.preferredInterfaceKey?.let { it.isNotBlank() && it.length <= 256 && it.none(Char::isISOControl) } != false)
        }
    }
}
