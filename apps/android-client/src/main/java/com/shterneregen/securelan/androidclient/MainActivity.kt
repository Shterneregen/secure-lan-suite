package com.shterneregen.securelan.androidclient

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import android.util.AndroidRuntimeException
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.androidclient.call.CallButtons
import com.shterneregen.securelan.androidclient.call.CallHost
import com.shterneregen.securelan.androidclient.call.CallPhase
import com.shterneregen.securelan.androidclient.model.*
import com.shterneregen.securelan.androidclient.transfer.BatchStatus
import com.shterneregen.securelan.androidclient.transfer.TransferItem
import com.shterneregen.securelan.androidclient.transfer.TransferPhase
import com.shterneregen.securelan.androidclient.transfer.TransferQueueState
import com.shterneregen.securelan.androidclient.ui.AndroidClipboard
import com.shterneregen.securelan.androidclient.ui.AndroidUiFormatters
import com.shterneregen.securelan.androidclient.ui.LocalAppResources
import com.shterneregen.securelan.androidclient.ui.QuickShareScreen
import kotlinx.coroutines.launch
import java.util.*

private val LocalActivityContext = staticCompositionLocalOf<Context> {
    error("Activity context is not available")
}

@Composable
private fun stringResource(id: Int): String = LocalAppResources.current.getString(id)

@Composable
private fun stringResource(id: Int, vararg formatArgs: Any): String =
    LocalAppResources.current.getString(id, *formatArgs)

@Composable
private fun pluralStringResource(id: Int, count: Int, vararg formatArgs: Any): String =
    LocalAppResources.current.getQuantityString(id, count, *formatArgs)

