package com.shterneregen.securelan.desktop.compose.state.quickshare

import com.shterneregen.securelan.common.net.NetworkConstants
import com.shterneregen.securelan.desktop.compose.state.shell.ComposeEmptyStateVisualWeight
import com.shterneregen.securelan.desktop.ui.DesktopQuickShareFormatters
import com.shterneregen.securelan.desktop.ui.QuickShareEntry

public data class ComposeQuickShareState(
    val running: Boolean = false,
    val portText: String = NetworkConstants.DEFAULT_QUICK_SHARE_PORT.toString(),
    val selectedFilePath: String = "",
    val textDraft: String = "SecureLanSuite quick-share text",
    val expirationMinutesText: String = "10",
    val accessLimitText: String = "3",
    val useCustomPort: Boolean = false,
    val noExpiration: Boolean = false,
    val unlimitedAccess: Boolean = false,
    val serverError: String? = null,
    val entries: List<QuickShareEntry> = emptyList(),
    val landingUrls: List<String> = emptyList(),
    val javaFxFallbackAvailable: Boolean = true,
) {
    val title: String = "Share by browser link"
    val subtitle: String = "Create temporary file or text links that people on this trusted LAN can open in a browser."
    val customPort: Int? = portText.trim().toIntOrNull()?.takeIf { it in 1..65_535 }
    val port: Int? = if (useCustomPort) customPort else NetworkConstants.DEFAULT_QUICK_SHARE_PORT
    val expirationMinutes: Long? = expirationMinutesText.trim().toLongOrNull()?.takeIf { it >= 1 }
    val accessLimit: Int? = accessLimitText.trim().toIntOrNull()?.takeIf { it >= 1 }
    val expirationPolicyValid: Boolean = noExpiration || expirationMinutes != null
    val accessPolicyValid: Boolean = unlimitedAccess || accessLimit != null
    val effectiveExpirationMinutes: Long? = if (noExpiration) null else expirationMinutes
    val effectiveAccessLimit: Int? = if (unlimitedAccess) null else accessLimit
    val hasSelectedFile: Boolean = selectedFilePath.trim().isNotEmpty()
    val selectedFileName: String = selectedFilePath.trim()
        .let { if (it.isEmpty()) "" else java.nio.file.Path.of(it).fileName?.toString() ?: it }
    val hasText: Boolean = textDraft.trim().isNotEmpty()
    val activeEntries: List<QuickShareEntry> = entries.filter { it.active() }
    val inactiveEntries: List<QuickShareEntry> = entries.filterNot { it.active() }
    val statusText: String = if (running && port != null) DesktopQuickShareFormatters.formatServerStatus(port) else "Quick share is stopped"
    val statusDetail: String = if (running) {
        "Links are available until their time or access limit is reached."
    } else {
        "Start quick share first, then create a file or text link."
    }
    val landingText: String = DesktopQuickShareFormatters.formatLandingValue(landingUrls)
    val trustedLanWarning: String =
        "Trusted LAN only. These links have no login, so stop shares when everyone has downloaded them."
    val canStartServer: Boolean = !running && port != null && javaFxFallbackAvailable
    val canStopServer: Boolean = running && javaFxFallbackAvailable
    val canCreateFileShare: Boolean = hasSelectedFile && expirationPolicyValid && accessPolicyValid && javaFxFallbackAvailable
    val canCreateTextShare: Boolean = hasText && expirationPolicyValid && accessPolicyValid && javaFxFallbackAvailable
    val canCreateFileLinkNow: Boolean = canCreateFileShare && running
    val canCreateTextLinkNow: Boolean = canCreateTextShare && running
    val canCopyIndex: Boolean = running && landingUrls.isNotEmpty() && javaFxFallbackAvailable
    val shareRows: List<String> = entries.map { DesktopQuickShareFormatters.formatSnapshotMeta(it.snapshot()) }
    val activeShareCountLabel: String = when (activeEntries.size) {
        0 -> "No active links"
        1 -> "1 active link"
        else -> "${activeEntries.size} active links"
    }
    val inactiveShareCountLabel: String = when (inactiveEntries.size) {
        0 -> "No stopped or expired links"
        1 -> "1 stopped or expired link"
        else -> "${inactiveEntries.size} stopped or expired links"
    }
    val linkPolicySummary: String = buildList {
        add(if (noExpiration) "available until stopped" else expirationMinutes?.let { "expires after $it min" } ?: "set expiration")
        add(if (unlimitedAccess) "unlimited downloads" else accessLimit?.let { "$it downloads max" } ?: "set download limit")
    }.joinToString(" · ")
    val policySentence: String = when {
        noExpiration && unlimitedAccess -> "Link remains available until stopped, with unlimited downloads."
        noExpiration -> "Link remains available until stopped or ${accessLimit ?: "the configured number of"} downloads."
        unlimitedAccess -> "Link expires after ${expirationMinutes ?: "the configured number of"} minutes, with unlimited downloads."
        else -> "Link expires after ${expirationMinutes ?: "the configured number of"} minutes or ${accessLimit ?: "the configured number of"} downloads."
    }
    val portValidationMessage: String? = when {
        !useCustomPort -> null
        customPort == null -> "Enter a port from 1 to 65535."
        else -> serverError
    }
    val expirationValidationMessage: String? =
        if (!noExpiration && expirationMinutes == null) "Enter at least 1 minute." else null
    val accessValidationMessage: String? =
        if (!unlimitedAccess && accessLimit == null) "Enter at least 1 download." else null
    val serverStatusSummary: String = if (running) {
        "Sharing active on this trusted LAN"
    } else {
        "Start sharing to create temporary browser links"
    }
    val createLinkHint: String = if (running) {
        "Choose a file or enter text, then create a link."
    } else {
        "Start sharing above before you can create links."
    }
    val readinessSummary: String = buildList {
        if (port == null) add("Enter a valid port from 1 to 65535.")
        if (!expirationPolicyValid) add("Set expiration to at least 1 minute.")
        if (!accessPolicyValid) add("Set the download limit to at least 1.")
        serverError?.takeIf { it.isNotBlank() }?.let(::add)
        if (!javaFxFallbackAvailable) add("JavaFX fallback is unavailable; live quick-share actions stay disabled.")
    }.ifEmpty {
        listOf(policySentence)
    }.joinToString(" · ")
    val emptySharesTitle: String = if (running) "No links created yet" else "Quick share is idle"
    val emptySharesSituation: String = emptySharesTitle
    val emptySharesExplanation: String = if (running) {
        "Browser links will appear here after you create one."
    } else {
        "Temporary browser links are off until you start trusted-LAN sharing."
    }
    val emptySharesNextAction: String = if (running) {
        "Choose a file or enter text"
    } else {
        "Start quick share"
    }
    val emptySharesDetail: String = if (running) {
        "$emptySharesExplanation $emptySharesNextAction, then create a link."
    } else {
        "$emptySharesExplanation $emptySharesNextAction to create links for this LAN."
    }
    val emptySharesStructuredCopy: List<String> = listOf(emptySharesSituation, emptySharesExplanation, emptySharesNextAction)
    val emptySharesVisualWeight: ComposeEmptyStateVisualWeight = ComposeEmptyStateVisualWeight.INLINE
    val quickStartSteps: List<String> = listOf(
        "1. Start quick share.",
        "2. Choose a file or type text.",
        "3. Create a link and send it to trusted LAN peers.",
    )
    val shareRowsDetailed: List<ComposeQuickShareRow> = entries.map(ComposeQuickShareRow::from)
}
