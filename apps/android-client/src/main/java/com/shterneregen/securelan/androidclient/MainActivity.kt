package com.shterneregen.securelan.androidclient

import android.annotation.SuppressLint
import android.Manifest
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Wifi
import com.shterneregen.securelan.androidclient.model.AppLogEntry
import com.shterneregen.securelan.androidclient.model.AppLanguage
import com.shterneregen.securelan.androidclient.model.ChatLine
import com.shterneregen.securelan.androidclient.model.DiscoveredPeer
import com.shterneregen.securelan.androidclient.model.MainUiState
import com.shterneregen.securelan.androidclient.model.NearbyPermissionState
import com.shterneregen.securelan.androidclient.model.PeerRole
import com.shterneregen.securelan.androidclient.model.SecureLanPorts
import com.shterneregen.securelan.androidclient.model.ThemeMode
import com.shterneregen.securelan.androidclient.model.TransferDirection
import com.shterneregen.securelan.androidclient.model.TransferRecord
import com.shterneregen.securelan.androidclient.model.TransferResult
import com.shterneregen.securelan.androidclient.ui.AndroidClipboard
import com.shterneregen.securelan.androidclient.ui.AndroidUiFormatters
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                viewModel.updateNearbyPermission(if (granted) NearbyPermissionState.GRANTED else NearbyPermissionState.DENIED)
            }
            val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                viewModel.updateNotificationsEnabled(granted)
            }
            LaunchedEffect(Unit) {
                val nearbyState = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    NearbyPermissionState.NOT_REQUIRED
                } else if (checkSelfPermission(Manifest.permission.NEARBY_WIFI_DEVICES) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    NearbyPermissionState.GRANTED
                } else {
                    NearbyPermissionState.REQUIRED
                }
                viewModel.updateNearbyPermission(nearbyState)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    viewModel.updateNotificationsEnabled(false)
                }
            }
            DisposableEffect(Unit) {
                val connectivityManager = getSystemService(ConnectivityManager::class.java)
                fun updateNetworkState() {
                    val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
                    val available = capabilities?.let {
                        it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            it.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                    } == true
                    viewModel.updateNetworkAvailable(available)
                }
                val callback = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) = updateNetworkState()
                    override fun onLost(network: Network) = updateNetworkState()
                    override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) = updateNetworkState()
                }
                updateNetworkState()
                connectivityManager.registerDefaultNetworkCallback(callback)
                onDispose { connectivityManager.unregisterNetworkCallback(callback) }
            }
            SecureLanAndroidApp(
                viewModel = viewModel,
                onRequestNearbyPermission = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.NEARBY_WIFI_DEVICES)
                    }
                },
                onNotificationsChange = { enabled ->
                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.updateNotificationsEnabled(enabled)
                    }
                },
            )
        }
    }
}

@Composable
@SuppressLint("AppBundleLocaleChanges") // Language splitting is disabled in android.bundle.language.
private fun SecureLanAndroidApp(
    viewModel: MainViewModel,
    onRequestNearbyPermission: () -> Unit,
    onNotificationsChange: (Boolean) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val baseContext = LocalContext.current
    val activityResultRegistryOwner = requireNotNull(LocalActivityResultRegistryOwner.current) {
        "SecureLan must be hosted by an ActivityResultRegistryOwner"
    }
    val systemConfiguration = LocalConfiguration.current
    val systemLocaleTags = systemConfiguration.locales.toLanguageTags()
    val localizedContext = remember(state.appLanguage, systemLocaleTags) {
        when (state.appLanguage) {
            AppLanguage.SYSTEM -> baseContext
            AppLanguage.ENGLISH, AppLanguage.RUSSIAN -> {
                val locale = if (state.appLanguage == AppLanguage.ENGLISH) Locale.ENGLISH else Locale.forLanguageTag("ru")
                val configuration = Configuration(systemConfiguration).apply { setLocales(LocaleList(locale)) }
                baseContext.createConfigurationContext(configuration)
            }
        }
    }
    val darkTheme = when (state.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = if (darkTheme) SecureLanDarkColors else SecureLanLightColors
    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedContext.resources.configuration,
        LocalActivityResultRegistryOwner provides activityResultRegistryOwner,
    ) {
        MaterialTheme(colorScheme = colorScheme) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing),
                color = MaterialTheme.colorScheme.background,
            ) {
                RedesignedMainScreen(
                    state = state,
                    onNicknameChange = viewModel::updateNickname,
                    onPasswordChange = viewModel::updateSessionPassword,
                    onThemeModeChange = viewModel::updateThemeMode,
                    onLanguageChange = viewModel::updateAppLanguage,
                    onNotificationsChange = onNotificationsChange,
                    onAutoReceiveChange = viewModel::updateAutoReceiveFiles,
                    onPeerSelected = viewModel::selectPeer,
                    onConnect = viewModel::connectSelectedPeer,
                    onConnectManual = viewModel::connectManualPeer,
                    onDisconnect = viewModel::disconnect,
                    onInputChange = viewModel::updateInputMessage,
                    onSendMessage = viewModel::sendTextMessage,
                    onFileSelected = viewModel::selectFile,
                    onSendFile = viewModel::sendSelectedFile,
                    onRequestNearbyPermission = onRequestNearbyPermission,
                    onRetryDiscovery = viewModel::restartDiscovery,
                )
            }
        }
    }
}

private enum class AppDestination(val label: String) {
    CONNECTION("Connection"),
    CHAT("Chat"),
    FILES("Files"),
    SETTINGS("Settings"),
}

private enum class AdaptiveLayoutMode {
    COMPACT,
    MEDIUM,
    EXPANDED,
}

private data class AdaptiveLayoutSpec(
    val mode: AdaptiveLayoutMode,
    val contentMaxWidth: Dp,
    val chatMaxWidth: Dp,
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
    val paneSpacing: Dp,
) {
    val usesNavigationRail: Boolean
        get() = mode != AdaptiveLayoutMode.COMPACT

    val supportsTwoPane: Boolean
        get() = mode == AdaptiveLayoutMode.EXPANDED

    val supportsThreePaneWorkspace: Boolean
        get() = mode == AdaptiveLayoutMode.EXPANDED

    val chatBubbleWidthFraction: Float
        get() = when (mode) {
            AdaptiveLayoutMode.COMPACT -> 0.86f
            AdaptiveLayoutMode.MEDIUM -> 0.76f
            AdaptiveLayoutMode.EXPANDED -> 0.78f
        }
}

private fun adaptiveLayoutSpec(screenWidth: Dp): AdaptiveLayoutSpec = when {
    screenWidth < 600.dp -> AdaptiveLayoutSpec(
        mode = AdaptiveLayoutMode.COMPACT,
        contentMaxWidth = 720.dp,
        chatMaxWidth = 720.dp,
        horizontalPadding = 16.dp,
        verticalPadding = 12.dp,
        paneSpacing = 10.dp,
    )
    screenWidth < 840.dp -> AdaptiveLayoutSpec(
        mode = AdaptiveLayoutMode.MEDIUM,
        contentMaxWidth = 960.dp,
        chatMaxWidth = 840.dp,
        horizontalPadding = 20.dp,
        verticalPadding = 14.dp,
        paneSpacing = 14.dp,
    )
    else -> AdaptiveLayoutSpec(
        mode = AdaptiveLayoutMode.EXPANDED,
        contentMaxWidth = 1680.dp,
        chatMaxWidth = 1180.dp,
        horizontalPadding = 24.dp,
        verticalPadding = 16.dp,
        paneSpacing = 16.dp,
    )
}

