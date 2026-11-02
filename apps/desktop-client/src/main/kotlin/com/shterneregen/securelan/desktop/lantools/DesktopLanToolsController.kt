package com.shterneregen.securelan.desktop.lantools

import com.shterneregen.securelan.desktop.compose.state.lantools.*
import com.shterneregen.securelan.lantools.model.*
import com.shterneregen.securelan.lantools.wol.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext

/**
 * Application-owned utility state. Public actions run on the UI dispatcher; blocking storage uses
 * [io]. Closing the dialog invalidates network callbacks, while committed saves may finish safely.
 * This owner has no reference to the room adapter and cannot disconnect or reset the workspace.
 */
class DesktopLanToolsController(
    private val store: SavedLanDeviceStore,
    private val networks: LanNetworkInfoProvider,
    private val wake: WakeOnLanService,
    dispatcher: CoroutineDispatcher = LanToolsUiDispatcher,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : AutoCloseable {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val mutable = MutableStateFlow(LanToolsUiState())
    val state = mutable.asStateFlow()
    private var networkJob: Job? = null
    private var wakeJob: Job? = null
    private var generation = 0L
    private var closed = false

    fun open() {
        if (closed || state.value.visible) return
        generation++
        update { it.copy(visible = true) }
        if (!state.value.loaded) reload()
        refreshInterfaces()
    }

    /** Keeps drafts/selection but cancels the dialog's network work and rejects late completions. */
    fun dismiss() {
        generation++
        networkJob?.cancel(); wakeJob?.cancel()
        update { value -> value.copy(visible = false, networkLoading = false, wakingId = null,
            lastResults = value.wakingId?.let { value.lastResults + (it to "Send cancelled; a packet may already have been submitted.") } ?: value.lastResults) }
    }

    fun selectTab(tab: LanToolsTab) = update { it.copy(tab = tab) }
    fun selectDevice(id: UUID) = update { it.copy(selectedId = id, bindingKey = if (it.selectedId == id) it.bindingKey else null) }
    fun selectBinding(key: String?) = update { it.copy(bindingKey = key) }
    fun editDraft(draft: LanDeviceDraft) { if (!state.value.saving) update { it.copy(editor = draft, editorError = null) } }
    fun beginAdd() { if (state.value.canModify) update { it.copy(editor = LanDeviceDraft(), editorError = null) } }
    fun beginEdit(id: UUID) {
        val device = state.value.devices.firstOrNull { it.id == id } ?: return
        if (state.value.canModify) update { it.copy(editor = LanDeviceDraft.from(device), editorError = null) }
    }
    fun cancelEditor() { if (!state.value.saving) update { it.copy(editor = null, editorError = null) } }
    fun requestDelete(id: UUID) { if (state.value.canModify) update { it.copy(deletingId = id) } }
    fun cancelDelete() { if (!state.value.saving) update { it.copy(deletingId = null) } }

    /** Reload is explicit; unreadable or corrupt files never become an implicit empty writable store. */
    fun reload() {
        if (closed || state.value.loading || state.value.saving) return
        update { it.copy(loading = true, storageError = null) }
        scope.launch {
            try {
                val loaded = withContext(io) { store.load() }
                update { it.copy(loaded = true, loading = false, devices = loaded.devices,
                    storageWarning = loaded.warning, selectedId = it.selectedId?.takeIf { id -> loaded.devices.any { d -> d.id == id } }) }
            } catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) { update { it.copy(loaded = false, loading = false, storageError = "Could not read saved devices. Check access to the device settings file and reload.") } }
        }
    }

    fun saveEditor() {
        if (!state.value.canModify) return
        val draft = state.value.editor ?: return
        val device = try { draft.validated().copy(hostnameHint = state.value.devices.firstOrNull { it.id == draft.id }?.hostnameHint) } catch (error: IllegalArgumentException) {
            update { it.copy(editorError = error.message) }; return
        }
        val existing = state.value.devices
        if (existing.none { it.id == device.id } && existing.size >= PropertiesSavedLanDeviceStore.MAX_DEVICES) {
            update { it.copy(editorError = "The saved-device limit has been reached.") }; return
        }
        val replacement = if (existing.any { it.id == device.id }) existing.map { if (it.id == device.id) device else it } else existing + device
        persist(replacement, device.id)
    }

    fun confirmDelete() {
        if (!state.value.canModify) return
        val id = state.value.deletingId ?: return
        persist(state.value.devices.filterNot { it.id == id }, state.value.selectedId?.takeIf { it != id })
    }

    private fun persist(devices: List<SavedLanDevice>, selected: UUID?) {
        update { it.copy(saving = true, storageError = null, notice = null) }
        scope.launch {
            try {
                withContext(io) { store.save(devices) }
                // Publish only after replacement succeeds; failures retain both the old list and draft.
                update { it.copy(saving = false, devices = devices, selectedId = selected,
                    editor = null, deletingId = null, editorError = null, bindingKey = null,
                    notice = if (it.deletingId != null) "Device deleted." else "Device saved.",
                    lastResults = it.lastResults.filterKeys { id -> devices.any { device -> device.id == id } }) }
            } catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) { update { it.copy(saving = false, storageError = "Could not save devices. Check file access. Retry Save/Delete, or reload if the file changed elsewhere.") } }
        }
    }

    fun refreshInterfaces() {
        if (!state.value.visible || state.value.networkLoading || state.value.wakingId != null) return
        val token = generation
        update { it.copy(networkLoading = true, networkError = null) }
        networkJob = scope.launch {
            try {
                val result = withTimeoutOrNull(10_000) { networks.snapshot() }
                if (token != generation) return@launch
                when (result) {
                    is LanResult.Success -> update { it.copy(networks = result.value, networkLoading = false,
                        bindingKey = it.bindingKey?.takeIf { key -> result.value.interfaces.any { entry -> entry.binding.key == key } }) }
                    else -> update { it.copy(networkLoading = false, networkError = "Network interfaces are unavailable. Check your LAN connection and refresh.") }
                }
            } catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) { if (token == generation) update { it.copy(networkLoading = false, networkError = "Could not read network interfaces. Try Refresh interfaces.") } }
        }
    }

    fun wakeDevice(id: UUID) {
        if (!state.value.visible || state.value.wakingId != null || state.value.saving) return
        val device = state.value.devices.firstOrNull { it.id == id } ?: return
        val config = device.wake ?: return
        val binding = state.value.bindingKey.takeIf { state.value.selectedId == id }
        selectDevice(id)
        val token = generation
        update { it.copy(wakingId = id) }
        wakeJob = scope.launch {
            try {
                val result = wake.send(WakeRequest(config, binding))
                if (token != generation) return@launch
                val message = when (result) {
                    is LanResult.Success -> "Packet sent to ${result.value.route.target.value}:${result.value.route.port}. Device wake-up is not confirmed."
                    is LanResult.Failure -> failureMessage(result.code)
                }
                update { it.copy(wakingId = null, lastResults = it.lastResults + (id to message)) }
            } catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) { if (token == generation) update { it.copy(wakingId = null, lastResults = it.lastResults + (id to "Could not send the packet. Refresh interfaces and try again.")) } }
        }
    }

    private fun failureMessage(code: LanError): String = when (code) {
        LanError.INTERFACE_SELECTION_REQUIRED -> "Choose a network interface before sending."
        LanError.INTERFACE_UNAVAILABLE, LanError.INTERFACE_UNSUITABLE -> "The saved interface is unavailable or unsuitable. Refresh and choose another interface explicitly."
        LanError.NETWORK_CHANGED, LanError.NETWORK_UNAVAILABLE -> "The network changed or is unavailable. Refresh interfaces and try again."
        LanError.INVALID_INPUT -> "The MAC, port or broadcast does not match the selected network. Edit the device settings."
        LanError.PERMISSION_DENIED -> "The operating system denied sending. Check network permissions."
        LanError.TIMEOUT -> "Sending timed out; delivery is not confirmed."
        LanError.UNSUPPORTED -> "This network does not support the required broadcast operation."
        LanError.RESOURCE_LIMIT -> "The network tool is busy. Wait and try again."
        LanError.IO -> "Could not send the packet. Check the interface and network configuration."
    }

    private fun update(change: (LanToolsUiState) -> LanToolsUiState) { if (!closed) mutable.value = change(mutable.value) }

    override fun close() { dismiss(); closed = true; scope.cancel() }
}

/** Matches existing desktop state owners: UI mutations belong to Swing EDT, without a Main provider. */
private object LanToolsUiDispatcher : CoroutineDispatcher() {
    override fun isDispatchNeeded(context: CoroutineContext): Boolean = !SwingUtilities.isEventDispatchThread()
    override fun dispatch(context: CoroutineContext, block: Runnable) = SwingUtilities.invokeLater(block)
}
