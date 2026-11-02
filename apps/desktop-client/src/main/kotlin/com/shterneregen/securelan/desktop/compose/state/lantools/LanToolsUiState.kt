package com.shterneregen.securelan.desktop.compose.state.lantools

import com.shterneregen.securelan.common.net.address.IpLiteral
import com.shterneregen.securelan.lantools.model.*
import java.util.UUID

/** The utility owns no room, peer, chat draft, transfer or call state. */
enum class LanToolsTab { DEVICES, DIAGNOSTICS }

/** Editor input remains in memory when the temporary utility closes. Cancel explicitly discards it. */
data class LanDeviceDraft(
    val id: UUID = UUID.randomUUID(), val name: String = "", val address: String = "",
    val mac: String = "", val broadcast: String = "", val port: String = "9", val preferredInterface: String = "",
    val hostname: String = "",
) {
    /** Parses input before persistence or network operations; messages are safe application text. */
    fun validated(): SavedLanDevice {
        val displayName = name.trim()
        require(displayName.length in 1..128 && displayName.none(Char::isISOControl)) { "Enter a name of 1–128 characters without control characters." }
        val ip = address.trim().takeIf(String::isNotEmpty)?.let {
            requireNotNull(IpLiteral.parse(it)?.takeIf { value -> value.isUnicast }) { "Enter a valid device IP address, or leave it empty." }
        }
        val rawMac = mac.trim()
        val wake = if (rawMac.isEmpty()) {
            require(broadcast.isBlank() && preferredInterface.isBlank() && port == "9") { "Enter a MAC address before configuring Wake-on-LAN." }
            null
        } else {
            val parsed = MacAddress.parse(rawMac)
            require(parsed != null && parsed.isWakeTarget) { "Enter a valid nonzero unicast MAC address." }
            val destination = broadcast.trim().takeIf(String::isNotEmpty)?.let {
                requireNotNull(IpLiteral.parse(it)?.takeIf { value -> value.isIpv4 && value.value != "0.0.0.0" }) { "Enter an IPv4 broadcast address, or leave it empty." }
            }
            val udpPort = port.toIntOrNull()?.takeIf { it in 1..65535 }
            require(udpPort != null) { "Enter a UDP port from 1 to 65535." }
            val preferred = preferredInterface.trim().takeIf(String::isNotEmpty)
            require(preferred == null || preferred.length <= 256 && preferred.none(Char::isISOControl)) { "The preferred interface is invalid." }
            WakeConfiguration(parsed, destination, preferred, udpPort)
        }
        return SavedLanDevice(id, displayName, ip, hostname.takeIf(String::isNotEmpty), wake)
    }

    companion object {
        fun from(device: SavedLanDevice) = LanDeviceDraft(device.id, device.displayName, device.addressHint?.value.orEmpty(),
            device.wake?.mac?.value.orEmpty(), device.wake?.broadcast?.value.orEmpty(),
            (device.wake?.port ?: 9).toString(), device.wake?.preferredInterfaceKey.orEmpty(), device.hostnameHint.orEmpty())
    }
}

/** Immutable rendering snapshot. Mutations and jobs belong to DesktopLanToolsController. */
data class LanToolsUiState(
    val visible: Boolean = false, val tab: LanToolsTab = LanToolsTab.DEVICES,
    val loaded: Boolean = false, val loading: Boolean = false, val saving: Boolean = false,
    val devices: List<SavedLanDevice> = emptyList(), val selectedId: UUID? = null,
    val storageError: String? = null, val storageWarning: String? = null,
    val notice: String? = null,
    val editor: LanDeviceDraft? = null, val editorError: String? = null, val deletingId: UUID? = null,
    val networks: LanNetworkSnapshot = LanNetworkSnapshot(emptyList()), val networkLoading: Boolean = false,
    val networkError: String? = null, val bindingKey: String? = null,
    val wakingId: UUID? = null, val lastResults: Map<UUID, String> = emptyMap(),
) {
    val selected: SavedLanDevice? get() = devices.firstOrNull { it.id == selectedId }
    val canModify: Boolean get() = loaded && !loading && !saving && storageWarning == null
}