private enum class PrimaryDestination(val labelRes: Int, val icon: ImageVector) {
    DEVICES(R.string.nav_devices, Icons.Outlined.Devices),
    CHAT(R.string.nav_chat, Icons.AutoMirrored.Outlined.Chat),
    FILES(R.string.nav_files, Icons.Outlined.Description),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RedesignedMainScreen(
    state: MainUiState,
    onNicknameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onNotificationsChange: (Boolean) -> Unit,
    onAutoReceiveChange: (Boolean) -> Unit,
    onPeerSelected: (DiscoveredPeer) -> Unit,
    onConnect: () -> Unit,
    onConnectManual: (String, String, String) -> Unit,
    onDisconnect: () -> Unit,
    onInputChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onFileSelected: (android.net.Uri) -> Unit,
    onSendFile: () -> Unit,
    onRequestNearbyPermission: () -> Unit,
    onRetryDiscovery: () -> Unit,
) {
    var destination by remember { mutableStateOf(PrimaryDestination.DEVICES) }
    var settingsVisible by remember { mutableStateOf(false) }
    var connectionSheetVisible by remember { mutableStateOf(false) }
    var manualConnectionSheetVisible by remember { mutableStateOf(false) }
    var discoveryHelpVisible by remember { mutableStateOf(false) }
    var logsVisible by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val friendlyErrorTitle = state.error?.let { stringResource(it.toFriendlyErrorTitleRes()) }
    val errorDetailsLabel = stringResource(R.string.error_details)
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            onFileSelected(uri)
            settingsVisible = false
            destination = PrimaryDestination.FILES
        }
    }

    BackHandler(enabled = settingsVisible) { settingsVisible = false }

    LaunchedEffect(state.connected) {
        if (state.connected) {
            connectionSheetVisible = false
            manualConnectionSheetVisible = false
            settingsVisible = false
            destination = PrimaryDestination.CHAT
        }
    }
    LaunchedEffect(friendlyErrorTitle) {
        if (friendlyErrorTitle != null) {
            val result = snackbarHostState.showSnackbar(
                message = friendlyErrorTitle,
                actionLabel = errorDetailsLabel,
                withDismissAction = true,
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) logsVisible = true
        }
    }

    if (connectionSheetVisible) {
        ConnectionBottomSheet(
            state = state,
            onNicknameChange = onNicknameChange,
            onPasswordChange = onPasswordChange,
            onConnect = onConnect,
            onDismiss = { if (!state.connecting) connectionSheetVisible = false },
        )
    }
    if (manualConnectionSheetVisible) {
        ManualConnectionBottomSheet(
            state = state,
            onNicknameChange = onNicknameChange,
            onPasswordChange = onPasswordChange,
            onConnect = onConnectManual,
            onDismiss = { if (!state.connecting) manualConnectionSheetVisible = false },
        )
    }
    if (discoveryHelpVisible) {
        AlertDialog(
            onDismissRequest = { discoveryHelpVisible = false },
            icon = { Icon(Icons.Outlined.Wifi, contentDescription = null) },
            title = { Text(stringResource(R.string.no_devices_title)) },
            text = { Text(stringResource(R.string.no_devices_message)) },
            confirmButton = {
                Button(onClick = {
                    discoveryHelpVisible = false
                    onRetryDiscovery()
                }) { Text(stringResource(R.string.search_again)) }
            },
            dismissButton = {
                TextButton(onClick = { discoveryHelpVisible = false }) { Text(stringResource(R.string.close)) }
            },
        )
    }
    if (logsVisible) LogsDialog(logs = state.logs, onDismiss = { logsVisible = false })

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val layoutSpec = adaptiveLayoutSpec(maxWidth)
        val showDeviceSidebar = maxWidth >= 1200.dp && !settingsVisible && destination != PrimaryDestination.DEVICES
        Scaffold(
            topBar = {
                RedesignedAppHeader(
                    state = state,
                    title = when {
                        settingsVisible -> stringResource(R.string.settings)
                        destination == PrimaryDestination.CHAT && state.connected -> state.connectionPeer?.nickname
                            ?: stringResource(R.string.chat_title)
                        else -> stringResource(destination.labelRes)
                    },
                    settingsVisible = settingsVisible,
                    onSettingsClick = { settingsVisible = !settingsVisible },
                )
            },
            bottomBar = {
                if (!layoutSpec.usesNavigationRail) {
                    RedesignedNavigationBar(
                        selected = destination,
                        onSelected = {
                            settingsVisible = false
                            destination = it
                        },
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (layoutSpec.usesNavigationRail) {
                    RedesignedNavigationRail(
                        selected = destination,
                        onSelected = {
                            settingsVisible = false
                            destination = it
                        },
                    )
                }
                if (showDeviceSidebar) {
                    DeviceSidebar(
                        state = state,
                        onPeerClick = {
                            onPeerSelected(it)
                            connectionSheetVisible = true
                        },
                        onOpenDevices = { destination = PrimaryDestination.DEVICES },
                        onConnectManual = { manualConnectionSheetVisible = true },
                        modifier = Modifier.width(304.dp),
                    )
                    Box(Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outlineVariant))
                }
                Box(Modifier.fillMaxSize()) {
                    if (settingsVisible) {
                        RedesignedSettingsScreen(
                            state = state,
                            onNicknameChange = onNicknameChange,
                            onThemeModeChange = onThemeModeChange,
                            onLanguageChange = onLanguageChange,
                            onNotificationsChange = onNotificationsChange,
                            onAutoReceiveChange = onAutoReceiveChange,
                            onOpenLogs = { logsVisible = true },
                            layoutSpec = layoutSpec,
                        )
                    } else when (destination) {
                        PrimaryDestination.DEVICES -> RedesignedDevicesScreen(
                            state = state,
                            onPeerClick = {
                                onPeerSelected(it)
                                connectionSheetVisible = true
                            },
                            onOpenChat = { destination = PrimaryDestination.CHAT },
                            onDisconnect = onDisconnect,
                            onRequestNearbyPermission = onRequestNearbyPermission,
                            onRetryDiscovery = onRetryDiscovery,
                            onShowHelp = { discoveryHelpVisible = true },
                            onConnectManual = { manualConnectionSheetVisible = true },
                            layoutSpec = layoutSpec,
                        )
                        PrimaryDestination.CHAT -> RedesignedChatScreen(
                            state = state,
                            onInputChange = onInputChange,
                            onSendMessage = onSendMessage,
                            onAttachFile = { filePicker.launch(arrayOf("*/*")) },
                            onChooseDevice = { destination = PrimaryDestination.DEVICES },
                            onReconnect = onConnect,
                            layoutSpec = layoutSpec,
                        )
                        PrimaryDestination.FILES -> RedesignedFilesScreen(
                            state = state,
                            onChooseFile = { filePicker.launch(arrayOf("*/*")) },
                            onSendFile = onSendFile,
                            onChooseDevice = { destination = PrimaryDestination.DEVICES },
                            layoutSpec = layoutSpec,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RedesignedAppHeader(
    state: MainUiState,
    title: String,
    settingsVisible: Boolean,
    onSettingsClick: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                RedesignedConnectionStatus(state)
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = if (settingsVisible) Icons.AutoMirrored.Outlined.ArrowBack else Icons.Outlined.Settings,
                    contentDescription = stringResource(if (settingsVisible) R.string.back else R.string.settings),
                )
            }
        }
    }
}

@Composable
private fun RedesignedConnectionStatus(state: MainUiState) {
    val (text, color) = when {
        state.error != null -> stringResource(R.string.status_attention) to MaterialTheme.colorScheme.error
        state.connected -> stringResource(R.string.status_connected, state.connectionPeer?.nickname.orEmpty()) to SuccessGreen
        state.connecting -> stringResource(R.string.status_connecting, state.connectionPeer?.nickname.orEmpty()) to MaterialTheme.colorScheme.tertiary
        state.discoveryRunning -> stringResource(R.string.status_searching) to MaterialTheme.colorScheme.primary
        else -> stringResource(R.string.status_offline) to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
private fun RedesignedNavigationBar(selected: PrimaryDestination, onSelected: (PrimaryDestination) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
        PrimaryDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = selected == destination,
                onClick = { onSelected(destination) },
                icon = { Icon(destination.icon, contentDescription = null) },
                label = { Text(stringResource(destination.labelRes)) },
                alwaysShowLabel = true,
            )
        }
    }
}

@Composable
private fun RedesignedNavigationRail(selected: PrimaryDestination, onSelected: (PrimaryDestination) -> Unit) {
    NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
        Spacer(Modifier.height(12.dp))
        PrimaryDestination.entries.forEach { destination ->
            NavigationRailItem(
                selected = selected == destination,
                onClick = { onSelected(destination) },
                icon = { Icon(destination.icon, contentDescription = null) },
                label = { Text(stringResource(destination.labelRes)) },
            )
        }
    }
}

