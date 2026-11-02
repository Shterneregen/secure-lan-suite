package com.shterneregen.securelan.androidclient.lantools

import com.shterneregen.securelan.androidclient.R
import com.shterneregen.securelan.common.net.address.IpLiteral
import com.shterneregen.securelan.lantools.diagnostics.*
import com.shterneregen.securelan.lantools.discovery.LanDiscoverySnapshot
import com.shterneregen.securelan.lantools.model.*
import java.time.Instant
import java.util.UUID

enum class LanTab { DEVICES, DIAGNOSTICS }
enum class LanFilter { ALL, SAVED, DISCOVERED }
enum class LanOperation { NETWORK, DISCOVERY, TCP, DNS, GATEWAY, WAKE }
enum class LanPhase { IDLE, RUNNING, SUCCESS, FAILURE, CANCELLED }

/** Resource identifiers stay local to the application; shared services return only typed data/errors. */
data class LanMessage(val resource: Int, val args: List<String> = emptyList())
data class LanOutcome(val phase: LanPhase = LanPhase.IDLE, val message: LanMessage? = null)
data class LanCheck(val host: String, val port: Int, val binding: String?, val at: Instant, val outcome: LanOutcome)
data class LanDiagnosticInput(val host: String = "", val port: String = "", val dns: String = "", val timeout: String = "5000",
    val family: AddressFamily = AddressFamily.ALL, val gateway: String? = null, val gatewayPort: String = "", val origin: String? = null)

/** Editing a discovery copy never writes until the user confirms Save; MAC is always entered manually. */
data class LanDeviceForm(val id: UUID = UUID.randomUUID(), val name: String = "", val address: String = "", val hostname: String = "",
    val mac: String = "", val broadcast: String = "", val port: String = "9", val preferred: String = "") {
    fun device(): SavedLanDevice {
        fun check(condition: Boolean, message: Int) { if (!condition) throw InvalidLanForm(message) }
        val label = name.trim()
        check(label.length in 1..128 && label.none(Char::isISOControl), R.string.lan_invalid_name)
        val ip = address.trim().takeIf { it.isNotEmpty() }?.let {
            IpLiteral.parse(it)?.takeIf { value -> value.isUnicast } ?: throw InvalidLanForm(R.string.lan_invalid_address)
        }
        val host = hostname.trim().takeIf { it.isNotEmpty() }?.let {
            DiagnosticHost.parse(it) ?: throw InvalidLanForm(R.string.lan_invalid_address)
        }
        val wake = mac.trim().takeIf { it.isNotEmpty() }?.let { raw ->
            val parsed = MacAddress.parse(raw)?.takeIf { it.isWakeTarget } ?: throw InvalidLanForm(R.string.lan_invalid_mac)
            val target = broadcast.trim().takeIf { it.isNotEmpty() }?.let {
                IpLiteral.parse(it)?.takeIf { value -> value.isIpv4 } ?: throw InvalidLanForm(R.string.lan_invalid_broadcast)
            }
            val udpPort = port.toIntOrNull()?.takeIf { it in 1..65535 } ?: throw InvalidLanForm(R.string.lan_invalid_port)
            val preference = preferred.trim().takeIf { it.isNotEmpty() }
            check(preference == null || preference.length <= 256 && preference.none(Char::isISOControl), R.string.lan_invalid_device)
            WakeConfiguration(parsed, target, preference, udpPort)
        }
        return SavedLanDevice(id, label, ip, host, wake)
    }
    companion object {
        fun from(device: SavedLanDevice) = LanDeviceForm(device.id, device.displayName, device.addressHint?.value.orEmpty(), device.hostnameHint.orEmpty(),
            device.wake?.mac?.value.orEmpty(), device.wake?.broadcast?.value.orEmpty(), (device.wake?.port ?: 9).toString(), device.wake?.preferredInterfaceKey.orEmpty())
    }
}
class InvalidLanForm(val resource: Int) : IllegalArgumentException()

/** Entire tool state survives Activity recreation in its own ViewModel; room state is not referenced. */
data class LanToolsState(
    val active: Boolean = false, val tab: LanTab = LanTab.DEVICES,
    val loaded: Boolean = false, val loading: Boolean = false, val saving: Boolean = false,
    val devices: List<SavedLanDevice> = emptyList(), val found: LanDiscoverySnapshot = LanDiscoverySnapshot(),
    val query: String = "", val filter: LanFilter = LanFilter.ALL,
    val selectedSaved: UUID? = null, val selectedFound: String? = null,
    val form: LanDeviceForm? = null, val formError: LanMessage? = null, val deleting: UUID? = null,
    val storeError: LanMessage? = null, val notice: LanMessage? = null,
    val network: DiagnosticsNetworkSnapshot? = null, val binding: String? = null,
    val input: LanDiagnosticInput = LanDiagnosticInput(), val nearbyGranted: Boolean = false,
    val outcomes: Map<LanOperation, LanOutcome> = emptyMap(), val checks: Map<String, LanCheck> = emptyMap(),
) {
    val canModify get() = loaded && !loading && !saving
    val selectedInterface get() = network?.interfaces?.firstOrNull { it.source.binding.key == binding }
    fun outcome(kind: LanOperation) = outcomes[kind] ?: LanOutcome()
    fun matches(values: List<String>): Boolean = query.isBlank() || values.any { it.contains(query.trim(), ignoreCase = true) }
    val savedRows get() = if (filter == LanFilter.DISCOVERED) emptyList() else devices.filter {
        matches(listOfNotNull(it.displayName, it.addressHint?.value, it.hostnameHint))
    }
    val foundRows get() = if (filter == LanFilter.SAVED) emptyList() else found.devices.filter {
        matches(listOfNotNull(it.displayName, it.hostname) + it.addresses.map { ip -> ip.value } + it.services.map { service -> service.type })
    }
}
