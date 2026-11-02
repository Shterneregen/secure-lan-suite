package com.shterneregen.securelan.desktop.lantools

import com.shterneregen.securelan.common.net.address.IpLiteral
import com.shterneregen.securelan.desktop.compose.settings.DesktopAppPaths
import com.shterneregen.securelan.lantools.model.MacAddress
import com.shterneregen.securelan.lantools.model.SavedLanDevice
import com.shterneregen.securelan.lantools.model.WakeConfiguration
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.Properties
import java.util.UUID

/** Loaded records may remain viewable when malformed data prevents safe replacement. */
data class SavedDevicesLoad(val devices: List<SavedLanDevice>, val warning: String? = null)

/** Blocking storage boundary; the controller dispatches every call to its IO worker. */
interface SavedLanDeviceStore {
    fun load(): SavedDevicesLoad
    fun save(devices: List<SavedLanDevice>)
}

/**
 * Versioned portable store with bounded decoding and serialized, atomic replacement where supported.
 * Corrupt/future schemas are read-only; external changes detected before writing require a reload.
 * One instance owns the file; concurrent external writers are unsupported. No room credentials,
 * probe results or transient binding keys are persisted.
 */
class PropertiesSavedLanDeviceStore(
    private val path: Path = DesktopAppPaths.settingsPath().resolveSibling("lan-tools-devices.properties"),
    private val replace: (Path, Path) -> Unit = { source, target ->
        try { Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING) }
        catch (_: AtomicMoveNotSupportedException) { Files.move(source, target, StandardCopyOption.REPLACE_EXISTING) }
    },
) : SavedLanDeviceStore {
    private var loaded = false
    private var writable = false
    private var fingerprint: String? = null

    @Synchronized
    override fun load(): SavedDevicesLoad {
        loaded = true
        writable = false
        val bytes = read()
        fingerprint = bytes?.let(::digest)
        if (bytes == null) { writable = true; return SavedDevicesLoad(emptyList()) }
        val properties = try { Properties().also { bytes.inputStream().use(it::load) } }
        catch (_: IllegalArgumentException) { return damaged() }
        if (properties.getProperty("schemaVersion") != "1") return damaged()
        val count = properties.getProperty("count")?.toIntOrNull()?.takeIf { it in 0..MAX_DEVICES } ?: return damaged()
        val devices = mutableListOf<SavedLanDevice>()
        var invalid = false
        repeat(count) { index ->
            val device = runCatching { decode(properties, index) }.getOrNull()
            if (device == null || devices.any { it.id == device.id }) invalid = true else devices += device
        }
        writable = !invalid
        return if (invalid) damaged(devices) else SavedDevicesLoad(devices.toList())
    }

    @Synchronized
    override fun save(devices: List<SavedLanDevice>) {
        if (!loaded || !writable) throw IOException("Store requires a valid reload")
        require(devices.size <= MAX_DEVICES && devices.map { it.id }.distinct().size == devices.size)
        // Refuse stale writes even if another process edited a syntactically valid file.
        if (read()?.let(::digest) != fingerprint) throw IOException("Store changed externally")
        val properties = Properties().apply {
            setProperty("schemaVersion", "1")
            setProperty("count", devices.size.toString())
            devices.forEachIndexed { index, device ->
                val prefix = "device.$index."
                setProperty(prefix + "id", device.id.toString())
                setProperty(prefix + "name", device.displayName)
                device.addressHint?.let { setProperty(prefix + "address", it.value) }
                device.hostnameHint?.let { setProperty(prefix + "hostname", it) }
                device.wake?.let { wake ->
                    require(wake.mac.isWakeTarget && wake.port in 1..65535 && wake.broadcast?.isIpv4 != false)
                    require(wake.preferredInterfaceKey?.let { it.isNotBlank() && it.length <= 256 } != false)
                    setProperty(prefix + "mac", wake.mac.value)
                    setProperty(prefix + "port", wake.port.toString())
                    wake.broadcast?.let { setProperty(prefix + "broadcast", it.value) }
                    wake.preferredInterfaceKey?.let { setProperty(prefix + "interface", it) }
                }
            }
        }
        val bytes = java.io.ByteArrayOutputStream().also { properties.store(it, "Saved LAN devices") }.toByteArray()
        if (bytes.size > MAX_BYTES) throw IOException("Store size limit")
        val absolute = path.toAbsolutePath().normalize()
        Files.createDirectories(absolute.parent)
        val temporary = Files.createTempFile(absolute.parent, "lan-devices-", ".tmp")
        try {
            Files.write(temporary, bytes)
            replace(temporary, absolute)
            fingerprint = digest(bytes)
        } finally { Files.deleteIfExists(temporary) }
    }

    private fun read(): ByteArray? {
        if (!Files.exists(path)) return null
        val bytes = Files.newInputStream(path).use { it.readNBytes(MAX_BYTES + 1) }
        if (bytes.size > MAX_BYTES) throw IOException("Store size limit")
        return bytes
    }

    private fun decode(properties: Properties, index: Int): SavedLanDevice {
        fun field(name: String) = properties.getProperty("device.$index.$name")
        fun address(name: String) = field(name)?.let { requireNotNull(IpLiteral.parse(it)) }
        val mac = field("mac")?.let { requireNotNull(MacAddress.parse(it)).also { value -> require(value.isWakeTarget) } }
        require(mac != null || listOf("broadcast", "interface", "port").all { field(it) == null })
        val wake = mac?.let {
            val port = field("port")?.toIntOrNull()?.takeIf { value -> value in 1..65535 } ?: error("Invalid port")
            val broadcast = address("broadcast").also { value -> require(value?.isIpv4 != false) }
            val preferred = field("interface").also { value -> require(value == null || value.isNotBlank() && value.length <= 256) }
            WakeConfiguration(it, broadcast, preferred, port)
        }
        val hint = address("address").also { require(it == null || it.isUnicast) }
        return SavedLanDevice(UUID.fromString(field("id")), requireNotNull(field("name")), hint, field("hostname"), wake)
    }

    private fun damaged(devices: List<SavedLanDevice> = emptyList()) = SavedDevicesLoad(devices,
        "Some saved data is invalid or uses an unsupported format. Back up and repair the device settings file, then reload. Changes are disabled to protect it.")

    private fun digest(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    companion object {
        const val MAX_DEVICES = 256
        private const val MAX_BYTES = 1_048_576
    }
}
