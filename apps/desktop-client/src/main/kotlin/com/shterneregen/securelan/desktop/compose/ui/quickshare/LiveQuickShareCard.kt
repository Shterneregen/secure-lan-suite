package com.shterneregen.securelan.desktop.compose.ui.quickshare

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.ComposeDesktopHostAdapter
import com.shterneregen.securelan.desktop.compose.state.quickshare.ComposeQuickShareState
import com.shterneregen.securelan.desktop.compose.state.shell.ComposeShellMetadata
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.util.copyToSystemClipboard
import com.shterneregen.securelan.desktop.compose.util.openComposeFileChooser
import com.shterneregen.securelan.desktop.compose.util.openInBrowser
import com.shterneregen.securelan.desktop.ui.DesktopQuickShareFormatters
import java.nio.file.Path

@Composable
internal fun LiveQuickShareCard(hostAdapter: ComposeDesktopHostAdapter) {
    var quickSharePort by remember {
        mutableStateOf(
            ComposeShellMetadata.DEFAULT_STATUS_ADAPTER_STATE.serverFilePortText.replace(
                "5051",
                "5053"
            )
        )
    }
    var filePath by remember { mutableStateOf("") }
    var textDraft by remember { mutableStateOf("SecureLanSuite quick-share text") }
    var expirationMinutes by remember { mutableStateOf("10") }
    var accessLimit by remember { mutableStateOf("3") }
    var useCustomPort by remember { mutableStateOf(false) }
    var noExpiration by remember { mutableStateOf(false) }
    var unlimitedAccess by remember { mutableStateOf(false) }
    val quickShareState = ComposeQuickShareState(
        running = hostAdapter.quickShareRunning,
        portText = quickSharePort,
        selectedFilePath = filePath,
        textDraft = textDraft,
        expirationMinutesText = expirationMinutes,
        accessLimitText = accessLimit,
        useCustomPort = useCustomPort,
        noExpiration = noExpiration,
        unlimitedAccess = unlimitedAccess,
        serverError = hostAdapter.quickShareError,
        entries = hostAdapter.quickShareEntries,
        landingUrls = hostAdapter.quickShareLandingUrls,
    )

    val tokens = LocalSecureLanDesignTokens.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(tokens.radius.medium))
            .background(tokens.colors.surfaceLevel1)
            .padding(tokens.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        QuickShareServerPanel(
            state = quickShareState,
            onStart = { quickShareState.port?.let(hostAdapter::startQuickShare) },
            onStop = { hostAdapter.stopQuickShare() },
            onCopyIndex = { copyToSystemClipboard(DesktopQuickShareFormatters.pickPrimaryLandingUrl(hostAdapter.quickShareLandingUrls)) },
        )

        QuickShareCreateLinksPanel(
            state = quickShareState,
            filePath = filePath,
            onChooseFile = {
                openComposeFileChooser("Choose file to share by LAN browser link")?.let { filePath = it.toString() }
            },
            onCreateFile = {
                if (!quickShareState.expirationPolicyValid || !quickShareState.accessPolicyValid) return@QuickShareCreateLinksPanel
                val minutes = quickShareState.effectiveExpirationMinutes
                val limit = quickShareState.effectiveAccessLimit
                hostAdapter.createFileQuickShare(Path.of(filePath), minutes, limit)
            },
            textDraft = textDraft,
            onTextDraftChange = { textDraft = it },
            onCreateText = {
                if (!quickShareState.expirationPolicyValid || !quickShareState.accessPolicyValid) return@QuickShareCreateLinksPanel
                val minutes = quickShareState.effectiveExpirationMinutes
                val limit = quickShareState.effectiveAccessLimit
                hostAdapter.createTextQuickShare(textDraft, minutes, limit)
            },
        )

        QuickShareAdvancedPanel(
            state = quickShareState,
            port = quickSharePort,
            onPortChange = { quickSharePort = it },
            expirationMinutes = expirationMinutes,
            onExpirationMinutesChange = { expirationMinutes = it },
            accessLimit = accessLimit,
            onAccessLimitChange = { accessLimit = it },
            useCustomPort = useCustomPort,
            onUseCustomPortChange = { useCustomPort = it },
            noExpiration = noExpiration,
            onNoExpirationChange = { noExpiration = it },
            unlimitedAccess = unlimitedAccess,
            onUnlimitedAccessChange = { unlimitedAccess = it },
        )

        QuickShareLinksPanel(
            state = quickShareState,
            onCopy = { copyToSystemClipboard(it) },
            onOpen = { openInBrowser(it) },
            onStop = { hostAdapter.stopQuickShareEntry(it) },
        )
    }
}
