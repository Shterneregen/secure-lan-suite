package com.shterneregen.securelan.desktop.lantools

import com.shterneregen.securelan.desktop.compose.state.lantools.*
import com.shterneregen.securelan.lantools.discovery.LanDeviceDiscovery
import com.shterneregen.securelan.lantools.discovery.LanDiscoverySnapshot
import com.shterneregen.securelan.lantools.model.LanError
import com.shterneregen.securelan.lantools.model.LanResult
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Manual browse owner. Opening the utility never starts discovery and closing never touches rooms. */
class DesktopExplorerController(
    private val discovery: LanDeviceDiscovery,
    private val dispatcher: CoroutineDispatcher = LanToolsUiDispatcher,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : AutoCloseable {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val mutable = MutableStateFlow(LanExplorerUiState())
    val state = mutable.asStateFlow()
    private var job: Job? = null
    private var generation = 0L
    private var visible = false
    private var closed = false

    fun open() { if (!closed) visible = true }
    fun query(value: String) { if (!closed) mutable.value = state.value.copy(query = value.take(253)) }
    fun filter(value: LanDeviceFilter) { if (!closed) mutable.value = state.value.copy(filter = value) }
    fun select(id: String?) { if (!closed) mutable.value = state.value.copy(selectedId = id) }

    private fun completedMessage(snapshot: LanDiscoverySnapshot): String {
        val count = snapshot.devices.size
        val found = if (count == 0) "No advertised devices found" else "$count ${if (count == 1) "device" else "devices"} found"
        return "$found · historical, not online status"
    }

    fun refresh() {
        if (closed || !visible || state.value.running) return
        val token = ++generation
        mutable.value = state.value.copy(running = true, phase = DiagnosticPhase.RUNNING,
            snapshot = stale(), message = "Browsing advertised services for up to 30 seconds; backend cleanup may take additional time.")
        job = scope.launch {
            try {
                val result = withTimeoutOrNull(60_000) {
                    withContext(io) {
                        discovery.browse { snapshot -> withContext(dispatcher) {
                            if (visible && token == generation) mutable.value = state.value.copy(snapshot = snapshot)
                        } }
                    }
                }
                if (!visible || token != generation) return@launch
                val snapshot = (result as? LanResult.Success)?.value
                mutable.value = state.value.copy(running = false,
                    snapshot = snapshot?.copy(devices = snapshot.devices.map { it.copy(stale = true) }) ?: stale(),
                    phase = if (snapshot != null) DiagnosticPhase.SUCCESS else DiagnosticPhase.FAILURE,
                    message = if (snapshot != null) completedMessage(snapshot)
                    else when ((result as? LanResult.Failure)?.code) {
                        LanError.RESOURCE_LIMIT -> "The previous browser is still closing. Wait and try Refresh devices again."
                        LanError.PERMISSION_DENIED -> "The OS denied multicast access. Check network permissions and refresh."
                        LanError.NETWORK_CHANGED -> "The LAN changed during discovery. Refresh devices on the current network."
                        LanError.NETWORK_UNAVAILABLE -> "No usable multicast LAN interface was opened. Check Wi-Fi/Ethernet, VPN/firewall restrictions and refresh."
                        else -> "Device discovery failed or timed out. Check your LAN connection and try Refresh devices."
                    })
            } catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) {
                if (visible && token == generation) mutable.value = state.value.copy(running = false, phase = DiagnosticPhase.FAILURE,
                    snapshot = stale(), message = "Could not browse LAN services. Check your network and refresh.")
            }
        }
    }

    /** Invalidates callbacks before cancellation; retained rows are explicitly historical. */
    fun cancel() {
        generation++
        job?.cancel(); job = null
        if (!closed) mutable.value = state.value.copy(running = false, snapshot = stale(),
            phase = if (state.value.running) DiagnosticPhase.CANCELLED else state.value.phase,
            message = if (state.value.running) "Browse cancelled. Results are historical; backend resources are closing." else state.value.message)
    }
    fun dismiss() { visible = false; cancel() }
    private fun stale() = state.value.snapshot.copy(devices = state.value.snapshot.devices.map { it.copy(stale = true) })
    override fun close() { if (!closed) { dismiss(); closed = true; scope.cancel() } }
}
