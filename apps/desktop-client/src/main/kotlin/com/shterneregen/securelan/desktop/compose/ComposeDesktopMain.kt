package com.shterneregen.securelan.desktop.compose

import androidx.compose.runtime.remember
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.shterneregen.securelan.chat.discovery.impl.UdpBroadcastPeerDiscoveryService
import com.shterneregen.securelan.chat.service.ChatEventPublisher
import com.shterneregen.securelan.chat.service.impl.DefaultChatClientService
import com.shterneregen.securelan.chat.service.impl.DefaultChatServerService
import com.shterneregen.securelan.chat.service.impl.DefaultRandomNicknameService
import com.shterneregen.securelan.common.model.rtc.RtcSignalEnvelope
import com.shterneregen.securelan.filetransfer.service.impl.DefaultFileTransferClientService
import com.shterneregen.securelan.filetransfer.service.impl.DefaultFileTransferServerService
import com.shterneregen.securelan.filetransfer.quickshare.impl.DefaultQuickShareService
import com.shterneregen.securelan.webrtc.event.RtcEvent
import com.shterneregen.securelan.webrtc.service.RtcEventPublisher
import com.shterneregen.securelan.webrtc.service.RtcSignalingGateway
import com.shterneregen.securelan.webrtc.service.impl.DefaultRtcMediaDeviceService
import com.shterneregen.securelan.webrtc.service.impl.DefaultRtcSessionService

@Suppress("DEPRECATION")
fun main() = application {
    val appIcon = painterResource(ComposeDesktopResources.APP_ICON_PNG)

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
        ).also {
            sharedPublisher.delegate = it.chatEventPublisher
            fileTransferBridge.delegate = it.fileTransferEventPublisher
            quickShareBridge.delegate = it.quickShareEventPublisher
            rtcBridge.delegate = it.rtcEventPublisher
            signalingBridge.delegate = RtcSignalingGateway { signal -> it.chatClientServiceSendSignal(signal) }
        }
    }

    Window(
        icon = appIcon,
        onCloseRequest = {
            hostAdapter.shutdown()
            exitApplication()
        },
        state = rememberWindowState(
            width = ComposeShellMetadata.DEFAULT_WINDOW_WIDTH,
            height = ComposeShellMetadata.DEFAULT_WINDOW_HEIGHT,
        ),
        title = ComposeShellMetadata.WINDOW_TITLE,
    ) {
        SecureLanComposeApp(hostAdapter)
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