/** Hosts the adaptive Compose workspace, runtime permission prompts and call Picture-in-Picture. */
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels { MainViewModelFactory(application) }
    var inCallPip by mutableStateOf(false)
        private set

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        inCallPip = isInPictureInPictureMode
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val call = viewModel.callState.value
        if (call.phase == CallPhase.ACTIVE && call.video &&
            packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            runCatching { enterPictureInPictureMode(android.app.PictureInPictureParams.Builder().setAspectRatio(android.util.Rational(4, 3)).build()) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleConnectionIntent(intent)
    }
    private fun handleConnectionIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) intent.dataString?.let(viewModel::importConnectionUri)
    }
    override fun onStart() { super.onStart(); viewModel.updateSearchForeground(true) }
    override fun onStop() { viewModel.updateSearchForeground(false); super.onStop() }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleConnectionIntent(intent)
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
                val monitor = com.shterneregen.securelan.androidclient.network.discovery.AndroidLanNetworkMonitor(this@MainActivity)
                val observing = java.util.concurrent.atomic.AtomicBoolean(true)
                val mainExecutor = androidx.core.content.ContextCompat.getMainExecutor(this@MainActivity)
                fun updateNetworkState() {
                    mainExecutor.execute {
                        if (observing.get()) viewModel.updateNetworkAvailable(monitor.networks().isNotEmpty())
                    }
                }
                monitor.start(::updateNetworkState)
                updateNetworkState()
                onDispose { observing.set(false); monitor.close() }
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
    val queue by viewModel.transferQueue.collectAsState()
    val quickShares by viewModel.quickShares.collectAsState()
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
    key(state.appLanguage, systemLocaleTags) {
        CompositionLocalProvider(
            LocalContext provides localizedContext,
            LocalConfiguration provides localizedContext.resources.configuration,
            LocalActivityResultRegistryOwner provides activityResultRegistryOwner,
            LocalActivityContext provides baseContext,
            LocalAppResources provides localizedContext.resources,
        ) {
            MaterialTheme(colorScheme = colorScheme) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    com.shterneregen.securelan.androidclient.ui.connection.PendingRoomConnectionDialog(viewModel, state)
                    CallHost(viewModel, baseContext as MainActivity) { requestCall ->
                        RedesignedMainScreen(
                            roomTools = { com.shterneregen.securelan.androidclient.ui.connection.RoomConnectionTools(viewModel, state) },
                            onSearchSurface = viewModel::updateSearchSurface,
                            onHostDiscoverableChange = viewModel::setHostDiscoverable,
                            callActions = { CallButtons(state.callPeers, state.connected, requestCall) },
                            state = state,
                            queue = queue,
                            quickShares = quickShares,
                            quickShareUrls = viewModel.quickShareUrls,
                            onQuickShareText = viewModel::createQuickShareText,
                            onQuickShareFile = viewModel::createQuickShareFile,
                            onStopQuickShare = viewModel::stopQuickShare,
                            onQuickShareRefresh = viewModel::refreshQuickShares,
                            onNicknameChange = viewModel::updateNickname,
                            onPasswordChange = viewModel::updateSessionPassword,
                            onThemeModeChange = viewModel::updateThemeMode,
                            onLanguageChange = viewModel::updateAppLanguage,
                            onNotificationsChange = onNotificationsChange,
                            onAutoReceiveChange = viewModel::updateAutoReceiveFiles,
                            onPeerSelected = viewModel::selectPeer,
                            onConnect = viewModel::connectSelectedPeer,
                            onConnectManual = viewModel::connectManualPeer,
                            onStartHosting = viewModel::startHosting,
                            onStopHosting = viewModel::stopHosting,
                            onDisconnect = viewModel::disconnect,
                            onInputChange = viewModel::updateInputMessage,
                            onSendMessage = viewModel::sendTextMessage,
                            onBeginFileSelection = viewModel::beginFileSelection,
                            onFilesPicked = viewModel::addSelectedFiles,
                            onClearFiles = viewModel::clearFiles,
                            onRemoveFile = viewModel::removeFile,
                            onStartFileBatch = viewModel::startFileBatch,
                            onRetryFailedFiles = viewModel::retryFailedFiles,
                            onSkipFailedFiles = viewModel::skipFailedFiles,
                            onStopAfterCurrentFile = viewModel::stopAfterCurrentFile,
                            onRequestNearbyPermission = onRequestNearbyPermission,
                            onRetryDiscovery = viewModel::restartDiscovery,
                            onStopDiscovery = viewModel::stopDiscovery,
                        )
                    }
                }
            }
        }
    }
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
    QUICK_SHARE(R.string.nav_quick_share, Icons.Outlined.Share),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RedesignedMainScreen(
    roomTools: @Composable () -> Unit,
    onSearchSurface: (Boolean) -> Unit,
    onHostDiscoverableChange: (Boolean) -> Unit,
    callActions: @Composable () -> Unit,
    state: MainUiState,
    queue: TransferQueueState,
    quickShares: List<com.shterneregen.securelan.androidclient.network.AndroidQuickShareItem>,
    quickShareUrls: List<String>,
    onQuickShareText: (String, Long, Int?) -> Unit,
    onQuickShareFile: (android.net.Uri, String, Long, Int?) -> Unit,
    onStopQuickShare: (String) -> Unit,
    onQuickShareRefresh: () -> Unit,
    onNicknameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onNotificationsChange: (Boolean) -> Unit,
    onAutoReceiveChange: (Boolean) -> Unit,
    onPeerSelected: (DiscoveredPeer) -> Unit,
    onConnect: () -> Unit,
    onConnectManual: (String, String, String) -> Unit,
    onStartHosting: (String, String) -> Unit,
    onStopHosting: () -> Unit,
    onDisconnect: () -> Unit,
    onInputChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onBeginFileSelection: () -> Boolean,
    onFilesPicked: (List<android.net.Uri>) -> Unit,
    onClearFiles: () -> Unit,
    onRemoveFile: (String) -> Unit,
    onStartFileBatch: () -> Unit,
    onRetryFailedFiles: () -> Unit,
    onSkipFailedFiles: () -> Unit,
    onStopAfterCurrentFile: () -> Unit,
    onRequestNearbyPermission: () -> Unit,
    onRetryDiscovery: () -> Unit,
    onStopDiscovery: () -> Unit,
) {
    var destination by rememberSaveable { mutableStateOf(PrimaryDestination.DEVICES) }
    var settingsVisible by rememberSaveable { mutableStateOf(false) }
    var connectionSheetVisible by remember { mutableStateOf(false) }
    var manualConnectionSheetVisible by remember { mutableStateOf(false) }
    var hostRoomSheetVisible by remember { mutableStateOf(false) }
    var logsVisible by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var wasConnected by rememberSaveable { mutableStateOf(state.connected) }
    val friendlyErrorTitle = state.error?.let { stringResource(it.toFriendlyErrorTitleRes()) }
    val errorDetailsLabel = stringResource(R.string.error_details)
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            onFilesPicked(uris)
            settingsVisible = false
            destination = PrimaryDestination.FILES
        }
    }
    var quickSharePickerPending by remember { mutableStateOf(false) }
    var quickShareFilePolicy by remember { mutableStateOf<Pair<Long, Int?>>(60L to 1) }
    val shareContext = LocalContext.current
    val quickSharePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val name = runCatching {
                shareContext.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
            }.getOrNull()?.takeIf { it.isNotBlank() } ?: uri.lastPathSegment?.substringAfterLast('/') ?: "shared-file"
            onQuickShareFile(uri, name, quickShareFilePolicy.first, quickShareFilePolicy.second)
        }
        quickSharePickerPending = false
    }
    val launchFilePicker: () -> Unit = {
        if (onBeginFileSelection()) {
            filePicker.launch(arrayOf("*/*"))
        }
    }

    LaunchedEffect(destination, settingsVisible) { onSearchSurface(destination == PrimaryDestination.DEVICES && !settingsVisible) }
    BackHandler(enabled = settingsVisible) { settingsVisible = false }

    LaunchedEffect(state.connected) {
        if (state.connected && !wasConnected) {
            connectionSheetVisible = false
            manualConnectionSheetVisible = false
            settingsVisible = false
            destination = PrimaryDestination.CHAT
        }
        wasConnected = state.connected
    }
    LaunchedEffect(state.hosting) {
        if (state.hosting) hostRoomSheetVisible = false
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
    if (hostRoomSheetVisible) {
        HostRoomBottomSheet(
            state = state,
            onNicknameChange = onNicknameChange,
            onPasswordChange = onPasswordChange,
            onStartHosting = onStartHosting,
            onDiscoverableChange = onHostDiscoverableChange,
            onDismiss = { if (!state.hostingStarting) hostRoomSheetVisible = false },
        )
    }
    if (logsVisible) LogsDialog(logs = state.logs, onDismiss = { logsVisible = false })

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val layoutSpec = adaptiveLayoutSpec(this.maxWidth)
        val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
        val showDeviceSidebar = this.maxWidth >= 1200.dp && !settingsVisible && destination != PrimaryDestination.DEVICES
        Scaffold(
            modifier = Modifier.imePadding(),
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
                    onSettingsClick = {
                        keyboardController?.hide()
                        settingsVisible = !settingsVisible
                    },
                )
            },
            bottomBar = {
                if (!layoutSpec.usesNavigationRail && !keyboardVisible) {
                    RedesignedNavigationBar(
                        selected = destination,
                        onSelected = {
                            keyboardController?.hide()
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
                            keyboardController?.hide()
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
                            if (it.role == PeerRole.SERVER) connectionSheetVisible = true
                        },
                        onOpenDevices = { destination = PrimaryDestination.DEVICES },
                        onConnectManual = { manualConnectionSheetVisible = true },
                        onHostRoom = { hostRoomSheetVisible = true },
                        modifier = Modifier.width(304.dp),
                    )
                    Box(Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outlineVariant))
                }
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
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
                            roomTools = roomTools,
                            state = state,
                            onPeerClick = {
                                onPeerSelected(it)
                                if (it.role == PeerRole.SERVER) connectionSheetVisible = true
                            },
                            onOpenChat = { destination = PrimaryDestination.CHAT },
                            onDisconnect = onDisconnect,
                            onStopHosting = onStopHosting,
                            onRequestNearbyPermission = onRequestNearbyPermission,
                            onRetryDiscovery = onRetryDiscovery,
                            onStopDiscovery = onStopDiscovery,
                            onConnectManual = { manualConnectionSheetVisible = true },
                            onHostRoom = { hostRoomSheetVisible = true },
                            layoutSpec = layoutSpec,
                        )
                        PrimaryDestination.CHAT -> RedesignedChatScreen(
                            callActions = callActions,
                            state = state,
                            onInputChange = onInputChange,
                            onSendMessage = onSendMessage,
                            onAttachFile = launchFilePicker,
                            onChooseDevice = { destination = PrimaryDestination.DEVICES },
                            onReconnect = onConnect,
                            layoutSpec = layoutSpec,
                        )
                        PrimaryDestination.FILES -> RedesignedFilesScreen(
                            state = state,
                            queue = queue,
                            onChooseFile = launchFilePicker,
                            onSendBatch = onStartFileBatch,
                            onClearFiles = onClearFiles,
                            onRemoveFile = onRemoveFile,
                            onRetryFailed = onRetryFailedFiles,
                            onSkipFailed = onSkipFailedFiles,
                            onStopAfterCurrent = onStopAfterCurrentFile,
                            onChooseDevice = { destination = PrimaryDestination.DEVICES },
                            layoutSpec = layoutSpec,
                        )
                        PrimaryDestination.QUICK_SHARE -> QuickShareScreen(
                            items = quickShares,
                            urls = quickShareUrls,
                            onShareText = onQuickShareText,
                            onChooseFile = { duration, limit -> quickShareFilePolicy = duration to limit; quickSharePickerPending = true; quickSharePicker.launch(arrayOf("*/*")) },
                            onStop = onStopQuickShare,
                            onRefresh = onQuickShareRefresh,
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
                .heightIn(min = 76.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!settingsVisible) {
                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(Icons.Outlined.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(12.dp).size(24.dp))
                }
            }
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
        state.hostingStarting -> stringResource(R.string.status_hosting_starting) to MaterialTheme.colorScheme.tertiary
        state.hosting -> pluralStringResource(
            R.plurals.status_hosting,
            state.hostedParticipantCount,
            state.hostedParticipantCount,
        ) to SuccessGreen
        state.connected -> stringResource(R.string.status_connected, state.connectionPeer?.nickname.orEmpty()) to SuccessGreen
        state.connecting -> stringResource(R.string.status_connecting, state.connectionPeer?.nickname.orEmpty()) to MaterialTheme.colorScheme.tertiary
        state.discoveryRunning -> stringResource(R.string.status_searching) to MaterialTheme.colorScheme.primary
        else -> stringResource(R.string.status_offline) to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
    roomTools: @Composable () -> Unit,
    state: MainUiState,
    onPeerClick: (DiscoveredPeer) -> Unit,
    onOpenChat: () -> Unit,
    onDisconnect: () -> Unit,
    onStopHosting: () -> Unit,
    onRequestNearbyPermission: () -> Unit,
    onRetryDiscovery: () -> Unit,
    onStopDiscovery: () -> Unit,
    onConnectManual: () -> Unit,
    onHostRoom: () -> Unit,
    layoutSpec: AdaptiveLayoutSpec,
) {
    val serverPeers = state.peers
        .filter { it.role == PeerRole.SERVER }
        .filterNot { state.connected && it.peerId == state.connectionPeer?.peerId }
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
                roomTools()
                if (state.connected || state.hosting) CurrentDeviceCard(state, onOpenChat, if (state.hosting) onStopHosting else onDisconnect)
                DiscoveryStateCard(state, onRequestNearbyPermission, onRetryDiscovery, onStopDiscovery)
                if (!state.connected && !state.hosting) {
                    HostRoomButton(state, onHostRoom)
                    ManualConnectionButton(onConnectManual)
                }
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
                HostedParticipants(state, onPeerClick)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(layoutSpec.horizontalPadding, 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { DevicesSubtitle() }
            item { roomTools() }
            if (state.connected || state.hosting) item { CurrentDeviceCard(state, onOpenChat, if (state.hosting) onStopHosting else onDisconnect) }
            item { DiscoveryStateCard(state, onRequestNearbyPermission, onRetryDiscovery, onStopDiscovery) }
            if (serverPeers.isNotEmpty()) {
                item { DevicesSectionHeader(serverPeers.size) }
                items(serverPeers) { peer ->
                    val connected = state.connected && state.connectionPeer?.peerId == peer.peerId
                    DeviceCard(peer, connected) { selected -> if (connected) onOpenChat() else onPeerClick(selected) }
                }
            }
            if (!state.connected && !state.hosting) {
                item { HostRoomButton(state, onHostRoom) }
                item { ManualConnectionButton(onConnectManual) }
            }
            if (state.hosting && state.peers.any { it.role == PeerRole.CHAT_CLIENT }) {
                item { Text(stringResource(R.string.room_participants), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
                items(state.peers.filter { it.role == PeerRole.CHAT_CLIENT }) { peer ->
                    DeviceCard(peer, state.selectedPeer?.peerId == peer.peerId, onPeerClick)
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
private fun CurrentDeviceCard(state: MainUiState, onOpenChat: () -> Unit, onDisconnect: () -> Unit) {
    val peer = state.connectionPeer ?: return
    RedesignedCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.Computer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(stringResource(if (state.hosting) R.string.hosted_room else R.string.current_device), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(peer.nickname, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (state.hosting) {
                    Text(
                        stringResource(R.string.room_address),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (state.localRoomAddresses.isEmpty()) {
                        Text(
                            stringResource(R.string.local_room_address_unavailable),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    } else {
                        state.localRoomAddresses.forEach { address ->
                            Text(
                                AndroidUiFormatters.roomEndpoint(address, state.hostChatPort),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    Text(peer.host, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = SuccessGreen)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onOpenChat, enabled = state.connected, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.open_chat)) }
            OutlinedButton(onClick = onDisconnect, modifier = Modifier.weight(1f)) {
                Text(stringResource(if (state.hosting) R.string.stop_hosting else R.string.disconnect))
            }
        }
    }
}

@Composable
private fun DiscoveryStateCard(
    state: MainUiState,
    onRequestPermission: () -> Unit,
    onRetry: () -> Unit,
    onStop: () -> Unit,
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
                        actionLabel = stringResource(R.string.stop_search),
                        onAction = onStop,
                    )
                } else {
                    StateCard(
                        icon = Icons.Outlined.ErrorOutline,
                        title = stringResource(R.string.no_devices_title),
                        message = stringResource(R.string.no_devices_message),
                        actionLabel = stringResource(R.string.search_again),
                        onAction = onRetry,
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
private fun HostedParticipants(state: MainUiState, onPeerClick: (DiscoveredPeer) -> Unit) {
    if (!state.hosting) return
    val participants = state.peers.filter { it.role == PeerRole.CHAT_CLIENT }
    Text(stringResource(R.string.room_participants), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    if (participants.isEmpty()) {
        Text(stringResource(R.string.no_room_participants), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        participants.forEach { peer ->
            DeviceCard(peer, state.selectedPeer?.peerId == peer.peerId, onPeerClick)
        }
    }
}

@Composable
private fun DeviceCard(peer: DiscoveredPeer, connected: Boolean, onClick: (DiscoveredPeer) -> Unit) {
    Card(
        onClick = { onClick(peer) },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (connected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, if (connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(Icons.Outlined.Computer, contentDescription = null, modifier = Modifier.padding(10.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(peer.nickname, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("SecureLan · ${peer.host}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(
                if (connected) Icons.Outlined.CheckCircle else Icons.Outlined.ChevronRight,
                contentDescription = stringResource(if (connected) R.string.status_selected else R.string.connect),
                tint = MaterialTheme.colorScheme.primary,
            )
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

@Composable
private fun HostRoomButton(state: MainUiState, onClick: () -> Unit) {
    if (state.hosting || state.hostingStarting) return
    Button(
        onClick = onClick,
        enabled = !state.connected && !state.connecting,
        modifier = Modifier.fillMaxWidth().height(50.dp),
    ) {
        Icon(Icons.Outlined.Wifi, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.host_room))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HostRoomBottomSheet(
    onDiscoverableChange: (Boolean) -> Unit,
    state: MainUiState,
    onNicknameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onStartHosting: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var chatPort by remember { mutableStateOf(state.hostChatPort) }
    var filePort by remember { mutableStateOf(state.hostFilePort) }
    var passwordVisible by remember { mutableStateOf(false) }
    var portsExpanded by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val chatPortValid = chatPort.toIntOrNull()?.let { it in 1..65535 } == true
    val filePortValid = filePort.toIntOrNull()?.let { it in 1..65535 } == true
    val portsDifferent = chatPort != filePort
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(R.string.host_room_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(state.hostDiscoverable, onDiscoverableChange)
                Text(stringResource(R.string.room_discoverable))
            }
            Text(stringResource(R.string.host_room_message), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = state.nickname,
                onValueChange = onNicknameChange,
                label = { Text(stringResource(R.string.nickname)) },
                enabled = !state.hostingStarting,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.sessionPassword,
                onValueChange = onPasswordChange,
                label = { Text(stringResource(R.string.session_password)) },
                enabled = !state.hostingStarting,
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
            TextButton(onClick = { portsExpanded = !portsExpanded }) {
                Text(stringResource(if (portsExpanded) R.string.hide_network_settings else R.string.show_network_settings))
            }
            if (portsExpanded) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = chatPort,
                        onValueChange = { chatPort = it.filter(Char::isDigit).take(5) },
                        label = { Text(stringResource(R.string.chat_port)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = chatPort.isNotEmpty() && !chatPortValid,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = filePort,
                        onValueChange = { filePort = it.filter(Char::isDigit).take(5) },
                        label = { Text(stringResource(R.string.file_port)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = filePort.isNotEmpty() && (!filePortValid || !portsDifferent),
                        modifier = Modifier.weight(1f),
                    )
                }
                if (!portsDifferent) Text(stringResource(R.string.host_ports_must_differ), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = { onStartHosting(chatPort, filePort) },
                enabled = state.nickname.isNotBlank() && state.sessionPassword.isNotBlank() && chatPortValid && filePortValid && portsDifferent && !state.hostingStarting,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text(stringResource(if (state.hostingStarting) R.string.hosting_starting else R.string.start_hosting)) }
        }
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val peer = state.connectionPeer
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
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
    var portsExpanded by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val normalizedHost = host.trim().removeSurrounding("[", "]")
    val hostValid = normalizedHost.isNotBlank() &&
        normalizedHost.none { it.isWhitespace() } &&
        "://" !in normalizedHost &&
        '/' !in normalizedHost
    val chatPortValid = chatPort.toIntOrNull()?.let { it in 1..65535 } == true
    val filePortValid = filePort.toIntOrNull()?.let { it in 1..65535 } == true

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
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
            TextButton(onClick = { portsExpanded = !portsExpanded }) {
                Text(stringResource(if (portsExpanded) R.string.hide_network_settings else R.string.show_network_settings))
            }
            if (portsExpanded) {
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
            }
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
    callActions: @Composable () -> Unit,
    state: MainUiState,
    onInputChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onAttachFile: () -> Unit,
    onChooseDevice: () -> Unit,
    onReconnect: () -> Unit,
    layoutSpec: AdaptiveLayoutSpec,
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var previousMessageCount by remember { mutableStateOf(0) }
    LaunchedEffect(state.messages.size) {
        val nearBottom = (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >= previousMessageCount - 2
        if (state.messages.isNotEmpty() && (previousMessageCount == 0 || nearBottom || state.messages.last().outbound)) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
        previousMessageCount = state.messages.size
    }
    Column(
        modifier = Modifier.widthIn(max = layoutSpec.chatMaxWidth).fillMaxSize().padding(horizontal = layoutSpec.horizontalPadding),
    ) {
        if (state.connected) callActions()
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
        Box(Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
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
            if (listState.canScrollForward && state.messages.isNotEmpty()) {
                FilledTonalButton(
                    onClick = { coroutineScope.launch { listState.animateScrollToItem(state.messages.lastIndex) } },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
                ) {
                    Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.latest_messages))
                }
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
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
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
                shape = RoundedCornerShape(18.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (state.inputMessage.isNotBlank()) onSendMessage() }),
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
    queue: TransferQueueState,
    onChooseFile: () -> Unit,
    onSendBatch: () -> Unit,
    onClearFiles: () -> Unit,
    onRemoveFile: (String) -> Unit,
    onRetryFailed: () -> Unit,
    onSkipFailed: () -> Unit,
    onStopAfterCurrent: () -> Unit,
    onChooseDevice: () -> Unit,
    layoutSpec: AdaptiveLayoutSpec,
) {
    LazyColumn(
        modifier = Modifier.widthIn(max = layoutSpec.contentMaxWidth).fillMaxSize(),
        contentPadding = PaddingValues(layoutSpec.horizontalPadding, 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!state.connected && !state.hosting) {
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
            item {
                Text(
                    state.fileRecipient?.let { stringResource(R.string.recipient, it.nickname) }
                        ?: stringResource(R.string.files_choose_participant),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                FilesBatchCard(
                    queue = queue,
                    recipientName = state.fileRecipient?.nickname,
                    onAddFiles = onChooseFile,
                    onSendBatch = onSendBatch,
                    onClearFiles = onClearFiles,
                    onRemoveFile = onRemoveFile,
                    onRetryFailed = onRetryFailed,
                    onSkipFailed = onSkipFailed,
                    onStopAfterCurrent = onStopAfterCurrent,
                )
            }
            item { ReceiverReadyCard(state) }
            if (state.incomingFileProgress.active) item { IncomingTransferCard(state) }
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
private fun FilesBatchCard(
    queue: TransferQueueState,
    recipientName: String?,
    onAddFiles: () -> Unit,
    onSendBatch: () -> Unit,
    onClearFiles: () -> Unit,
    onRemoveFile: (String) -> Unit,
    onRetryFailed: () -> Unit,
    onSkipFailed: () -> Unit,
    onStopAfterCurrent: () -> Unit,
) {
    val items = queue.items
    val running = queue.status == BatchStatus.RUNNING || queue.status == BatchStatus.STOP_REQUESTED
    val paused = queue.status == BatchStatus.PAUSED
    val finished = queue.status == BatchStatus.FINISHED
    var expanded by remember { mutableStateOf(false) }
    RedesignedCard {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    if (items.isEmpty()) stringResource(R.string.choose_file_title) else stringResource(R.string.files_selected_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    if (items.isEmpty()) stringResource(R.string.choose_file_message) else stringResource(R.string.file_will_be_sent, recipientName.orEmpty()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (items.isNotEmpty()) {
            if (running || paused || finished) {
                val current = items.firstOrNull { it.phase == TransferPhase.PREPARING || it.phase == TransferPhase.WAITING_FOR_ACCEPTANCE || it.phase == TransferPhase.SENDING || it.phase == TransferPhase.WAITING_FOR_CONFIRMATION }
                    ?: items.lastOrNull { it.phase != TransferPhase.QUEUED }
                val attempt = current?.attempts?.lastOrNull()
                val total = attempt?.total
                val bytes = attempt?.bytes ?: 0L
                if (current != null) {
                    Text(
                        stringResource(R.string.transfer_current_file, current.source.name),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    stringResource(R.string.transfer_completed_count, queue.confirmed, items.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (total != null && total > 0L) {
                    LinearProgressIndicator(
                        progress = { (bytes.toFloat() / total.toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                TextButton(onClick = { expanded = !expanded }) {
                    Text(stringResource(R.string.transfer_details))
                }
                if (expanded) {
                    items.forEach { item ->
                        BatchItemRow(
                            item = item,
                            onRemove = if (item.phase == TransferPhase.QUEUED) {
                                { onRemoveFile(item.id) }
                            } else {
                                null
                            },
                        )
                    }
                }
            } else {
                items.forEach { item -> DraftItemRow(item, onRemoveFile) }
            }
        }
        when {
            finished -> {
                if (items.isNotEmpty()) {
                    Text(
                        stringResource(R.string.batch_result, queue.confirmed, items.size, queue.failed, queue.skipped),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onAddFiles, enabled = recipientName != null, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.add_files))
                    }
                    OutlinedButton(onClick = onClearFiles, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.clear_files))
                    }
                }
            }
            paused -> {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = onRetryFailed, enabled = recipientName != null, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.retry_failed))
                    }
                    OutlinedButton(onClick = onSkipFailed, enabled = recipientName != null, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.skip_and_continue))
                    }
                }
                OutlinedButton(onClick = onStopAfterCurrent, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.stop_after_current))
                }
            }
            running -> {
                OutlinedButton(onClick = onStopAfterCurrent, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.stop_after_current))
                }
            }
            else -> {
                if (items.isEmpty()) {
                    Button(onClick = onAddFiles, enabled = recipientName != null, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.add_files))
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onAddFiles, enabled = recipientName != null, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.add_files))
                        }
                        OutlinedButton(onClick = onClearFiles, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.clear_files))
                        }
                    }
                    Button(
                        onClick = onSendBatch,
                        enabled = recipientName != null,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(pluralStringResource(R.plurals.send_files_count, items.size, items.size))
                    }
                }
            }
        }
    }
}

@Composable
private fun DraftItemRow(item: TransferItem, onRemove: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(Icons.Outlined.Description, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(item.source.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                item.source.size?.let { AndroidUiFormatters.formatBytes(it) } ?: stringResource(R.string.file_size_pending),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (item.source.unavailable) {
                Text(stringResource(R.string.file_unavailable), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
        IconButton(onClick = { onRemove(item.id) }) {
            Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.remove_file))
        }
    }
}

@Composable
private fun BatchItemRow(item: TransferItem, onRemove: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(Icons.Outlined.Description, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(item.source.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                item.source.size?.let { AndroidUiFormatters.formatBytes(it) } ?: stringResource(R.string.file_size_pending),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            item.attempts.lastOrNull()?.failureKind?.let { kind ->
                Text(transferFailureLabel(kind), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error)
            }
        }
        Text(
            transferPhaseLabel(item.phase),
            style = MaterialTheme.typography.labelMedium,
            color = transferPhaseColor(item.phase),
        )
        if (onRemove != null) {
            IconButton(onClick = onRemove) {
                Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.remove_file))
            }
        }
    }
}

@Composable
private fun transferFailureLabel(kind: com.shterneregen.securelan.filetransfer.service.FileTransferFailureKind): String = stringResource(
    when (kind) {
        com.shterneregen.securelan.filetransfer.service.FileTransferFailureKind.REJECTED -> R.string.transfer_failure_rejected
        com.shterneregen.securelan.filetransfer.service.FileTransferFailureKind.SOURCE -> R.string.transfer_failure_source
        com.shterneregen.securelan.filetransfer.service.FileTransferFailureKind.PROTOCOL -> R.string.transfer_failure_protocol
        com.shterneregen.securelan.filetransfer.service.FileTransferFailureKind.CONNECTION -> R.string.transfer_failure_connection
        com.shterneregen.securelan.filetransfer.service.FileTransferFailureKind.TIMEOUT -> R.string.transfer_failure_timeout
        com.shterneregen.securelan.filetransfer.service.FileTransferFailureKind.ABORTED -> R.string.transfer_failure_aborted
        com.shterneregen.securelan.filetransfer.service.FileTransferFailureKind.OUTCOME_UNKNOWN -> R.string.transfer_failure_unknown
    },
)

@Composable
private fun transferPhaseLabel(phase: TransferPhase): String = stringResource(
    when (phase) {
        TransferPhase.QUEUED -> R.string.phase_queued
        TransferPhase.PREPARING -> R.string.phase_preparing
        TransferPhase.WAITING_FOR_ACCEPTANCE -> R.string.phase_waiting_acceptance
        TransferPhase.SENDING -> R.string.phase_sending
        TransferPhase.WAITING_FOR_CONFIRMATION -> R.string.phase_waiting_confirmation
        TransferPhase.COMPLETED -> R.string.phase_completed
        TransferPhase.FAILED -> R.string.phase_failed
        TransferPhase.OUTCOME_UNKNOWN -> R.string.phase_outcome_unknown
        TransferPhase.SKIPPED -> R.string.phase_skipped
    },
)

@Composable
private fun transferPhaseColor(phase: TransferPhase): Color = when (phase) {
    TransferPhase.COMPLETED -> SuccessGreen
    TransferPhase.FAILED, TransferPhase.OUTCOME_UNKNOWN -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun IncomingTransferCard(state: MainUiState) {
    RedesignedCard {
        Text(state.incomingFileProgress.fileName.ifBlank { stringResource(R.string.transfer_received) }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        LinearProgressIndicator(progress = { state.incomingFileProgress.percent }, modifier = Modifier.fillMaxWidth())
        Text("${(state.incomingFileProgress.percent * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
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
private fun TransferHistoryRow(record: TransferRecord) {
    val failed = record.result == TransferResult.FAILED
    val activityContext = LocalActivityContext.current
    val openFileTitle = stringResource(R.string.open_file)
    val unavailableMessage = stringResource(R.string.open_file_unavailable)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
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
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(AndroidUiFormatters.formatBytes(record.bytes), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (record.direction == TransferDirection.RECEIVED &&
                    record.result == TransferResult.COMPLETED &&
                    record.contentUri != null
                ) {
                    TextButton(
                        onClick = {
                            openReceivedFile(activityContext, record, openFileTitle, unavailableMessage)
                        },
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.open_file))
                    }
                }
            }
        }
    }
}

private fun openReceivedFile(
    context: Context,
    record: TransferRecord,
    chooserTitle: String,
    unavailableMessage: String,
) {
    val uri = record.contentUri ?: return
    val providerMimeType = context.contentResolver.getType(uri)
    val extensionMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
        record.fileName.substringAfterLast('.', "").lowercase(Locale.ROOT),
    )
    val mimeType = providerMimeType
        ?.takeUnless { it == "application/octet-stream" }
        ?: extensionMimeType
        ?: "application/octet-stream"
    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeType)
        clipData = ClipData.newRawUri(record.fileName, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooserIntent = Intent.createChooser(viewIntent, chooserTitle).apply {
        if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(chooserIntent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, unavailableMessage, Toast.LENGTH_SHORT).show()
    } catch (_: SecurityException) {
        Toast.makeText(context, unavailableMessage, Toast.LENGTH_SHORT).show()
    } catch (_: AndroidRuntimeException) {
        Toast.makeText(context, unavailableMessage, Toast.LENGTH_SHORT).show()
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
        modifier = Modifier.widthIn(max = 760.dp).fillMaxSize(),
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
                Column(Modifier.fillMaxWidth().selectableGroup()) {
                    AppLanguage.entries.forEach { language ->
                        SettingsChoiceRow(
                            selected = state.appLanguage == language,
                            onClick = { onLanguageChange(language) },
                            label = stringResource(
                                when (language) {
                                    AppLanguage.SYSTEM -> R.string.language_system
                                    AppLanguage.ENGLISH -> R.string.language_english
                                    AppLanguage.RUSSIAN -> R.string.language_russian
                                },
                            ),
                        )
                    }
                }
            }
        }
        item {
            SettingsGroup(stringResource(R.string.settings_theme)) {
                Column(Modifier.fillMaxWidth().selectableGroup()) {
                    ThemeMode.entries.forEach { mode ->
                        SettingsChoiceRow(
                            selected = state.themeMode == mode,
                            onClick = { onThemeModeChange(mode) },
                            label = stringResource(
                                when (mode) {
                                    ThemeMode.SYSTEM -> R.string.theme_system
                                    ThemeMode.LIGHT -> R.string.theme_light
                                    ThemeMode.DARK -> R.string.theme_dark
                                },
                            ),
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
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun SettingsChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .heightIn(min = 52.dp).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        RadioButton(selected = selected, onClick = null)
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
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.primaryContainer) {
            Icon(icon, contentDescription = null, modifier = Modifier.padding(20.dp).size(36.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
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
    onHostRoom: () -> Unit,
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
        HostRoomButton(state, onHostRoom)
        state.peers.filter { it.role == PeerRole.SERVER }.forEach { peer ->
            val connected = state.connected && state.connectionPeer?.peerId == peer.peerId
            DeviceCard(peer, connected) { selected -> if (connected) onOpenDevices() else onPeerClick(selected) }
        }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatBubble(line: ChatLine, widthFraction: Float) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val alignment = if (line.outbound) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (line.outbound) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
    val textColor = if (line.outbound) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val linkColor = MaterialTheme.colorScheme.primary
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Column(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = if (line.outbound) 20.dp else 6.dp, bottomEnd = if (line.outbound) 6.dp else 20.dp))
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
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
                withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) {
                    append(rawUrl)
                }
                append(trailing)
                currentIndex = match.range.last + 1
            }
            append(text.substring(currentIndex))
        }
    }
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    Text(
        text = annotatedText,
        style = style.merge(color = textColor),
        onTextLayout = { layoutResult = it },
        modifier = Modifier.pointerInput(annotatedText) {
            detectTapGestures { position ->
                val result = layoutResult ?: return@detectTapGestures
                val offset = result.getOffsetForPosition(position)
                annotatedText.getStringAnnotations(tag = UrlAnnotationTag, start = offset, end = offset)
                    .firstOrNull()
                    ?.let { onOpenUri(it.item) }
            }
        },
    )
}

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

private fun String.withUrlScheme(): String = if (startsWith("http://", ignoreCase = true) || startsWith("https://", ignoreCase = true)) {
    this
} else {
    "https://$this"
}

private const val UrlAnnotationTag = "URL"

private val UrlRegex = Regex("""(?i)\b((?:https?://|www\.)[^\s<>()]+|[a-z0-9][a-z0-9-]*(?:\.[a-z0-9][a-z0-9-]*)+[^\s<>()]*)""")

private val SecureLanDarkColors = darkColorScheme(
    primary = Color(0xFF8AB4FF),
    onPrimary = Color(0xFF082F6B),
    primaryContainer = Color(0xFF1B304D),
    onPrimaryContainer = Color(0xFFDCE8FF),
    secondary = Color(0xFFC8DAE8),
    onSecondary = Color(0xFF22323D),
    secondaryContainer = Color(0xFF374955),
    onSecondaryContainer = Color(0xFFDDEAF4),
    background = Color(0xFF0B111A),
    onBackground = Color(0xFFE3E8ED),
    surface = Color(0xFF0F1825),
    surfaceContainerLowest = Color(0xFF0B111A),
    surfaceContainerLow = Color(0xFF111C2B),
    surfaceContainer = Color(0xFF152131),
    surfaceContainerHigh = Color(0xFF1B2A3E),
    surfaceContainerHighest = Color(0xFF24344A),
    onSurface = Color(0xFFE3E8ED),
    surfaceVariant = Color(0xFF24344A),
    onSurfaceVariant = Color(0xFFC0C8D0),
    outline = Color(0xFF89939C),
    outlineVariant = Color(0xFF2C3E56),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

private val SecureLanLightColors = lightColorScheme(
    primary = Color(0xFF2864C7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE8FF),
    onPrimaryContainer = Color(0xFF123263),
    secondary = Color(0xFF50606B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD4E4F1),
    onSecondaryContainer = Color(0xFF0C1D27),
    background = Color(0xFFF7FAFD),
    onBackground = Color(0xFF181C20),
    surface = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1F5FC),
    surfaceContainer = Color(0xFFEDF2FA),
    surfaceContainerHigh = Color(0xFFE5ECF7),
    surfaceContainerHighest = Color(0xFFDCE5F2),
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
