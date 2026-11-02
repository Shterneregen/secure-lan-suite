package com.shterneregen.securelan.androidclient.lantools

import androidx.lifecycle.ViewModel
import com.shterneregen.securelan.androidclient.R
import com.shterneregen.securelan.lantools.diagnostics.*
import com.shterneregen.securelan.lantools.model.*
import com.shterneregen.securelan.lantools.wol.WakeRequest
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Clock
import java.util.Locale
import java.util.UUID

/**
 * Separate lifecycle owner: temporary closure/background cancels networking, not forms or committed
 * saves. Generations reject late native callbacks. Network recovery never restarts discovery.
 */
class LanToolsViewModel(private val services: LanToolsServices, private val store: LanDeviceStore,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate, private val io: CoroutineDispatcher = Dispatchers.IO,
    private val clock: Clock = Clock.systemUTC()) : ViewModel() {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val mutable = MutableStateFlow(LanToolsState())
    val state = mutable.asStateFlow()
    private val jobs = mutableMapOf<LanOperation, Job>()
    private val generations = mutableMapOf<LanOperation, Long>()
    private var closed = false

    fun setActive(active: Boolean) {
        if (closed || state.value.active == active) return
        update { it.copy(active = active) }
        if (active) {
            try { services.platform.start { scope.launch { if (state.value.active) refreshNetwork() } } }
            catch (_: Exception) { result(LanOperation.NETWORK, LanOutcome(LanPhase.FAILURE, LanMessage(R.string.lan_permission_error))) }
            if (!state.value.loaded) reload()
            refreshNetwork()
        } else {
            LanOperation.entries.forEach(::cancel)
            runCatching { services.platform.close() }
        }
    }
    fun permission(granted: Boolean) { update { it.copy(nearbyGranted = granted) }; if (!granted) cancel(LanOperation.DISCOVERY) }
    fun tab(tab: LanTab) = update { it.copy(tab = tab) }
    fun search(query: String) = update { it.copy(query = query.take(253)) }
    fun filter(filter: LanFilter) = update { it.copy(filter = filter) }
    fun binding(key: String?) = update { it.copy(binding = key, input = it.input.copy(gateway = null, origin = null)) }
    fun selectSaved(id: UUID) = update { it.copy(selectedSaved = id, selectedFound = null) }
    fun selectFound(id: String) = update { it.copy(selectedFound = id, selectedSaved = null) }
    fun editInput(change: (LanDiagnosticInput) -> LanDiagnosticInput) = update {
        val next = change(it.input)
        it.copy(input = if (next.host != it.input.host) next.copy(origin = null) else next)
    }
    fun add() { if (state.value.canModify) update { it.copy(form = LanDeviceForm(), formError = null) } }
    fun edit(id: UUID) { state.value.devices.firstOrNull { it.id == id }?.let { device ->
        if (state.value.canModify) update { it.copy(form = LanDeviceForm.from(device), formError = null) }
    } }
    fun changeForm(value: LanDeviceForm) { if (!state.value.saving) update { it.copy(form = value, formError = null) } }
    fun cancelForm() { if (!state.value.saving) update { it.copy(form = null, formError = null) } }
    fun requestDelete(id: UUID?) { if (state.value.canModify) update { it.copy(deleting = id) } }

    fun reload() {
        if (state.value.loading || state.value.saving) return
        update { it.copy(loading = true, storeError = null) }
        scope.launch {
            try { val devices = withContext(io) { store.load() }; update { it.copy(devices = devices, loaded = true, loading = false) } }
            catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) { update { it.copy(loading = false, loaded = false, storeError = LanMessage(R.string.lan_store_load_error)) } }
        }
    }
    fun save() {
        if (!state.value.canModify) return
        val device = try { state.value.form?.device() ?: return }
        catch (invalid: InvalidLanForm) { update { it.copy(formError = LanMessage(invalid.resource)) }; return }
        catch (_: IllegalArgumentException) { update { it.copy(formError = LanMessage(R.string.lan_invalid_device)) }; return }
        val current = state.value.devices
        if (current.size >= 256 && current.none { it.id == device.id }) { update { it.copy(formError = LanMessage(R.string.lan_resource_error)) }; return }
        persist(if (current.any { it.id == device.id }) current.map { if (it.id == device.id) device else it } else current + device, device.id)
    }
    fun delete() {
        if (!state.value.canModify) return
        val id = state.value.deleting ?: return
        persist(state.value.devices.filterNot { it.id == id }, null)
    }
    private fun persist(devices: List<SavedLanDevice>, selected: UUID?) {
        update { it.copy(saving = true, storeError = null) }
        scope.launch {
            try {
                withContext(io) { store.save(devices) }
                update { it.copy(devices = devices, selectedSaved = selected, selectedFound = null, saving = false, form = null, deleting = null,
                    checks = it.checks.filterKeys { key -> !key.startsWith("saved:") || devices.any { device -> key == "saved:${device.id}" } }, notice = LanMessage(R.string.lan_saved)) }
            } catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) { update { it.copy(saving = false, storeError = LanMessage(R.string.lan_store_save_error)) } }
        }
    }

    fun saveFound(id: String) {
        val row = state.value.found.devices.firstOrNull { it.id == id } ?: return
        if (state.value.canModify) update { it.copy(form = LanDeviceForm(name = (row.displayName ?: row.hostname.orEmpty()).take(128).trim(),
            address = row.addresses.firstOrNull()?.value.orEmpty(), hostname = row.hostname.orEmpty()), formError = null) }
    }
    fun diagnoseSaved(id: UUID) {
        val device = state.value.devices.firstOrNull { it.id == id } ?: return
        val host = device.addressHint?.value ?: device.hostnameHint ?: return
        update { it.copy(tab = LanTab.DIAGNOSTICS, input = it.input.copy(host = host, port = "", origin = "saved:$id")) }
    }
    fun diagnoseFound(id: String, host: String, port: Int) {
        val row = state.value.found.devices.firstOrNull { it.id == id } ?: return
        if (row.services.none { it.port == port && it.addresses.any { ip -> ip.value == host } }) return
        update { it.copy(tab = LanTab.DIAGNOSTICS, binding = row.scope,
            input = it.input.copy(host = host, port = port.toString(), origin = "found:$id", gateway = null)) }
    }

    fun refreshNetwork() = run(LanOperation.NETWORK, 10_000) {
        when (val value = services.network.snapshot()) {
            is LanResult.Failure -> failure(value)
            is LanResult.Success -> { currentCoroutineContext().ensureActive(); update { it.copy(network = value.value) }; LanMessage(R.string.lan_network_refreshed) }
        }
    }
    fun discover() {
        if (!state.value.nearbyGranted) { result(LanOperation.DISCOVERY, LanOutcome(LanPhase.FAILURE, LanMessage(R.string.lan_nearby_required))); return }
        val selected = state.value.binding
        val token = (generations[LanOperation.DISCOVERY] ?: 0) + 1
        run(LanOperation.DISCOVERY, 35_000) {
            when (val value = services.browser.browse(selected) { snapshot -> withContext(dispatcher) {
                currentCoroutineContext().ensureActive()
                if (state.value.active && generations[LanOperation.DISCOVERY] == token) update { it.copy(found = snapshot) }
            } }) {
                is LanResult.Failure -> failure(value)
                is LanResult.Success -> { currentCoroutineContext().ensureActive(); update { it.copy(found = value.value.copy(devices = value.value.devices.map { row -> row.copy(stale = true) })) }; LanMessage(R.string.lan_browse_completed) }
            }
        }
    }
    fun wake(id: UUID) {
        val device = state.value.devices.firstOrNull { it.id == id } ?: return
        val config = device.wake ?: return
        val selected = state.value.binding
        run(LanOperation.WAKE, 3_000) { when (val value = services.wake.send(WakeRequest(config, selected))) {
            is LanResult.Failure -> failure(value)
            is LanResult.Success -> LanMessage(R.string.lan_packet_sent, listOf(value.value.route.target.value, value.value.route.port.toString()))
        } }
    }
    fun tcp() {
        val captured = state.value
        val input = captured.input
        val port = input.port.toIntOrNull()?.takeIf { it in 1..65535 } ?: return invalid(LanOperation.TCP, R.string.lan_invalid_port)
        val timeout = timeout(LanOperation.TCP) ?: return
        run(LanOperation.TCP, timeout, input.origin, input.host.trim(), port, captured.binding) {
            tcpMessage(services.diagnostics.tcp(TcpCheckRequest(input.host.trim(), port, captured.binding, timeout)))
        }
    }
    fun dns() {
        val input = state.value.input
        val timeout = timeout(LanOperation.DNS) ?: return
        run(LanOperation.DNS, timeout) { when (val value = services.diagnostics.lookup(DnsLookupRequest(input.dns.trim(), input.family, timeout))) {
            is LanResult.Failure -> failure(value)
            is LanResult.Success -> LanMessage(if (value.value.numericInput) R.string.lan_dns_numeric else R.string.lan_dns_result,
                listOf(value.value.host, value.value.addresses.joinToString { it.value }, milliseconds(value.value.lookupNanos)))
        } }
    }
    fun gateway() {
        val captured = state.value
        val source = captured.selectedInterface ?: return invalid(LanOperation.GATEWAY, R.string.lan_select_network)
        val target = captured.input.gateway?.takeIf { key -> source.gateways.any { it.value == key } } ?: return invalid(LanOperation.GATEWAY, R.string.lan_select_gateway)
        val port = captured.input.gatewayPort.toIntOrNull()?.takeIf { it in 1..65535 } ?: return invalid(LanOperation.GATEWAY, R.string.lan_invalid_port)
        val timeout = timeout(LanOperation.GATEWAY) ?: return
        run(LanOperation.GATEWAY, timeout) {
            when (val fresh = services.network.snapshot()) {
                is LanResult.Failure -> failure(fresh)
                is LanResult.Success -> {
                    val current = fresh.value.interfaces.firstOrNull { it.source.binding == source.source.binding }
                    if (current?.metadata != MetadataAvailability.AVAILABLE || current.gateways.none { it.value == target }) failure(LanResult.Failure(LanError.NETWORK_CHANGED))
                    else tcpMessage(services.diagnostics.tcp(TcpCheckRequest(target, port, captured.binding, timeout)))
                }
            }
        }
    }
    private fun tcpMessage(value: LanResult<TcpCheckReceipt>): LanMessage = when (value) {
        is LanResult.Failure -> failure(value)
        is LanResult.Success -> LanMessage(R.string.lan_tcp_result, listOf(value.value.address.value, value.value.port.toString(), milliseconds(value.value.connectNanos)))
    }
    private fun timeout(kind: LanOperation): Long? = state.value.input.timeout.toLongOrNull()?.takeIf { it in 1..30_000 }
        .also { if (it == null) invalid(kind, R.string.lan_invalid_timeout) }
    private fun invalid(kind: LanOperation, resource: Int) { if (state.value.outcome(kind).phase != LanPhase.RUNNING) result(kind, LanOutcome(LanPhase.FAILURE, LanMessage(resource))) }
    private fun failure(value: LanResult.Failure): Nothing = throw LanCheckFailure(value.code)

    /** Network jobs are bounded, per-kind and invalidated before cancellation; no queued health scans. */
    private fun run(kind: LanOperation, timeout: Long, origin: String? = null, host: String = "", port: Int = 0, binding: String? = null, block: suspend () -> LanMessage) {
        if (!state.value.active || state.value.outcome(kind).phase == LanPhase.RUNNING) return
        val token = (generations[kind] ?: 0) + 1
        generations[kind] = token
        result(kind, LanOutcome(LanPhase.RUNNING, LanMessage(R.string.lan_running)))
        jobs[kind] = scope.launch {
            val outcome = try {
                val message = withTimeoutOrNull(timeout) { block() }
                if (message == null) LanOutcome(LanPhase.FAILURE, errorMessage(LanError.TIMEOUT)) else LanOutcome(LanPhase.SUCCESS, message)
            } catch (cancel: CancellationException) { throw cancel }
            catch (failure: LanCheckFailure) { LanOutcome(LanPhase.FAILURE, errorMessage(failure.code)) }
            catch (_: Exception) { LanOutcome(LanPhase.FAILURE, errorMessage(LanError.IO)) }
            if (state.value.active && generations[kind] == token) {
                result(kind, outcome)
                if (kind == LanOperation.DISCOVERY) update { it.copy(found = it.found.copy(devices = it.found.devices.map { row -> row.copy(stale = true) })) }
                if (origin != null) update { state ->
                    val check = LanCheck(host, port, binding, clock.instant(), outcome)
                    state.copy(checks = ((state.checks - origin) + (origin to check)).entries.toList().takeLast(256).associate { it.toPair() })
                }
            }
        }
    }
    fun cancel(kind: LanOperation) {
        generations[kind] = (generations[kind] ?: 0) + 1
        jobs.remove(kind)?.cancel()
        if (state.value.outcome(kind).phase == LanPhase.RUNNING) result(kind, LanOutcome(LanPhase.CANCELLED, LanMessage(R.string.lan_cancelled)))
        if (kind == LanOperation.DISCOVERY) update { it.copy(found = it.found.copy(devices = it.found.devices.map { row -> row.copy(stale = true) })) }
    }
    private fun result(kind: LanOperation, outcome: LanOutcome) = update { it.copy(outcomes = it.outcomes + (kind to outcome)) }
    private fun update(change: (LanToolsState) -> LanToolsState) { if (!closed) mutable.value = change(state.value) }
    private fun milliseconds(nanos: Long) = String.format(Locale.ROOT, "%.2f", nanos / 1_000_000.0)
    override fun onCleared() { setActive(false); closed = true; scope.cancel(); services.release() }
}

private class LanCheckFailure(val code: LanError) : RuntimeException()

/** Shared mappings describe the evidence of the operation, never device power state. */
internal fun errorMessage(error: LanError) = LanMessage(when (error) {
    LanError.INVALID_INPUT -> R.string.lan_invalid_device
    LanError.INTERFACE_SELECTION_REQUIRED -> R.string.lan_select_network
    LanError.INTERFACE_UNAVAILABLE, LanError.INTERFACE_UNSUITABLE, LanError.NETWORK_CHANGED -> R.string.lan_network_changed
    LanError.NETWORK_UNAVAILABLE -> R.string.lan_network_unavailable
    LanError.PERMISSION_DENIED -> R.string.lan_permission_error
    LanError.TIMEOUT -> R.string.lan_timeout_error
    LanError.CONNECTION_FAILED -> R.string.lan_tcp_failure
    LanError.RESOLUTION_FAILED -> R.string.lan_dns_failure
    LanError.RESOURCE_LIMIT -> R.string.lan_resource_error
    LanError.UNSUPPORTED -> R.string.lan_unsupported
    LanError.IO -> R.string.lan_io_error
})
