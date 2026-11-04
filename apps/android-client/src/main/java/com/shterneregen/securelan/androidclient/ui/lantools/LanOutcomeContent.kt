package com.shterneregen.securelan.androidclient.ui.lantools

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.androidclient.R
import com.shterneregen.securelan.androidclient.lantools.*
import java.text.DateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import com.shterneregen.securelan.androidclient.ui.appStringResource as text

/** Persist UTC instants; render dates in the device locale and time zone, including historical dates. */
internal fun localCheckTime(at: Instant, now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault(), locale: Locale = Locale.getDefault()): String {
    val formatter = if (at.atZone(zone).toLocalDate() == now.atZone(zone).toLocalDate()) DateFormat.getTimeInstance(DateFormat.SHORT, locale)
        else DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale)
    formatter.timeZone = java.util.TimeZone.getTimeZone(zone)
    return formatter.format(Date.from(at))
}

@Composable
internal fun Disclosure(label: String, expanded: Boolean, onClick: () -> Unit, tag: String = "lan-details-toggle") {
    val expandedLabel = text(if (expanded) R.string.lan_expanded else R.string.lan_collapsed)
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(tag)
        .semantics { stateDescription = expandedLabel }) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
        Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null)
    }
}

@Composable
internal fun phaseLabel(phase: LanPhase): String = text(when (phase) {
    LanPhase.IDLE -> R.string.lan_not_checked
    LanPhase.RUNNING -> R.string.lan_running
    LanPhase.SUCCESS -> R.string.lan_phase_success
    LanPhase.FAILURE -> R.string.lan_check_failed
    LanPhase.CANCELLED -> R.string.lan_phase_cancelled
})

@Composable
internal fun Outcome(state: LanToolsState, kind: LanOperation, model: LanToolsViewModel) {
    val outcome = state.outcome(kind)
    if (outcome.phase == LanPhase.IDLE) return
    OutcomeSummary(outcome, "lan-result-${kind.name}")
    if (outcome.phase == LanPhase.RUNNING) {
        LinearProgressIndicator(Modifier.fillMaxWidth())
        TextButton(onClick = { model.cancel(kind) }) { Text(text(R.string.lan_cancel)) }
    } else if (kind in setOf(LanOperation.TCP, LanOperation.DNS, LanOperation.GATEWAY) && state.submissions.containsKey(kind)) {
        TextButton(onClick = { model.retry(kind) }, modifier = Modifier.testTag("lan-retry-${kind.name}")) { Text(text(R.string.lan_run_again)) }
        if (kind != LanOperation.DNS && outcome.phase == LanPhase.FAILURE) RecoveryNetwork(state, model)
    }
}

@Composable
internal fun RecoveryNetwork(state: LanToolsState, model: LanToolsViewModel) {
    var select by rememberSaveable { mutableStateOf(false) }
    TextButton(onClick = { select = !select }) { Text(text(R.string.lan_choose_network)) }
    if (select) {
        SourcePicker(state, model)
        TextButton(onClick = model::refreshNetwork, enabled = state.outcome(LanOperation.NETWORK).phase != LanPhase.RUNNING) {
            Text(text(R.string.lan_refresh_network))
        }
    }
}

/** The message and metadata all belong to the submitted request, never the current editable fields. */
@Composable
internal fun OutcomeSummary(outcome: LanOutcome, tag: String = "lan-check-history") {
    var details by rememberSaveable(outcome.startedAt?.toString(), outcome.at?.toString()) { mutableStateOf(false) }
    val failed = outcome.phase == LanPhase.FAILURE
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, if (failed) MaterialTheme.colorScheme.error.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant),
        shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth().testTag(tag)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(when (outcome.phase) {
                    LanPhase.SUCCESS -> Icons.Outlined.CheckCircle
                    LanPhase.FAILURE -> Icons.Outlined.ErrorOutline
                    LanPhase.RUNNING -> Icons.Outlined.HourglassTop
                    else -> Icons.Outlined.Info
                }, null, Modifier.size(20.dp), tint = if (failed) MaterialTheme.colorScheme.error else LocalContentColor.current)
                Text(phaseLabel(outcome.phase), style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
            outcome.subject?.let { Notice(it) }
            outcome.message?.let { Notice(it) }
            outcome.at?.let { Text(text(R.string.lan_checked_at, localCheckTime(it)), style = MaterialTheme.typography.bodySmall) }
            if (outcome.startedAt != null || outcome.source != null) {
                Disclosure(text(R.string.lan_details), details, { details = !details }, "$tag-details")
                if (details) {
                    outcome.startedAt?.let { Text(text(R.string.lan_started_at, localCheckTime(it)), style = MaterialTheme.typography.bodySmall) }
                    outcome.source?.let { Notice(it) }
                    outcome.timeoutMillis?.let { Text(text(R.string.lan_timeout_summary, it.toString()), style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
    }
}
