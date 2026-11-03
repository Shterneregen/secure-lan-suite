package com.shterneregen.securelan.desktop.compose.state.lantools

import com.shterneregen.securelan.lantools.diagnostics.AddressFamily
import com.shterneregen.securelan.lantools.diagnostics.DiagnosticsNetworkSnapshot
import java.time.Instant

enum class DiagnosticKind { NETWORK, TCP, DNS, GATEWAY }
enum class DiagnosticPhase { IDLE, RUNNING, SUCCESS, PARTIAL, FAILURE, CANCELLED }

/** An operation retains its captured endpoint/result even when the next request's form is edited. */
data class DiagnosticOutcome(val phase: DiagnosticPhase = DiagnosticPhase.IDLE, val message: String = "Not run")

/** Independent forms: selecting a source for TCP/gateway does not claim to bind the system resolver. */
data class DiagnosticInputs(
    val tcpHost: String = "", val tcpPort: String = "", val dnsHost: String = "",
    val family: AddressFamily = AddressFamily.ALL, val timeoutMillis: String = "5000",
    val bindingKey: String? = null, val gateway: String? = null, val gatewayPort: String = "",
    val deviceKey: String? = null,
)

/** An endpoint-specific history entry, not a global online flag or a device identity assertion. */
data class DeviceTcpCheck(val host: String, val port: Int, val bindingKey: String?, val checkedAt: Instant, val outcome: DiagnosticOutcome)

/** No room references, persisted diagnostics or inferred global online/offline flags. */
data class LanDiagnosticsUiState(
    val inputs: DiagnosticInputs = DiagnosticInputs(),
    val network: DiagnosticsNetworkSnapshot? = null,
    val outcomes: Map<DiagnosticKind, DiagnosticOutcome> = DiagnosticKind.entries.associateWith { DiagnosticOutcome() },
    val deviceChecks: Map<String, DeviceTcpCheck> = emptyMap(),
) {
    fun outcome(kind: DiagnosticKind): DiagnosticOutcome = outcomes.getValue(kind)
    val selectedInterface get() = network?.interfaces?.firstOrNull { it.source.binding.key == inputs.bindingKey }
}
