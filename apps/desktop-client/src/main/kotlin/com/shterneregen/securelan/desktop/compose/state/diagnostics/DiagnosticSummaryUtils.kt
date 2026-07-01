package com.shterneregen.securelan.desktop.compose.state.diagnostics

private const val COMPOSE_DIAGNOSTIC_SUMMARY_MAX_LENGTH: Int = 120

internal fun summarizeContextPanelText(value: String, maxLength: Int = COMPOSE_DIAGNOSTIC_SUMMARY_MAX_LENGTH): String {
    val normalized = value.replace(Regex("\\s+"), " ").trim()
    return if (normalized.length <= maxLength) {
        normalized
    } else {
        normalized.take(maxLength - 1).trimEnd() + "…"
    }
}
