package com.shterneregen.securelan.desktop.compose

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.shterneregen.securelan.desktop.compose.tray.DesktopCloseOutcome
import com.shterneregen.securelan.desktop.compose.tray.DesktopChatTrayNotification
import com.shterneregen.securelan.desktop.compose.tray.DesktopTrayMenuState
import com.shterneregen.securelan.desktop.compose.tray.SecureLanTray
import com.shterneregen.securelan.desktop.compose.tray.desktopCloseOutcome
import com.shterneregen.securelan.webrtc.event.RtcEvent
import com.shterneregen.securelan.webrtc.service.RtcEventPublisher
import com.shterneregen.securelan.webrtc.service.RtcSignalingGateway
import com.shterneregen.securelan.webrtc.service.impl.DefaultRtcMediaDeviceService
import com.shterneregen.securelan.webrtc.service.impl.DefaultRtcSessionService
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import kotlinx.coroutines.channels.Channel
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
        val chatNotificationChannel = remember { Channel<DesktopChatTrayNotification>(Channel.UNLIMITED) }
        var unreadMessageCount by remember { mutableStateOf(0) }

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
                chatNotificationPublisher = { notification -> chatNotificationChannel.trySend(notification) },
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
        val traySupported = isTraySupported
        val trayState = rememberTrayState()
        var windowVisible by remember { mutableStateOf(true) }
        var trayHintShown by remember { mutableStateOf(false) }
        var mainWindow by remember { mutableStateOf<java.awt.Window?>(null) }

        LaunchedEffect(chatNotificationChannel, traySupported) {
            for (notification in chatNotificationChannel) {
                val userIsLookingAtChat = windowVisible && mainWindow?.isActive == true
                val notificationSettings = settingsController.settings.notifications
                val chatNotificationsEnabled = notificationSettings.enabled &&
                    notificationSettings.messageNotificationsEnabled
                if (!userIsLookingAtChat && traySupported && chatNotificationsEnabled) {
                    unreadMessageCount = (unreadMessageCount + 1).coerceAtMost(MAX_TRAY_UNREAD_COUNT)
                    trayState.sendNotification(
                        Notification(
                            title = notification.title,
                            message = notification.message,
                            type = if (notificationSettings.soundsEnabled) {
                                Notification.Type.Info
                            } else {
                                Notification.Type.None
                            },
                        ),
                    )
                }
            }
        }

        fun saveWindowState() {
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
        }

        fun showWindow() {
            unreadMessageCount = 0
            windowVisible = true
            windowState.isMinimized = false
            mainWindow?.apply {
                isVisible = true
                toFront()
                requestFocus()
            }
        }

        fun hideWindow(showHint: Boolean) {
            saveWindowState()
            windowVisible = false
            if (showHint && !trayHintShown) {
                trayHintShown = true
                trayState.sendNotification(
                    Notification(
                        title = ComposeShellMetadata.APP_NAME,
                        message = "SecureLanSuite is still running. Use the tray icon to reopen or exit.",
                        type = Notification.Type.Info,
                    ),
                )
            }
        }

        fun quitApplication() {
            saveWindowState()
            hostAdapter.shutdown()
            exitApplication()
        }

        fun setMessageNotifications(enabled: Boolean) {
            settingsController.update { settings ->
                val notifications = settings.notifications
                settings.copy(
                    notifications = notifications.copy(
                        enabled = if (enabled) true else notifications.enabled,
                        messageNotificationsEnabled = enabled,
                    ),
                )
            }
            if (!enabled) unreadMessageCount = 0
        }

        fun setKeepRunningAfterClose(enabled: Boolean) {
            settingsController.update { settings ->
                settings.copy(
                    lifecycle = settings.lifecycle.copy(
                        keepRunningAfterWindowClose = enabled,
                    ),
                )
            }
        }

        if (traySupported) {
            SecureLanTray(
                icon = appIcon,
                trayState = trayState,
                menuState = DesktopTrayMenuState(
                    windowVisible = windowVisible,
                    localServerRunning = hostAdapter.statusState.localServerRunning,
                    clientConnected = hostAdapter.statusState.clientConnected,
                    unreadMessageCount = unreadMessageCount,
                    messageNotificationsEnabled = settingsController.settings.notifications.enabled &&
                        settingsController.settings.notifications.messageNotificationsEnabled,
                    keepRunningAfterClose = settingsController.settings.lifecycle.keepRunningAfterWindowClose,
                ),
                onShowWindow = ::showWindow,
                onHideWindow = { hideWindow(showHint = false) },
                onMessageNotificationsChange = ::setMessageNotifications,
                onKeepRunningAfterCloseChange = ::setKeepRunningAfterClose,
                onExitApplication = ::quitApplication,
            )
        }

        Window(
            icon = appIcon,
            onCloseRequest = {
                when (
                    desktopCloseOutcome(
                        keepRunningAfterWindowClose = settingsController.settings.lifecycle.keepRunningAfterWindowClose,
                        traySupported = traySupported,
                    )
                ) {
                    DesktopCloseOutcome.HIDE_WINDOW -> hideWindow(showHint = true)
                    DesktopCloseOutcome.EXIT_APPLICATION -> quitApplication()
                }
            },
            state = windowState,
            title = ComposeShellMetadata.WINDOW_TITLE,
            visible = windowVisible,
        ) {
            DisposableEffect(window) {
                mainWindow = window
                val focusListener = object : WindowAdapter() {
                    override fun windowGainedFocus(event: WindowEvent) {
                        unreadMessageCount = 0
                    }
                }
                window.addWindowFocusListener(focusListener)
                onDispose {
                    window.removeWindowFocusListener(focusListener)
                    if (mainWindow === window) mainWindow = null
                }
            }
            SecureLanComposeApp(
                hostAdapter = hostAdapter,
                settingsController = settingsController,
            )
        }
    }
}

private const val MAX_TRAY_UNREAD_COUNT = 999

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
