package com.shterneregen.securelan.desktop.compose

import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.*
import com.shterneregen.securelan.chat.discovery.impl.UdpBroadcastPeerDiscoveryService
import com.shterneregen.securelan.chat.service.ChatEventPublisher
import com.shterneregen.securelan.chat.service.impl.DefaultChatClientService
import com.shterneregen.securelan.chat.service.impl.DefaultChatServerService
import com.shterneregen.securelan.chat.service.impl.DefaultRandomNicknameService
import com.shterneregen.securelan.common.model.rtc.RtcSignalEnvelope
import com.shterneregen.securelan.filetransfer.service.impl.DefaultFileTransferClientService
import com.shterneregen.securelan.filetransfer.service.impl.DefaultFileTransferServerService
import com.shterneregen.securelan.filetransfer.quickshare.impl.DefaultQuickShareService
import com.shterneregen.securelan.desktop.compose.state.shell.ComposeShellMetadata
import com.shterneregen.securelan.desktop.compose.settings.PropertiesDesktopAppSettingsStore
import com.shterneregen.securelan.desktop.compose.settings.DesktopAppSettingsController
import com.shterneregen.securelan.desktop.compose.settings.DesktopWindowSettings
import com.shterneregen.securelan.webrtc.event.RtcEvent
import com.shterneregen.securelan.webrtc.service.RtcEventPublisher
import com.shterneregen.securelan.webrtc.service.RtcSignalingGateway
import com.shterneregen.securelan.webrtc.service.impl.DefaultRtcMediaDeviceService
import com.shterneregen.securelan.webrtc.service.impl.DefaultRtcSessionService
import kotlin.math.roundToInt

@Suppress("DEPRECATION")
fun main() {
    val startupSettingsStore = PropertiesDesktopAppSettingsStore.default()
    val startupSettings = startupSettingsStore.load()
    DesktopLookAndFeel.install(startupSettings.themeMode)

    application {
    val appIcon = painterResource(ComposeDesktopResources.APP_ICON_PNG)
    val settingsStore = remember { startupSettingsStore }
    val settingsController = remember(settingsStore) {
        DesktopAppSettingsController(startupSettings, settingsStore)
    }
    val initialSettings = remember(settingsController) { settingsController.settings }

    val hostAdapter = remember {
        val sharedPublisher = ComposeDesktopChatEventBridge()
        val fileTransferBridge = ComposeDesktopFileTransferEventBridge()
        val quickShareBridge = ComposeDesktopQuickShareEventBridge()
        val rtcBridge = ComposeDesktopRtcEventBridge()
        val signalingBridge = ComposeDesktopRtcSignalingBridge()
        ComposeDesktopHostAdapter(
            chatServerService = DefaultChatServerService(sharedPublisher),
            chatClientService = DefaultChatClientService(sharedPublisher),
            fileTransferServerService = DefaultFileTransferServerService(fileTransferBridge),
            quickShareService = DefaultQuickShareService(quickShareBridge),
            discoveryService = UdpBroadcastPeerDiscoveryService(),
            randomNicknameService = DefaultRandomNicknameService(),
            fileTransferClientService = DefaultFileTransferClientService(fileTransferBridge),
            rtcSessionService = DefaultRtcSessionService(rtcBridge, signalingBridge),
            rtcMediaDeviceService = DefaultRtcMediaDeviceService(),
            settingsController = settingsController,
        ).also {
            sharedPublisher.delegate = it.chatEventPublisher
            fileTransferBridge.delegate = it.fileTransferEventPublisher
            quickShareBridge.delegate = it.quickShareEventPublisher
            rtcBridge.delegate = it.rtcEventPublisher
            signalingBridge.delegate = RtcSignalingGateway { signal -> it.chatClientServiceSendSignal(signal) }
        }
    }

    val initialWindow = initialSettings.window
    val windowState = rememberWindowState(
        placement = if (initialWindow.maximized) WindowPlacement.Maximized else WindowPlacement.Floating,
        position = if (initialWindow.x != null && initialWindow.y != null) {
            WindowPosition.Absolute(initialWindow.x.dp, initialWindow.y.dp)
        } else {
            WindowPosition.PlatformDefault
        },
        width = initialWindow.width.dp,
        height = initialWindow.height.dp,
    )

    Window(
        icon = appIcon,
        onCloseRequest = {
            val absolutePosition = windowState.position as? WindowPosition.Absolute
            settingsController.update { settings ->
                settings.copy(
                    window = DesktopWindowSettings(
                        width = windowState.size.width.value.roundToInt(),
                        height = windowState.size.height.value.roundToInt(),
                        x = absolutePosition?.x?.value?.roundToInt(),
                        y = absolutePosition?.y?.value?.roundToInt(),
                        maximized = windowState.placement == WindowPlacement.Maximized,
                    ),
                )
            }
            hostAdapter.shutdown()
            exitApplication()
        },
        state = windowState,
        title = ComposeShellMetadata.WINDOW_TITLE,
    ) {
        SecureLanComposeApp(
            hostAdapter = hostAdapter,
            settingsController = settingsController,
        )
    }
    }
}

private class ComposeDesktopChatEventBridge : ChatEventPublisher {
    var delegate: ChatEventPublisher = ChatEventPublisher { }

    override fun publish(event: com.shterneregen.securelan.chat.event.ChatCoreEvent) {
        delegate.publish(event)
    }
}

private class ComposeDesktopFileTransferEventBridge : com.shterneregen.securelan.filetransfer.service.FileTransferEventPublisher {
    var delegate: com.shterneregen.securelan.filetransfer.service.FileTransferEventPublisher =
        com.shterneregen.securelan.filetransfer.service.FileTransferEventPublisher { }

    override fun publish(event: com.shterneregen.securelan.filetransfer.event.FileTransferEvent) {
        delegate.publish(event)
    }
}

private class ComposeDesktopQuickShareEventBridge : com.shterneregen.securelan.filetransfer.quickshare.QuickShareEventPublisher {
    var delegate: com.shterneregen.securelan.filetransfer.quickshare.QuickShareEventPublisher =
        com.shterneregen.securelan.filetransfer.quickshare.QuickShareEventPublisher.noOp()

    override fun publish(event: com.shterneregen.securelan.filetransfer.quickshare.QuickShareEvent) {
        delegate.publish(event)
    }
}

private class ComposeDesktopRtcEventBridge : RtcEventPublisher {
    var delegate: RtcEventPublisher = RtcEventPublisher { }

    override fun publish(event: RtcEvent) {
        delegate.publish(event)
    }
}

private class ComposeDesktopRtcSignalingBridge : RtcSignalingGateway {
    var delegate: RtcSignalingGateway = RtcSignalingGateway { }

    override fun send(signal: RtcSignalEnvelope) {
        delegate.send(signal)
    }
}
