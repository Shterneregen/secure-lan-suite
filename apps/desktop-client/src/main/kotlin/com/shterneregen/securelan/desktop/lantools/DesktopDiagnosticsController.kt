package com.shterneregen.securelan.desktop.lantools

import com.shterneregen.securelan.desktop.compose.state.lantools.*
import com.shterneregen.securelan.lantools.diagnostics.*
import com.shterneregen.securelan.lantools.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Clock

/**
 * UI-thread owner of bounded diagnostic jobs. Forms survive temporary closure; jobs do not.
 * Each operation has its own generation so late native DNS/process callbacks cannot publish results
 * into a later session. [release] closes only application-owned diagnostic resources on app shutdown.
 */
class DesktopDiagnosticsController(
    private val service: NetworkDiagnosticsService,
    private val networks: DiagnosticsNetworkInfoProvider,
    dispatcher: CoroutineDispatcher = LanToolsUiDispatcher,
    private val release: () -> Unit = {},
    private val clock: Clock = Clock.systemUTC(),
) : AutoCloseable {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val mutable = MutableStateFlow(LanDiagnosticsUiState())
    val state = mutable.asStateFlow()
    private val jobs = mutableMapOf<DiagnosticKind, Job>()
    private val generations = DiagnosticKind.entries.associateWith { 0L }.toMutableMap()
    private val subjects = mutableMapOf<DiagnosticKind, String>()
    private var visible = false
    private var closed = false

    fun open() {
        if (closed || visible) return
        visible = true
        refresh()
    }

    /** The controller's pure editor callback keeps orchestration and validation out of composables. */
    fun edit(change: (DiagnosticInputs) -> DiagnosticInputs) {
        if (closed) return
        val old = state.value.inputs
        val changed = change(old)
        // A manually changed endpoint no longer belongs to the row that originally prefilled it.
        mutable.value = state.value.copy(inputs = if (old.tcpHost != changed.tcpHost || old.bindingKey != changed.bindingKey)
            changed.copy(deviceKey = null) else changed)
    }

    /** Prefill only: an explicit Check TCP action is still required. Existing running checks keep their input. */
    fun prepareTcp(host: String, port: Int?, bindingKey: String?, deviceKey: String) {
        if (!closed) mutable.value = state.value.copy(inputs = state.value.inputs.copy(
            tcpHost = host, tcpPort = port?.toString().orEmpty(), bindingKey = bindingKey, gateway = null, deviceKey = deviceKey))
    }

    fun selectInterface(key: String?) = edit { it.copy(bindingKey = key, gateway = null) }

    fun refresh() = start(DiagnosticKind.NETWORK) {
        when (val result = withTimeoutOrNull(10_000) { networks.snapshot() }) {
            is LanResult.Success -> {
                currentCoroutineContext().ensureActive()
                mutable.value = state.value.copy(network = result.value)
                DiagnosticOutcome(DiagnosticPhase.SUCCESS, "Local network information refreshed. ${result.value.provenance}")
            }
            is LanResult.Failure -> failure(result, "Local network information")
            else -> DiagnosticOutcome(DiagnosticPhase.FAILURE, "Reading network information timed out. Try Refresh network information.")
        }
    }

    fun checkTcp() {
        val input = state.value.inputs
        val port = input.tcpPort.toIntOrNull()
        val timeout = input.timeoutMillis.toLongOrNull()
        if (port == null || port !in 1..65535 || timeout == null || timeout !in 1..30_000) { invalid(DiagnosticKind.TCP, "Enter a TCP port (1–65535) and timeout (1–30000 ms)."); return }
        start(DiagnosticKind.TCP, timeout, input) { tcpResult(service.tcp(TcpCheckRequest(input.tcpHost.trim(), port, input.bindingKey, timeout)), "TCP ${input.tcpHost.trim()}:$port") }
    }

    fun lookupDns() {
        val input = state.value.inputs
        val timeout = input.timeoutMillis.toLongOrNull()
        if (timeout == null || timeout !in 1..30_000) { invalid(DiagnosticKind.DNS, "Enter a timeout from 1 to 30000 ms."); return }
        start(DiagnosticKind.DNS, timeout) {
            when (val result = service.lookup(DnsLookupRequest(input.dnsHost.trim(), input.family, timeout))) {
                is LanResult.Failure -> failure(result, "DNS lookup")
                is LanResult.Success -> DiagnosticOutcome(DiagnosticPhase.SUCCESS,
                    "Addresses for ${result.value.host}: ${result.value.addresses.joinToString { it.value }}. Lookup time: ${milliseconds(result.value.lookupNanos)} ms. " +
                        if (result.value.numericInput) "Numeric input; no DNS lookup was needed. This is not a connectivity check."
                        else "System resolution may use cached/hosts answers; this is not a connectivity check.")
            }
        }
    }

    /** Checks exactly the selected OS-reported IPv4 gateway and user-entered port, never a port list. */
    fun checkGateway() {
        val input = state.value.inputs
        val selected = state.value.selectedInterface
        val gateway = input.gateway
        val port = input.gatewayPort.toIntOrNull()
        val timeout = input.timeoutMillis.toLongOrNull()
        if (selected == null || selected.metadata != MetadataAvailability.AVAILABLE ||
            selected.gateways.none { it.value == gateway } || port == null || port !in 1..65535 || timeout == null || timeout !in 1..30_000) {
            invalid(DiagnosticKind.GATEWAY, "Choose an available interface, an OS-reported gateway and a TCP port. No gateway or port is guessed.")
            return
        }
        start(DiagnosticKind.GATEWAY, timeout) {
            // Recheck OS metadata before connecting: a stale gateway must not silently target another device.
            val fresh = when (val result = networks.snapshot()) {
                is LanResult.Success -> result.value
                is LanResult.Failure -> return@start failure(result, "Gateway metadata")
            }
            val current = fresh.interfaces.firstOrNull { it.source.binding == selected.source.binding }
            if (current?.metadata != MetadataAvailability.AVAILABLE || current.gateways.none { it.value == gateway }) {
                return@start failure(LanResult.Failure(LanError.NETWORK_CHANGED), "Gateway TCP check")
            }
            tcpResult(service.tcp(TcpCheckRequest(gateway!!, port, input.bindingKey, timeout)), "Gateway TCP $gateway:$port")
        }
    }

    /** Both explicit cancellation and dialog dismissal invalidate the result before cancelling its job. */
    fun cancel(kind: DiagnosticKind) {
        generations[kind] = generations.getValue(kind) + 1
        jobs.remove(kind)?.cancel()
        if (state.value.outcome(kind).phase == DiagnosticPhase.RUNNING) outcome(kind,
            identified(DiagnosticOutcome(DiagnosticPhase.CANCELLED, when (kind) {
                DiagnosticKind.NETWORK -> "Network information refresh cancelled."
                DiagnosticKind.DNS -> "Lookup cancelled. The OS resolver may still be finishing."
                else -> "Cancelled. A connection attempt may already have reached the endpoint."
            }), subjects[kind]))
    }

    fun dismiss() { visible = false; DiagnosticKind.entries.forEach(::cancel) }

    private fun invalid(kind: DiagnosticKind, message: String) {
        if (visible && state.value.outcome(kind).phase != DiagnosticPhase.RUNNING) outcome(kind, DiagnosticOutcome(DiagnosticPhase.FAILURE, message))
    }

    private fun start(kind: DiagnosticKind, timeout: Long = 30_000, check: DiagnosticInputs? = null, operation: suspend () -> DiagnosticOutcome) {
        if (closed || !visible || state.value.outcome(kind).phase == DiagnosticPhase.RUNNING) return
        val token = generations.getValue(kind) + 1
        generations[kind] = token
        // Capture before launch: editing a form must not relabel failure, timeout or cancellation.
        val input = state.value.inputs
        val subject = when (kind) {
            DiagnosticKind.NETWORK -> null
            DiagnosticKind.TCP -> "TCP ${input.tcpHost.trim()}:${input.tcpPort}"
            DiagnosticKind.DNS -> "DNS ${input.dnsHost.trim()}"
            DiagnosticKind.GATEWAY -> "Gateway TCP ${input.gateway}:${input.gatewayPort}"
        }
        if (subject != null) subjects[kind] = subject
        outcome(kind, identified(DiagnosticOutcome(DiagnosticPhase.RUNNING, "Running…"), subject))
        jobs[kind] = scope.launch {
            try {
                // Includes metadata refresh for a gateway, as well as the service's own deadline.
                val result = withTimeoutOrNull(timeout) { operation() }
                    ?: DiagnosticOutcome(DiagnosticPhase.FAILURE, "Operation timed out.")
                if (visible && generations[kind] == token) complete(kind, identified(result, subject), check)
            } catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) { if (visible && generations[kind] == token) complete(kind,
                identified(DiagnosticOutcome(DiagnosticPhase.FAILURE, "Could not complete the check. Refresh network information and try again."), subject), check) }
        }
    }

    private fun tcpResult(result: LanResult<TcpCheckReceipt>, label: String): DiagnosticOutcome = when (result) {
        is LanResult.Failure -> failure(result, label)
        is LanResult.Success -> DiagnosticOutcome(DiagnosticPhase.SUCCESS,
            "$label: connected to ${result.value.address.value}:${result.value.port} in ${milliseconds(result.value.connectNanos)} ms" +
                (result.value.source?.let { " from ${it.value}" } ?: " using OS routing") +
                ". Only this TCP port accepted a connection; this is not ICMP RTT or proof of device power state.")
    }

    private fun failure(result: LanResult.Failure, label: String): DiagnosticOutcome {
        val explanation = when (result.code) {
            LanError.INVALID_INPUT -> "Invalid ${result.field ?: "input"}. Enter a hostname/IP, port 1–65535 and timeout 1–30000 ms."
            LanError.CONNECTION_FAILED -> "The TCP port rejected the connection or could not accept it. The device may still be running."
            LanError.INTERFACE_SELECTION_REQUIRED -> "Choose a source interface explicitly."
            LanError.RESOLUTION_FAILED -> "No address was resolved for this input/address family. This does not prove the device is offline."
            LanError.TIMEOUT -> "The check timed out. The endpoint may be filtered or unavailable; its power state is unknown."
            LanError.RESOURCE_LIMIT -> "Resolver/check capacity is busy. A previous OS lookup may still be finishing; try again later."
            LanError.NETWORK_CHANGED, LanError.INTERFACE_UNAVAILABLE, LanError.INTERFACE_UNSUITABLE -> "The selected network changed or is unavailable. Refresh and select an interface again."
            LanError.NETWORK_UNAVAILABLE -> "No network route is available. Check your LAN connection."
            LanError.PERMISSION_DENIED -> "The OS denied access. Check network permissions."
            LanError.UNSUPPORTED -> "This operation or address scope is unsupported on this platform."
            LanError.IO -> "The network operation failed. Check the address, port and network configuration."
        }
        return DiagnosticOutcome(DiagnosticPhase.FAILURE, "$label: $explanation")
    }

    private fun milliseconds(nanos: Long): String = java.lang.String.format(java.util.Locale.ROOT, "%.2f", nanos / 1_000_000.0)
    private fun identified(value: DiagnosticOutcome, subject: String?): DiagnosticOutcome =
        if (subject == null || value.message.startsWith("$subject:")) value else value.copy(message = "$subject: ${value.message}")
    private fun complete(kind: DiagnosticKind, value: DiagnosticOutcome, input: DiagnosticInputs?) {
        outcome(kind, value)
        if (!closed && input?.deviceKey != null) {
            val history = state.value.deviceChecks - input.deviceKey + (input.deviceKey to DeviceTcpCheck(
                input.tcpHost.trim(), input.tcpPort.toInt(), input.bindingKey, clock.instant(), value))
            mutable.value = state.value.copy(deviceChecks = history.entries.toList().takeLast(256).associate { it.toPair() })
        }
    }
    private fun outcome(kind: DiagnosticKind, value: DiagnosticOutcome) { if (!closed) mutable.value = state.value.copy(outcomes = state.value.outcomes + (kind to value)) }
    override fun close() { if (!closed) { dismiss(); closed = true; scope.cancel(); release() } }
}