@Composable
private fun RedesignedDevicesScreen(
    state: MainUiState,
    onPeerClick: (DiscoveredPeer) -> Unit,
    onOpenChat: () -> Unit,
    onDisconnect: () -> Unit,
    onRequestNearbyPermission: () -> Unit,
    onRetryDiscovery: () -> Unit,
    onShowHelp: () -> Unit,
    onConnectManual: () -> Unit,
    layoutSpec: AdaptiveLayoutSpec,
) {
    val serverPeers = state.peers.filter { it.role == PeerRole.SERVER }
    if (layoutSpec.supportsTwoPane) {
        Row(
            modifier = Modifier.fillMaxSize().padding(layoutSpec.horizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                Modifier.weight(0.9f).fillMaxHeight().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DevicesSubtitle()
                if (state.connected) CurrentDeviceCard(state, onOpenChat, onDisconnect)
                DiscoveryStateCard(state, onRequestNearbyPermission, onRetryDiscovery, onShowHelp)
                ManualConnectionButton(onConnectManual)
            }
            Column(
                Modifier.weight(1.1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DevicesSectionHeader(serverPeers.size)
                serverPeers.forEach { peer ->
                    val connected = state.connected && state.connectionPeer?.peerId == peer.peerId
                    DeviceCard(peer, connected) { selected -> if (connected) onOpenChat() else onPeerClick(selected) }
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(layoutSpec.horizontalPadding, 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { DevicesSubtitle() }
            if (state.connected) item { CurrentDeviceCard(state, onOpenChat, onDisconnect) }
            item { DiscoveryStateCard(state, onRequestNearbyPermission, onRetryDiscovery, onShowHelp) }
            item { ManualConnectionButton(onConnectManual) }
            if (serverPeers.isNotEmpty()) {
                item { DevicesSectionHeader(serverPeers.size) }
                items(serverPeers) { peer ->
                    val connected = state.connected && state.connectionPeer?.peerId == peer.peerId
                    DeviceCard(peer, connected) { selected -> if (connected) onOpenChat() else onPeerClick(selected) }
                }
            }
        }
    }
}

@Composable
private fun DevicesSubtitle() {
    Text(
        text = stringResource(R.string.devices_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun PageHeading(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CurrentDeviceCard(state: MainUiState, onOpenChat: () -> Unit, onDisconnect: () -> Unit) {
    val peer = state.connectionPeer ?: return
    RedesignedCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.Computer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.current_device), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(peer.nickname, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(peer.host, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = SuccessGreen)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onOpenChat, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.open_chat)) }
            OutlinedButton(onClick = onDisconnect, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.disconnect)) }
        }
    }
}

@Composable
private fun DiscoveryStateCard(
    state: MainUiState,
    onRequestPermission: () -> Unit,
    onRetry: () -> Unit,
    onShowHelp: () -> Unit,
) {
    if (!state.networkAvailable) {
        StateCard(
            icon = Icons.Outlined.Wifi,
            title = stringResource(R.string.network_unavailable_title),
            message = stringResource(R.string.network_unavailable_message),
            actionLabel = stringResource(R.string.search_again),
            onAction = onRetry,
        )
        return
    }
    when (state.nearbyPermissionState) {
        NearbyPermissionState.REQUIRED, NearbyPermissionState.DENIED -> {
            val denied = state.nearbyPermissionState == NearbyPermissionState.DENIED
            StateCard(
                icon = Icons.Outlined.Wifi,
                title = stringResource(if (denied) R.string.permission_denied_title else R.string.permission_title),
                message = stringResource(if (denied) R.string.permission_denied_message else R.string.permission_message),
                actionLabel = stringResource(R.string.allow),
                onAction = onRequestPermission,
            )
        }
        else -> {
            val hasServers = state.peers.any { it.role == PeerRole.SERVER }
            if (!hasServers) {
                if (state.discoveryRunning && !state.discoveryTimedOut) {
                    StateCard(
                        icon = Icons.Outlined.Wifi,
                        title = stringResource(R.string.searching_title),
                        message = stringResource(R.string.searching_message),
                        actionLabel = stringResource(R.string.search_again),
                        onAction = onRetry,
                    )
                } else {
                    StateCard(
                        icon = Icons.Outlined.ErrorOutline,
                        title = stringResource(R.string.no_devices_title),
                        message = stringResource(R.string.no_devices_message),
                        actionLabel = stringResource(R.string.search_again),
                        onAction = onShowHelp,
                    )
                }
            }
        }
    }
}

@Composable
private fun StateCard(icon: ImageVector, title: String, message: String, actionLabel: String, onAction: () -> Unit) {
    RedesignedCard {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        FilledTonalButton(onClick = onAction, modifier = Modifier.fillMaxWidth()) { Text(actionLabel) }
    }
}

@Composable
private fun DevicesSectionHeader(count: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.available_devices), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(pluralStringResource(R.plurals.device_count, count, count), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DeviceCard(peer: DiscoveredPeer, connected: Boolean, onClick: (DiscoveredPeer) -> Unit) {
    Card(
        onClick = { onClick(peer) },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, if (connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(Icons.Outlined.Computer, contentDescription = null, modifier = Modifier.padding(10.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(peer.nickname, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("SecureLan Desktop · ${peer.host}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            Text(if (connected) "✓" else stringResource(R.string.connect), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ManualConnectionButton(onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().height(50.dp)) {
        Icon(Icons.Outlined.Computer, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.connect_by_ip))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConnectionBottomSheet(
    state: MainUiState,
    onNicknameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConnect: () -> Unit,
    onDismiss: () -> Unit,
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val peer = state.connectionPeer
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(R.string.connect_to, peer?.nickname.orEmpty()), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.connect_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = state.nickname,
                onValueChange = onNicknameChange,
                label = { Text(stringResource(R.string.nickname)) },
                enabled = !state.connecting,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.sessionPassword,
                onValueChange = onPasswordChange,
                label = { Text(stringResource(R.string.session_password)) },
                enabled = !state.connecting,
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = stringResource(if (passwordVisible) R.string.hide_password else R.string.show_password),
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = onConnect,
                enabled = peer != null && state.nickname.isNotBlank() && !state.connecting && !state.connected,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text(stringResource(if (state.connecting) R.string.connecting else R.string.connect)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManualConnectionBottomSheet(
    state: MainUiState,
    onNicknameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConnect: (String, String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var host by remember { mutableStateOf(state.manualHost) }
    var chatPort by remember { mutableStateOf(state.manualChatPort) }
    var filePort by remember { mutableStateOf(state.manualFilePort) }
    var passwordVisible by remember { mutableStateOf(false) }
    val normalizedHost = host.trim().removeSurrounding("[", "]")
    val hostValid = normalizedHost.isNotBlank() &&
        normalizedHost.none { it.isWhitespace() } &&
        "://" !in normalizedHost &&
        '/' !in normalizedHost
    val chatPortValid = chatPort.toIntOrNull()?.let { it in 1..65535 } == true
    val filePortValid = filePort.toIntOrNull()?.let { it in 1..65535 } == true

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(R.string.manual_connection_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.manual_connection_message), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = host,
                onValueChange = { host = it },
                label = { Text(stringResource(R.string.manual_host)) },
                enabled = !state.connecting,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                isError = host.isNotEmpty() && !hostValid,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = chatPort,
                    onValueChange = { chatPort = it.filter(Char::isDigit).take(5) },
                    label = { Text(stringResource(R.string.chat_port)) },
                    enabled = !state.connecting,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = chatPort.isNotEmpty() && !chatPortValid,
                    supportingText = if (chatPort.isNotEmpty() && !chatPortValid) {
                        { Text(stringResource(R.string.port_range_hint)) }
                    } else null,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = filePort,
                    onValueChange = { filePort = it.filter(Char::isDigit).take(5) },
                    label = { Text(stringResource(R.string.file_port)) },
                    enabled = !state.connecting,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = filePort.isNotEmpty() && !filePortValid,
                    supportingText = if (filePort.isNotEmpty() && !filePortValid) {
                        { Text(stringResource(R.string.port_range_hint)) }
                    } else null,
                    modifier = Modifier.weight(1f),
                )
            }
            OutlinedTextField(
                value = state.nickname,
                onValueChange = onNicknameChange,
                label = { Text(stringResource(R.string.nickname)) },
                enabled = !state.connecting,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.sessionPassword,
                onValueChange = onPasswordChange,
                label = { Text(stringResource(R.string.session_password)) },
                enabled = !state.connecting,
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = stringResource(if (passwordVisible) R.string.hide_password else R.string.show_password),
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { onConnect(normalizedHost, chatPort, filePort) },
                enabled = hostValid && chatPortValid && filePortValid && state.nickname.isNotBlank() && !state.connecting && !state.connected,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text(stringResource(if (state.connecting) R.string.connecting else R.string.connect)) }
        }
    }
}

@Composable
private fun RedesignedChatScreen(
    state: MainUiState,
    onInputChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onAttachFile: () -> Unit,
    onChooseDevice: () -> Unit,
    onReconnect: () -> Unit,
    layoutSpec: AdaptiveLayoutSpec,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.lastIndex)
    }
    Column(
        modifier = Modifier.fillMaxSize().widthIn(max = layoutSpec.chatMaxWidth).padding(horizontal = layoutSpec.horizontalPadding),
    ) {
        if (!state.connected && state.messages.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                RedesignedEmptyState(
                    icon = Icons.AutoMirrored.Outlined.Chat,
                    title = stringResource(R.string.connect_first_title),
                    message = stringResource(R.string.connect_first_message),
                    action = stringResource(R.string.choose_device),
                    onAction = onChooseDevice,
                )
            }
            return@Column
        }
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.messages.isEmpty()) {
                item {
                    RedesignedEmptyState(
                        icon = Icons.AutoMirrored.Outlined.Chat,
                        title = stringResource(R.string.no_messages_title),
                        message = stringResource(R.string.no_messages_message),
                    )
                }
            } else {
                items(state.messages) { ChatBubble(it, layoutSpec.chatBubbleWidthFraction) }
            }
        }
        if (state.connected) {
            RedesignedChatComposer(state, onInputChange, onSendMessage, onAttachFile)
        } else {
            Button(onClick = if (state.connectionPeer != null) onReconnect else onChooseDevice, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Text(stringResource(if (state.connectionPeer != null) R.string.reconnect else R.string.choose_device))
            }
        }
    }
}

@Composable
private fun RedesignedChatComposer(
    state: MainUiState,
    onInputChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onAttachFile: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).imePadding(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onAttachFile) { Icon(Icons.Outlined.AttachFile, stringResource(R.string.attach_file)) }
            OutlinedTextField(
                value = state.inputMessage,
                onValueChange = onInputChange,
                placeholder = { Text(stringResource(R.string.message_placeholder)) },
                modifier = Modifier.weight(1f),
                minLines = 1,
                maxLines = 4,
            )
            IconButton(onClick = onSendMessage, enabled = state.inputMessage.isNotBlank()) {
                Icon(Icons.AutoMirrored.Outlined.Send, stringResource(R.string.send_message), tint = if (state.inputMessage.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
private fun RedesignedFilesScreen(
    state: MainUiState,
    onChooseFile: () -> Unit,
    onSendFile: () -> Unit,
    onChooseDevice: () -> Unit,
    layoutSpec: AdaptiveLayoutSpec,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().widthIn(max = layoutSpec.contentMaxWidth),
        contentPadding = PaddingValues(layoutSpec.horizontalPadding, 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            PageHeading(
                stringResource(R.string.files_title),
                state.connectionPeer?.let { stringResource(R.string.recipient, it.nickname) }
                    ?: stringResource(R.string.files_connect_first),
            )
        }
        if (!state.connected) {
            item {
                RedesignedEmptyState(
                    Icons.Outlined.Folder,
                    stringResource(R.string.connect_first_title),
                    stringResource(R.string.files_connect_first),
                    stringResource(R.string.choose_device),
                    onChooseDevice,
                )
            }
        } else {
            item { FilePickerCard(state, onChooseFile, onSendFile) }
            item { ReceiverReadyCard(state) }
            if (state.fileProgress.active || state.incomingFileProgress.active) item { ActiveTransferCard(state) }
        }
        item {
            Text(stringResource(R.string.recent_transfers), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        if (state.recentTransfers.isEmpty()) {
            item { Text(stringResource(R.string.no_transfers), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium) }
        } else {
            items(state.recentTransfers) { TransferHistoryRow(it) }
        }
    }
}

@Composable
private fun FilePickerCard(state: MainUiState, onChooseFile: () -> Unit, onSendFile: () -> Unit) {
    RedesignedCard {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    if (state.selectedFile == null) stringResource(R.string.choose_file_title) else stringResource(R.string.selected_file),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    state.selectedFile?.name ?: stringResource(R.string.choose_file_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                state.selectedFile?.let {
                    Text(AndroidUiFormatters.formatBytes(it.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        state.connectionPeer?.let {
            Text(stringResource(R.string.file_will_be_sent, it.nickname), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onChooseFile, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.choose_file)) }
            Button(
                onClick = onSendFile,
                enabled = state.selectedFile != null && !state.fileProgress.active,
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(R.string.send_file)) }
        }
    }
}

@Composable
private fun ReceiverReadyCard(state: MainUiState) {
    val ready = state.autoReceiveFiles && state.fileReceiverRunning
    val unavailable = state.autoReceiveFiles && !state.fileReceiverRunning
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (ready) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (ready) Icons.Outlined.CheckCircle else Icons.Outlined.Close, null, tint = if (ready) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant)
            Column {
                Text(
                    stringResource(
                        when {
                            ready -> R.string.receiving_ready
                            unavailable -> R.string.receiving_unavailable
                            else -> R.string.receiving_off
                        },
                    ),
                    fontWeight = FontWeight.SemiBold,
                )
                if (ready) Text(stringResource(R.string.receiving_ready_message), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ActiveTransferCard(state: MainUiState) {
    val incoming = state.incomingFileProgress.active
    val title = if (incoming) state.incomingFileProgress.fileName else state.fileProgress.fileName
    val progress = if (incoming) state.incomingFileProgress.percent else state.fileProgress.percent
    RedesignedCard {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun TransferHistoryRow(record: TransferRecord) {
    val failed = record.result == TransferResult.FAILED
    RedesignedCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(
                if (failed) Icons.Outlined.ErrorOutline else Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = if (failed) MaterialTheme.colorScheme.error else SuccessGreen,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(record.fileName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (failed) stringResource(R.string.transfer_failed)
                    else stringResource(if (record.direction == TransferDirection.SENT) R.string.transfer_sent else R.string.transfer_received),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                record.savedPath?.let { Text(stringResource(R.string.transfer_saved_to, it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Text(AndroidUiFormatters.formatBytes(record.bytes), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RedesignedSettingsScreen(
    state: MainUiState,
    onNicknameChange: (String) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onNotificationsChange: (Boolean) -> Unit,
    onAutoReceiveChange: (Boolean) -> Unit,
    onOpenLogs: () -> Unit,
    layoutSpec: AdaptiveLayoutSpec,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().widthIn(max = 760.dp),
        contentPadding = PaddingValues(layoutSpec.horizontalPadding, 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SettingsGroup(stringResource(R.string.settings_profile)) {
                Text(stringResource(R.string.settings_profile_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = state.nickname,
                    onValueChange = onNicknameChange,
                    label = { Text(stringResource(R.string.nickname)) },
                    enabled = !state.connected,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        item {
            SettingsGroup(stringResource(R.string.settings_language)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppLanguage.entries.forEach { language ->
                        FilterChip(
                            selected = state.appLanguage == language,
                            onClick = { onLanguageChange(language) },
                            label = {
                                Text(
                                    stringResource(
                                        when (language) {
                                            AppLanguage.SYSTEM -> R.string.language_system
                                            AppLanguage.ENGLISH -> R.string.language_english
                                            AppLanguage.RUSSIAN -> R.string.language_russian
                                        },
                                    ),
                                )
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        item {
            SettingsGroup(stringResource(R.string.settings_theme)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = state.themeMode == mode,
                            onClick = { onThemeModeChange(mode) },
                            label = {
                                Text(stringResource(when (mode) {
                                    ThemeMode.SYSTEM -> R.string.theme_system
                                    ThemeMode.LIGHT -> R.string.theme_light
                                    ThemeMode.DARK -> R.string.theme_dark
                                }))
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        item {
            SettingsGroup(stringResource(R.string.nav_files)) {
                ToggleSettingRow(
                    stringResource(R.string.settings_notifications),
                    stringResource(R.string.settings_notifications_hint),
                    state.notificationsEnabled,
                    onNotificationsChange,
                )
                HorizontalDivider()
                ToggleSettingRow(
                    stringResource(R.string.settings_auto_receive),
                    stringResource(R.string.settings_auto_receive_hint),
                    state.autoReceiveFiles,
                    onAutoReceiveChange,
                )
                HorizontalDivider()
                SettingsValueRow(stringResource(R.string.settings_download_folder), stringResource(R.string.settings_download_folder_value))
            }
        }
        item {
            SettingsGroup(stringResource(R.string.settings_diagnostics)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_logs), fontWeight = FontWeight.SemiBold)
                        Text(pluralStringResource(R.plurals.settings_logs_count, state.logs.size, state.logs.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OutlinedButton(onClick = onOpenLogs) { Text(stringResource(R.string.open)) }
                }
                HorizontalDivider()
                SettingsValueRow(
                    stringResource(R.string.network_ports),
                    stringResource(R.string.network_ports_value, SecureLanPorts.DEFAULT_CHAT_PORT, SecureLanPorts.DEFAULT_FILE_TRANSFER_PORT, SecureLanPorts.DEFAULT_DISCOVERY_PORT),
                )
            }
        }
        item {
            SettingsGroup(stringResource(R.string.about)) {
                SettingsValueRow(stringResource(R.string.app_name), stringResource(R.string.about_value))
            }
        }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    RedesignedCard {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        content()
    }
}

@Composable
private fun ToggleSettingRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsValueRow(title: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold)
        Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RedesignedEmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(42.dp), tint = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (action != null && onAction != null) Button(onClick = onAction) { Text(action) }
    }
}

@Composable
private fun RedesignedCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
private fun DeviceSidebar(
    state: MainUiState,
    onPeerClick: (DiscoveredPeer) -> Unit,
    onOpenDevices: () -> Unit,
    onConnectManual: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxHeight().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.devices_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            IconButton(onClick = onOpenDevices) { Icon(Icons.Outlined.Refresh, stringResource(R.string.search_again)) }
        }
        OutlinedButton(onClick = onConnectManual, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.connect_by_ip))
        }
        state.peers.filter { it.role == PeerRole.SERVER }.forEach { peer ->
            val connected = state.connected && state.connectionPeer?.peerId == peer.peerId
            DeviceCard(peer, connected) { selected -> if (connected) onOpenDevices() else onPeerClick(selected) }
        }
    }
}

@Composable
private fun MainScreen(
    state: MainUiState,
    onNicknameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onDarkThemeChange: (Boolean) -> Unit,
    onPeerSelected: (DiscoveredPeer) -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onInputChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onFileSelected: (android.net.Uri) -> Unit,
    onSendFile: () -> Unit,
    onStartFileReceiver: () -> Unit,
    onStopFileReceiver: () -> Unit,
) {
    var selectedDestination by remember { mutableStateOf(AppDestination.CONNECTION) }
    var logsVisible by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onFileSelected(uri)
    }
    val friendlyError = state.error?.toFriendlyError()

    if (logsVisible) {
        LogsDialog(logs = state.logs, onDismiss = { logsVisible = false })
    }

    LaunchedEffect(friendlyError) {
        if (friendlyError != null) {
            val result = snackbarHostState.showSnackbar(
                message = friendlyError.title,
                actionLabel = "Logs",
                withDismissAction = true,
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) {
                logsVisible = true
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val layoutSpec = adaptiveLayoutSpec(this.maxWidth)
        Scaffold(
            topBar = {
                AppHeader(
                    state = state,
                    friendlyError = friendlyError,
                    onOpenLogs = { logsVisible = true },
                    layoutSpec = layoutSpec,
                )
            },
            bottomBar = {
                if (!layoutSpec.usesNavigationRail) {
                    AppNavigationBar(
                        selectedDestination = selectedDestination,
                        onDestinationSelected = { selectedDestination = it },
                    )
                }
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.navigationBars.exclude(WindowInsets.ime),
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (layoutSpec.usesNavigationRail) {
                    AppNavigationRail(
                        selectedDestination = selectedDestination,
                        onDestinationSelected = { selectedDestination = it },
                    )
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    if (layoutSpec.supportsThreePaneWorkspace && selectedDestination != AppDestination.SETTINGS) {
                        ThreePaneWorkspace(
                            state = state,
                            onNicknameChange = onNicknameChange,
                            onPasswordChange = onPasswordChange,
                            onPeerSelected = onPeerSelected,
                            onConnect = onConnect,
                            onDisconnect = onDisconnect,
                            onInputChange = onInputChange,
                            onSendMessage = onSendMessage,
                            onPickFile = { filePicker.launch(arrayOf("*/*")) },
                            onSendFile = onSendFile,
                            onStartFileReceiver = onStartFileReceiver,
                            onStopFileReceiver = onStopFileReceiver,
                            layoutSpec = layoutSpec,
                        )
                    } else when (selectedDestination) {
                        AppDestination.CONNECTION -> ConnectionScreen(
                            state = state,
                            onNicknameChange = onNicknameChange,
                            onPasswordChange = onPasswordChange,
                            onPeerSelected = onPeerSelected,
                            onConnect = onConnect,
                            onDisconnect = onDisconnect,
                            layoutSpec = layoutSpec,
                        )
                        AppDestination.CHAT -> ChatScreen(
                            state = state,
                            onInputChange = onInputChange,
                            onSendMessage = onSendMessage,
                            layoutSpec = layoutSpec,
                        )
                        AppDestination.FILES -> FilesScreen(
                            state = state,
                            onPeerSelected = onPeerSelected,
                            onPickFile = { filePicker.launch(arrayOf("*/*")) },
                            onSendFile = onSendFile,
                            onStartFileReceiver = onStartFileReceiver,
                            onStopFileReceiver = onStopFileReceiver,
                            layoutSpec = layoutSpec,
                        )
                        AppDestination.SETTINGS -> SettingsScreen(
                            state = state,
                            onDarkThemeChange = onDarkThemeChange,
                            onOpenLogs = { logsVisible = true },
                            layoutSpec = layoutSpec,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppHeader(state: MainUiState, friendlyError: FriendlyError?, onOpenLogs: () -> Unit, layoutSpec: AdaptiveLayoutSpec) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 1.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = layoutSpec.horizontalPadding, vertical = 12.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = layoutSpec.contentMaxWidth),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "SecureLan",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = state.connectionPeer?.let { "Target: ${it.nickname} · ${it.host}:${it.chatPort}" }
                                ?: "Encrypted LAN chat and file transfer",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    StatusText(text = if (state.connected) "Connected" else "Offline", active = state.connected)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    StatusChip(
                        text = state.status.toFriendlyStatus(),
                        active = state.connected || state.discoveryRunning,
                        modifier = Modifier.weight(1.35f),
                    )
                    StatusChip(
                        text = if (state.connected) "Session active" else "Session inactive",
                        active = state.connected,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (friendlyError != null) {
                    FriendlyErrorBanner(error = friendlyError, onOpenLogs = onOpenLogs)
                }
            }
        }
    }
}

@Composable
private fun FriendlyErrorBanner(error: FriendlyError, onOpenLogs: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(error.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Text(error.message, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onOpenLogs) { Text("Logs") }
        }
    }
}

@Composable
private fun StatusChip(text: String, active: Boolean, modifier: Modifier = Modifier) {
    val background = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val foreground = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = modifier.semantics {
            contentDescription = text
            stateDescription = if (active) "active" else "inactive"
        },
        color = background.copy(alpha = 0.9f),
        contentColor = foreground,
        shape = RoundedCornerShape(100.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusDot(active = active)
            Spacer(modifier = Modifier.width(6.dp))
            Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun StatusText(text: String, active: Boolean) {
    Row(
        modifier = Modifier.semantics {
            contentDescription = "Connection status: $text"
            stateDescription = if (active) "connected" else "offline"
        },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusDot(active = active)
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AppNavigationBar(selectedDestination: AppDestination, onDestinationSelected: (AppDestination) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
        AppDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = selectedDestination == destination,
                onClick = { onDestinationSelected(destination) },
                icon = { Icon(destination.navIcon(), contentDescription = null) },
                label = null,
                modifier = Modifier.semantics {
                    contentDescription = "Open ${destination.label}"
                    stateDescription = if (selectedDestination == destination) "selected" else "not selected"
                },
            )
        }
    }
}

@Composable
private fun AppNavigationRail(selectedDestination: AppDestination, onDestinationSelected: (AppDestination) -> Unit) {
    NavigationRail(
        modifier = Modifier.fillMaxHeight(),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        AppDestination.entries.forEach { destination ->
            NavigationRailItem(
                selected = selectedDestination == destination,
                onClick = { onDestinationSelected(destination) },
                icon = { Icon(destination.navIcon(), contentDescription = null) },
                label = { Text(destination.label) },
                modifier = Modifier.semantics {
                    contentDescription = "Open ${destination.label}"
                    stateDescription = if (selectedDestination == destination) "selected" else "not selected"
                },
            )
        }
    }
}

private fun AppDestination.navIcon(): ImageVector = when (this) {
    AppDestination.CONNECTION -> Icons.Outlined.Devices
    AppDestination.CHAT -> Icons.AutoMirrored.Outlined.Chat
    AppDestination.FILES -> Icons.Outlined.Description
    AppDestination.SETTINGS -> Icons.Outlined.Settings
}

@Composable
private fun ThreePaneWorkspace(
    state: MainUiState,
    onNicknameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPeerSelected: (DiscoveredPeer) -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onInputChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onPickFile: () -> Unit,
    onSendFile: () -> Unit,
    onStartFileReceiver: () -> Unit,
    onStopFileReceiver: () -> Unit,
    layoutSpec: AdaptiveLayoutSpec,
) {
    var helpVisible by remember { mutableStateOf(false) }
    if (helpVisible) {
        AlertDialog(
            onDismissRequest = { helpVisible = false },
            title = { Text("Connection help") },
            text = {
                Text(
                    "Choose a discovered desktop server, enter the nickname and the same session password as on desktop, then connect. If connection fails, check firewall, VPN, guest Wi‑Fi, and that the desktop room is still open.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = { Button(onClick = { helpVisible = false }) { Text("Got it") } },
        )
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = layoutSpec.horizontalPadding, vertical = layoutSpec.verticalPadding),
        contentAlignment = Alignment.TopCenter,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = layoutSpec.contentMaxWidth),
            horizontalArrangement = Arrangement.spacedBy(layoutSpec.paneSpacing),
        ) {
            Column(
                modifier = Modifier
                    .weight(0.30f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(layoutSpec.paneSpacing),
            ) {
                ConnectionFormCard(
                    state = state,
                    onNicknameChange = onNicknameChange,
                    onPasswordChange = onPasswordChange,
                    onConnect = onConnect,
                    onDisconnect = onDisconnect,
                    onOpenHelp = { helpVisible = true },
                )
                PeersCard(state = state, onPeerSelected = onPeerSelected)
            }
            WorkspaceChatPane(
                state = state,
                onInputChange = onInputChange,
                onSendMessage = onSendMessage,
                layoutSpec = layoutSpec,
                modifier = Modifier
                    .weight(0.42f)
                    .fillMaxHeight(),
            )
            Column(
                modifier = Modifier
                    .weight(0.28f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(layoutSpec.paneSpacing),
            ) {
                FileSelectedCard(state = state, onPickFile = onPickFile, onSendFile = onSendFile)
                FileReceiveCard(
                    state = state,
                    onStartFileReceiver = onStartFileReceiver,
                    onStopFileReceiver = onStopFileReceiver,
                )
                FileProgressCards(state)
            }
        }
    }
}

@Composable
private fun WorkspaceChatPane(
    state: MainUiState,
    onInputChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    layoutSpec: AdaptiveLayoutSpec,
    modifier: Modifier = Modifier,
) {
    val chatListState = rememberLazyListState()
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            chatListState.animateScrollToItem(state.messages.lastIndex)
        }
    }
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Chat", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    text = if (state.connected) "Secure session is active." else "Connect before sending messages.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                state = chatListState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.messages.isEmpty()) {
                    item {
                        EmptyState(
                            title = if (state.connected) "No messages yet" else "Connect first",
                            message = if (state.connected) {
                                "Your secure session is ready. Send the first message to start the conversation."
                            } else {
                                "Use the Connection pane before sending messages."
                            },
                        )
                    }
                } else {
                    items(state.messages) { line -> ChatBubble(line, widthFraction = layoutSpec.chatBubbleWidthFraction) }
                }
            }
            ChatInputBar(
                state = state,
                onInputChange = onInputChange,
                onSendMessage = onSendMessage,
                modifier = Modifier.imePadding(),
            )
        }
    }
}

@Composable
private fun ConnectionScreen(
    state: MainUiState,
    onNicknameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPeerSelected: (DiscoveredPeer) -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    layoutSpec: AdaptiveLayoutSpec,
) {
    var helpVisible by remember { mutableStateOf(false) }
    if (helpVisible) {
        AlertDialog(
            onDismissRequest = { helpVisible = false },
            title = { Text("Connection help") },
            text = {
                Text(
                    "Choose a discovered desktop server, enter the nickname and the same session password as on desktop, then connect. If connection fails, check firewall, VPN, guest Wi‑Fi, and that the desktop room is still open.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = { Button(onClick = { helpVisible = false }) { Text("Got it") } },
        )
    }
    if (layoutSpec.supportsTwoPane) {
        AdaptiveContentFrame(layoutSpec) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(layoutSpec.paneSpacing),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(layoutSpec.paneSpacing),
                ) {
                    ConnectionFormCard(
                        state = state,
                        onNicknameChange = onNicknameChange,
                        onPasswordChange = onPasswordChange,
                        onConnect = onConnect,
                        onDisconnect = onDisconnect,
                        onOpenHelp = { helpVisible = true },
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                ) {
                    PeersCard(state = state, onPeerSelected = onPeerSelected)
                }
            }
        }
    } else {
        ScreenLazyColumn(layoutSpec) {
            item {
                ConnectionFormCard(
                    state = state,
                    onNicknameChange = onNicknameChange,
                    onPasswordChange = onPasswordChange,
                    onConnect = onConnect,
                    onDisconnect = onDisconnect,
                    onOpenHelp = { helpVisible = true },
                )
            }
            item { PeersCard(state = state, onPeerSelected = onPeerSelected) }
        }
    }
}

@Composable
private fun ConnectionFormCard(
    state: MainUiState,
    onNicknameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onOpenHelp: () -> Unit,
) {
    SectionCard(
        title = "Connection",
        subtitle = state.connectionPeer?.let { "Target: ${it.nickname} · ${it.host}:${it.chatPort}" }
            ?: "Choose a server peer and connect.",
        trailing = { OutlinedButton(onClick = onOpenHelp) { Text("?") } },
    ) {
        OutlinedTextField(
            value = state.nickname,
            onValueChange = onNicknameChange,
            label = { Text("Nickname") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.connected && !state.connecting,
        )
        OutlinedTextField(
            value = state.sessionPassword,
            onValueChange = onPasswordChange,
            label = { Text("Session password") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Session password field" },
            visualTransformation = PasswordVisualTransformation(),
            enabled = !state.connected && !state.connecting,
        )
        CompactSelectedPeerSummary(state.connectionPeer)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onConnect,
                enabled = !state.connected && !state.connecting && state.connectionPeer != null,
                modifier = Modifier.weight(1f),
            ) {
                Text(if (state.connecting) "Connecting…" else "Connect")
            }
            OutlinedButton(onClick = onDisconnect, enabled = state.connected, modifier = Modifier.weight(1f)) {
                Text("Disconnect")
            }
        }
        if (!state.connected && state.connectionPeer == null) {
            DisabledReason("Connect becomes available after a server peer is discovered and selected.")
        }
    }
}

@Composable
private fun PeersCard(state: MainUiState, onPeerSelected: (DiscoveredPeer) -> Unit) {
    var helpVisible by remember { mutableStateOf(false) }
    if (helpVisible) {
        AlertDialog(
            onDismissRequest = { helpVisible = false },
            title = { Text("No peers yet") },
            text = {
                Text(
                    text = "Discovery can take a few seconds on some networks. Check that the desktop app is open, both devices use the same Wi‑Fi, discovery is enabled, and VPN, firewall, guest Wi‑Fi, or client isolation are not blocking LAN traffic.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = { Button(onClick = { helpVisible = false }) { Text("Got it") } },
        )
    }
    SectionCard(
        title = "Peers",
        subtitle = if (state.peers.isEmpty()) "No discovered peers yet." else "${state.peers.size} peer(s) available.",
        trailing = if (state.peers.isEmpty()) {
            { OutlinedButton(onClick = { helpVisible = true }) { Text("?") } }
        } else {
            null
        },
    ) {
        if (state.peers.isEmpty()) {
            EmptyState(
                title = "No peers available",
                message = "Keep discovery on and check that desktop SecureLan is open on the same Wi‑Fi.",
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.peers.forEach { peer ->
                    CompactPeerChoice(
                        peer = peer,
                        selected = state.selectedPeer?.peerId == peer.peerId,
                        onClick = { onPeerSelected(peer) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatScreen(
    state: MainUiState,
    onInputChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    layoutSpec: AdaptiveLayoutSpec,
) {
    val chatListState = rememberLazyListState()
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            chatListState.animateScrollToItem(state.messages.lastIndex)
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = layoutSpec.horizontalPadding, vertical = layoutSpec.verticalPadding),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = layoutSpec.chatMaxWidth),
            verticalArrangement = Arrangement.spacedBy(layoutSpec.paneSpacing),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                state = chatListState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                if (state.messages.isEmpty()) {
                    item {
                        EmptyState(
                            title = if (state.connected) "No messages yet" else "Connect first",
                            message = if (state.connected) {
                                "Your secure session is ready. Send the first message to start the conversation."
                            } else {
                                "Open Connection, choose a desktop server, and connect before sending messages."
                            },
                        )
                    }
                } else {
                    items(state.messages) { line -> ChatBubble(line, widthFraction = layoutSpec.chatBubbleWidthFraction) }
                }
            }
            ChatInputBar(
                state = state,
                onInputChange = onInputChange,
                onSendMessage = onSendMessage,
                modifier = Modifier.imePadding(),
            )
        }
    }
}

@Composable
private fun ChatInputBar(
    state: MainUiState,
    onInputChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = state.inputMessage,
                    onValueChange = onInputChange,
                    placeholder = { Text(if (state.connected) "Type a secure message" else "Connect first") },
                    modifier = Modifier.weight(1f),
                    enabled = state.connected,
                    minLines = 1,
                    maxLines = 4,
                )
                Button(
                    onClick = onSendMessage,
                    enabled = state.connected && state.inputMessage.isNotBlank(),
                    modifier = Modifier.height(56.dp),
                ) {
                    Text("Send")
                }
            }
            if (!state.connected) {
                DisabledReason("Connect to a desktop peer before sending messages.")
            }
        }
    }
}

@Composable
private fun FilesScreen(
    state: MainUiState,
    onPeerSelected: (DiscoveredPeer) -> Unit,
    onPickFile: () -> Unit,
    onSendFile: () -> Unit,
    onStartFileReceiver: () -> Unit,
    onStopFileReceiver: () -> Unit,
    layoutSpec: AdaptiveLayoutSpec,
) {
    if (layoutSpec.supportsTwoPane) {
        AdaptiveContentFrame(layoutSpec) {
            ScreenIntroCard(
                title = "Files",
                message = "Send one selected document to a peer or listen for an encrypted desktop-to-Android transfer.",
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(layoutSpec.paneSpacing),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(layoutSpec.paneSpacing),
                ) {
                    FileSendTargetCard(state = state, onPeerSelected = onPeerSelected)
                    FileSelectedCard(state = state, onPickFile = onPickFile, onSendFile = onSendFile)
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(layoutSpec.paneSpacing),
                ) {
                    FileReceiveCard(
                        state = state,
                        onStartFileReceiver = onStartFileReceiver,
                        onStopFileReceiver = onStopFileReceiver,
                    )
                    FileProgressCards(state)
                }
            }
        }
    } else {
        ScreenLazyColumn(layoutSpec) {
            item {
                ScreenIntroCard(
                    title = "Files",
                    message = "Send one selected document to a peer or listen for an encrypted desktop-to-Android transfer.",
                )
            }
            item { FileSendTargetCard(state = state, onPeerSelected = onPeerSelected) }
            item { FileSelectedCard(state = state, onPickFile = onPickFile, onSendFile = onSendFile) }
            item {
                FileReceiveCard(
                    state = state,
                    onStartFileReceiver = onStartFileReceiver,
                    onStopFileReceiver = onStopFileReceiver,
                )
            }
            item { FileProgressCards(state) }
        }
    }
}

@Composable
private fun FileSendTargetCard(state: MainUiState, onPeerSelected: (DiscoveredPeer) -> Unit) {
    SectionCard(
        title = "Send target",
        subtitle = state.selectedPeer?.let { "${it.nickname} · ${it.host}:${it.filePort}" } ?: "Select a peer before sending files.",
    ) {
        if (state.peers.isEmpty()) {
            EmptyState(
                title = "No peers available",
                message = "Keep discovery on and check that desktop SecureLan is open on the same Wi‑Fi.",
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.peers.forEach { peer ->
                    CompactPeerChoice(
                        peer = peer,
                        selected = state.selectedPeer?.peerId == peer.peerId,
                        onClick = { onPeerSelected(peer) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FileSelectedCard(state: MainUiState, onPickFile: () -> Unit, onSendFile: () -> Unit) {
    SectionCard(title = "Selected file", subtitle = "Choose a local document to send.") {
        SelectedFileCard(state)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onPickFile, modifier = Modifier.weight(1f)) { Text("Pick file") }
            FilledTonalButton(
                onClick = onSendFile,
                enabled = state.selectedPeer != null && state.selectedFile != null && !state.fileProgress.active,
                modifier = Modifier.weight(1f),
            ) {
                Text("Send file")
            }
        }
        FileSendHint(state)
    }
}

@Composable
private fun FileReceiveCard(state: MainUiState, onStartFileReceiver: () -> Unit, onStopFileReceiver: () -> Unit) {
    SectionCard(title = "Receive from desktop", subtitle = "Start a listener when the desktop sends a file to this phone.") {
        ReceiveListenerStatus(state)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onStartFileReceiver,
                enabled = !state.fileReceiverRunning && state.sessionPassword.isNotBlank(),
                modifier = Modifier.weight(1f),
            ) {
                Text("Receive")
            }
            OutlinedButton(onClick = onStopFileReceiver, enabled = state.fileReceiverRunning, modifier = Modifier.weight(1f)) {
                Text("Stop")
            }
        }
        if (state.sessionPassword.isBlank()) {
            DisabledReason("Enter the session password on Connection before receiving files.")
        }
    }
}

@Composable
private fun FileProgressCards(state: MainUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (state.fileProgress.active || state.fileProgress.bytesSent > 0 || state.fileProgress.error != null) {
            TransferProgressCard(
                title = if (state.fileProgress.active) "Sending ${state.fileProgress.fileName.ifBlank { "file" }}" else "Last sent file",
                progress = state.fileProgress.percent,
                currentBytes = state.fileProgress.bytesSent,
                totalBytes = state.fileProgress.totalBytes,
                error = state.fileProgress.error,
            )
        }
        if (state.incomingFileProgress.active || state.incomingFileProgress.bytesReceived > 0 || state.incomingFileProgress.error != null) {
            TransferProgressCard(
                title = if (state.incomingFileProgress.active) {
                    "Receiving ${state.incomingFileProgress.fileName.ifBlank { "file" }}"
                } else {
                    "Incoming file"
                },
                progress = state.incomingFileProgress.percent,
                currentBytes = state.incomingFileProgress.bytesReceived,
                totalBytes = state.incomingFileProgress.totalBytes,
                error = state.incomingFileProgress.error,
                completedPath = state.incomingFileProgress.completedPath,
            )
        }
    }
}

@Composable
private fun SettingsScreen(state: MainUiState, onDarkThemeChange: (Boolean) -> Unit, onOpenLogs: () -> Unit, layoutSpec: AdaptiveLayoutSpec) {
    if (layoutSpec.supportsTwoPane) {
        AdaptiveContentFrame(layoutSpec) {
            ScreenIntroCard(
                title = "Settings",
                message = "Theme, diagnostics, and quick app state details for troubleshooting LAN sessions.",
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(layoutSpec.paneSpacing),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(layoutSpec.paneSpacing),
                ) {
                    AppearanceCard(state = state, onDarkThemeChange = onDarkThemeChange)
                    DiagnosticsCard(state = state, onOpenLogs = onOpenLogs)
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    CurrentSessionCard(state)
                }
            }
        }
    } else {
        ScreenLazyColumn(layoutSpec) {
            item {
                ScreenIntroCard(
                    title = "Settings",
                    message = "Theme, diagnostics, and quick app state details for troubleshooting LAN sessions.",
                )
            }
            item { AppearanceCard(state = state, onDarkThemeChange = onDarkThemeChange) }
            item { DiagnosticsCard(state = state, onOpenLogs = onOpenLogs) }
            item { CurrentSessionCard(state) }
        }
    }
}

@Composable
private fun AppearanceCard(state: MainUiState, onDarkThemeChange: (Boolean) -> Unit) {
    SectionCard(title = "Appearance", subtitle = "Choose the local app theme.") {
        SettingsRow(
            title = "Dark theme",
            subtitle = if (state.darkThemeEnabled) "Enabled" else "Disabled",
            trailing = { Switch(checked = state.darkThemeEnabled, onCheckedChange = onDarkThemeChange) },
        )
    }
}

@Composable
private fun DiagnosticsCard(state: MainUiState, onOpenLogs: () -> Unit) {
    SectionCard(title = "Diagnostics", subtitle = "Useful when discovery, connection, or file transfer fails.") {
        SettingsRow(
            title = "Logs",
            subtitle = "${state.logs.size} entries available to view or copy.",
            trailing = { OutlinedButton(onClick = onOpenLogs, enabled = state.logs.isNotEmpty()) { Text("Open") } },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
        SettingsRow(
            title = "Network ports",
            subtitle = "Chat ${SecureLanPorts.DEFAULT_CHAT_PORT} · Files ${SecureLanPorts.DEFAULT_FILE_TRANSFER_PORT} · Discovery ${SecureLanPorts.DEFAULT_DISCOVERY_PORT}",
        )
    }
}

@Composable
private fun CurrentSessionCard(state: MainUiState) {
    SectionCard(title = "Current session", subtitle = "Read-only summary.") {
        SettingsRow(title = "Discovery", subtitle = if (state.discoveryRunning) "Listening for peers" else "Stopped")
        SettingsRow(title = "Connection", subtitle = if (state.connected) "Connected as ${state.nickname}" else "Offline")
        SettingsRow(title = "Selected peer", subtitle = state.selectedPeer?.let { "${it.nickname} · ${it.host}:${it.chatPort}" } ?: "None")
    }
}

@Composable
private fun LogsDialog(logs: List<AppLogEntry>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val logText = remember(logs) { logs.joinToString(separator = "\n") { AndroidUiFormatters.formatLogEntry(it) } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.logs_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.logs_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    if (logs.isEmpty()) {
                        Text(
                            text = stringResource(R.string.logs_empty),
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        SelectionContainer {
                            Text(
                                text = logText,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(360.dp)
                                    .verticalScroll(rememberScrollState())
                                    .padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { AndroidClipboard.copyLogs(context, logText) }, enabled = logText.isNotBlank()) {
                Text(stringResource(R.string.copy_all))
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        },
    )
}

@Composable
private fun CompactSelectedPeerSummary(peer: DiscoveredPeer?) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusDot(active = peer != null)
            Text(
                text = peer?.let { "${it.nickname} · ${it.host}:${it.chatPort}" } ?: "No server peer selected",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatBubble(line: ChatLine, widthFraction: Float) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val alignment = if (line.outbound) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (line.outbound) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (line.outbound) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val linkColor = if (line.outbound) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Column(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .clip(RoundedCornerShape(20.dp))
                .background(bubbleColor)
                .combinedClickable(
                    onClick = {},
                    onLongClick = { AndroidClipboard.copyMessage(context, line.text) },
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = if (line.outbound) stringResource(R.string.you) else line.sender,
                    style = MaterialTheme.typography.labelMedium,
                    color = textColor.copy(alpha = 0.76f),
                )
                Text(
                    text = AndroidUiFormatters.formatTimestamp(line.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.68f),
                )
            }
            LinkifiedText(
                text = line.text,
                textColor = textColor,
                linkColor = linkColor,
                style = MaterialTheme.typography.bodyMedium,
                onOpenUri = uriHandler::openUri,
            )
        }
    }
}

@Composable
private fun LinkifiedText(text: String, textColor: Color, linkColor: Color, style: TextStyle, onOpenUri: (String) -> Unit) {
    val annotatedText = remember(text, linkColor) {
        buildAnnotatedString {
            var currentIndex = 0
            UrlRegex.findAll(text).forEach { match ->
                append(text.substring(currentIndex, match.range.first))
                val rawUrl = match.value.trimEnd('.', ',', ';', ':', ')', ']', '}')
                val trailing = match.value.substring(rawUrl.length)
                val url = rawUrl.withUrlScheme()
                pushStringAnnotation(tag = UrlAnnotationTag, annotation = url)
                pushStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))
                append(rawUrl)
                pop()
                pop()
                append(trailing)
                currentIndex = match.range.last + 1
            }
            append(text.substring(currentIndex))
        }
    }
    ClickableText(
        text = annotatedText,
        style = style.merge(color = textColor),
        onClick = { offset ->
            annotatedText.getStringAnnotations(tag = UrlAnnotationTag, start = offset, end = offset)
                .firstOrNull()
                ?.let { onOpenUri(it.item) }
        },
    )
}

@Composable
private fun SelectedFileCard(state: MainUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Selected file", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = state.selectedFile?.name ?: "No file selected",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = state.selectedFile?.let { AndroidUiFormatters.formatBytes(it.size) } ?: "Pick a file from Android document picker.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FileSendHint(state: MainUiState) {
    when {
        state.selectedPeer == null -> DisabledReason("Select a send target above before sending files.")
        state.selectedFile == null -> DisabledReason("Choose a file before sending.")
        state.fileProgress.active -> DisabledReason("Wait until the current transfer finishes.")
        else -> InfoBox("Ready to send", "The selected file will be encrypted and sent to ${state.selectedPeer.nickname}.", positive = true)
    }
}

@Composable
private fun CompactPeerChoice(peer: DiscoveredPeer, selected: Boolean, onClick: () -> Unit) {
    val containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Send target ${peer.nickname}, ${if (selected) "selected" else "not selected"}"
                stateDescription = if (selected) "selected" else "not selected"
            },
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusDot(active = selected)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(peer.nickname, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = contentColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${peer.host}:${peer.filePort} · chat ${peer.chatPort}", style = MaterialTheme.typography.bodySmall, color = contentColor.copy(alpha = 0.75f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(if (selected) "Selected" else "Select", style = MaterialTheme.typography.labelMedium, color = contentColor)
        }
    }
}

@Composable
private fun ReceiveListenerStatus(state: MainUiState) {
    InfoBox(
        title = if (state.fileReceiverRunning) "Listening for incoming files" else "Receiver stopped",
        message = if (state.fileReceiverRunning) {
            "Desktop can send files to this phone using file port ${SecureLanPorts.DEFAULT_FILE_TRANSFER_PORT + 1}."
        } else {
            "Start receiving before sending a desktop-to-Android file transfer."
        },
        positive = state.fileReceiverRunning,
    )
}

@Composable
private fun TransferProgressCard(
    title: String,
    progress: Float,
    currentBytes: Long,
    totalBytes: Long,
    error: String?,
    completedPath: String? = null,
) {
    SectionCard(title = title, subtitle = error?.toFriendlyError()?.title ?: "Transfer progress") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Progress", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = title
                        stateDescription = "${(progress * 100).toInt()} percent"
                    },
            )
            Text(
                text = "${AndroidUiFormatters.formatBytes(currentBytes)} / ${AndroidUiFormatters.formatBytes(totalBytes)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            error?.let { InfoBox("Transfer needs attention", it.toFriendlyError().message, positive = false) }
            completedPath?.let {
                Text(
                    text = "Saved to: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SettingsRow(title: String, subtitle: String, trailing: (@Composable () -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing?.invoke()
    }
}

@Composable
private fun AdaptiveContentFrame(
    layoutSpec: AdaptiveLayoutSpec,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = layoutSpec.horizontalPadding, vertical = layoutSpec.verticalPadding),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = layoutSpec.contentMaxWidth),
            verticalArrangement = Arrangement.spacedBy(layoutSpec.paneSpacing),
            content = content,
        )
    }
}

@Composable
private fun ScreenLazyColumn(
    layoutSpec: AdaptiveLayoutSpec = adaptiveLayoutSpec(0.dp),
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .widthIn(max = layoutSpec.contentMaxWidth),
            contentPadding = PaddingValues(horizontal = layoutSpec.horizontalPadding, vertical = layoutSpec.verticalPadding),
            verticalArrangement = Arrangement.spacedBy(layoutSpec.paneSpacing),
            content = content,
        )
    }
}

@Composable
private fun ScreenIntroCard(title: String, message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 2.dp,
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f))
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                trailing?.invoke()
            }
            content()
        }
    }
}

@Composable
private fun InfoBox(title: String, message: String, positive: Boolean) {
    val color = if (positive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
    val contentColor = if (positive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = color.copy(alpha = 0.72f),
        contentColor = contentColor,
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(message, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun EmptyState(title: String, message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DisabledReason(message: String, modifier: Modifier = Modifier) {
    Text(
        text = message,
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun StatusDot(active: Boolean) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(if (active) SuccessGreen else MaterialTheme.colorScheme.outline)
            .semantics {
                contentDescription = if (active) "Active status" else "Inactive status"
                stateDescription = if (active) "active" else "inactive"
            },
    )
}

private data class FriendlyError(val title: String, val message: String)

private fun String.toFriendlyErrorTitleRes(): Int {
    val lower = lowercase()
    return when {
        "data_too_large" in lower || "too large" in lower -> R.string.error_transfer
        "econnrefused" in lower || "connection refused" in lower || "failed to connect" in lower -> R.string.error_connection
        "password" in lower || "handshake" in lower || "rsa" in lower || "decrypt" in lower -> R.string.error_handshake
        "network" in lower || "timeout" in lower || "unreachable" in lower -> R.string.error_network
        else -> R.string.error_attention
    }
}

private fun String.toFriendlyError(): FriendlyError {
    val lower = lowercase()
    return when {
        "data_too_large" in lower || "too large" in lower -> FriendlyError(
            title = "This transfer cannot be completed",
            message = "The selected payload is too large for the current secure handshake. Try a smaller file or check desktop compatibility.",
        )
        "econnrefused" in lower || "connection refused" in lower || "failed to connect" in lower -> FriendlyError(
            title = "Desktop is not accepting connections",
            message = "Make sure the desktop app is open, the room is running, and firewall or VPN settings are not blocking LAN traffic.",
        )
        "password" in lower || "handshake" in lower || "rsa" in lower || "decrypt" in lower -> FriendlyError(
            title = "Secure handshake failed",
            message = "Check that the session password matches the desktop room and try reconnecting.",
        )
        "network" in lower || "timeout" in lower || "unreachable" in lower -> FriendlyError(
            title = "Network connection problem",
            message = "Keep both devices on the same Wi‑Fi and check VPN, hotspot, guest network, or firewall settings.",
        )
        else -> FriendlyError(
            title = "Action needs attention",
            message = take(140).ifBlank { "Open diagnostics logs for details." },
        )
    }
}

private fun String.toFriendlyStatus(): String = when {
    startsWith("Found ") -> this
    contains("Connected", ignoreCase = true) -> this
    contains("Connecting", ignoreCase = true) -> this
    contains("Receiving", ignoreCase = true) -> this
    contains("File receiver", ignoreCase = true) -> this
    contains("Listening", ignoreCase = true) -> "Listening for peers"
    contains("failed", ignoreCase = true) -> "Needs attention"
    else -> this.ifBlank { "Ready" }
}

private fun String.withUrlScheme(): String = if (startsWith("http://", ignoreCase = true) || startsWith("https://", ignoreCase = true)) {
    this
} else {
    "https://$this"
}

private const val UrlAnnotationTag = "URL"

private val UrlRegex = Regex("""(?i)\b((?:https?://|www\.)[^\s<>()]+|[a-z0-9][a-z0-9-]*(?:\.[a-z0-9][a-z0-9-]*)+[^\s<>()]*)""")

private val SecureLanDarkColors = darkColorScheme(
    primary = Color(0xFF9AD6FF),
    onPrimary = Color(0xFF00344F),
    primaryContainer = Color(0xFF0E3952),
    onPrimaryContainer = Color(0xFFD3EDFF),
    secondary = Color(0xFFC8DAE8),
    onSecondary = Color(0xFF22323D),
    secondaryContainer = Color(0xFF374955),
    onSecondaryContainer = Color(0xFFDDEAF4),
    background = Color(0xFF0E1419),
    onBackground = Color(0xFFE3E8ED),
    surface = Color(0xFF141B21),
    onSurface = Color(0xFFE3E8ED),
    surfaceVariant = Color(0xFF3F4850),
    onSurfaceVariant = Color(0xFFC0C8D0),
    outline = Color(0xFF89939C),
    outlineVariant = Color(0xFF3F4850),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

private val SecureLanLightColors = lightColorScheme(
    primary = Color(0xFF00658F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8E6FF),
    onPrimaryContainer = Color(0xFF001E2E),
    secondary = Color(0xFF50606B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD4E4F1),
    onSecondaryContainer = Color(0xFF0C1D27),
    background = Color(0xFFF7FAFD),
    onBackground = Color(0xFF181C20),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF181C20),
    surfaceVariant = Color(0xFFDDE3EA),
    onSurfaceVariant = Color(0xFF41484D),
    outline = Color(0xFF71787E),
    outlineVariant = Color(0xFFC1C7CE),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

private val SuccessGreen = Color(0xFF35D07F)
