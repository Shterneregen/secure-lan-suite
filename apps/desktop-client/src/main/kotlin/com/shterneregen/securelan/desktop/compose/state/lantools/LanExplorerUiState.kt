package com.shterneregen.securelan.desktop.compose.state.lantools

import com.shterneregen.securelan.lantools.discovery.LanDiscoverySnapshot
import com.shterneregen.securelan.lantools.model.SavedLanDevice

enum class LanDeviceFilter { ALL, SAVED, DISCOVERED }

/** Search/filter/selection are session UI state; discovered data is never written into the saved store. */
data class LanExplorerUiState(
    val snapshot: LanDiscoverySnapshot = LanDiscoverySnapshot(),
    val running: Boolean = false,
    val phase: DiagnosticPhase = DiagnosticPhase.IDLE,
    val message: String = "Choose Refresh devices to browse advertised LAN services.",
    val query: String = "",
    val filter: LanDeviceFilter = LanDeviceFilter.ALL,
    val selectedId: String? = null,
) {
    val selected get() = snapshot.devices.firstOrNull { it.id == selectedId }
    val filteredDevices get() = if (filter == LanDeviceFilter.SAVED) emptyList() else snapshot.devices.filter { device ->
        matches(
            buildList {
                addAll(listOfNotNull(device.displayName, device.hostname, device.deviceType, device.scope))
                addAll(device.addresses.map { it.value })
                addAll(device.services.map { it.type })
            },
        )
    }
    fun includes(device: SavedLanDevice): Boolean = filter != LanDeviceFilter.DISCOVERED &&
        matches(listOfNotNull(device.displayName, device.addressHint?.value, device.hostnameHint))
    private fun matches(values: List<String>): Boolean = query.isBlank() || values.any { it.contains(query.trim(), ignoreCase = true) }
}
