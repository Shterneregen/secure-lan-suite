package com.shterneregen.securelan.androidclient

import android.annotation.SuppressLint
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.LocaleList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.shterneregen.securelan.androidclient.call.AndroidCallSession
import com.shterneregen.securelan.androidclient.call.CallSession
import com.shterneregen.securelan.androidclient.model.AppLanguage
import com.shterneregen.securelan.androidclient.model.DiscoveredPeer
import com.shterneregen.securelan.androidclient.network.*
import com.shterneregen.securelan.common.model.rtc.RtcSignalEnvelope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

interface HostRoom {
    val state: StateFlow<AndroidHostServiceState>
    fun password(): String
    fun start(peerId: String, nickname: String, password: String, chatPort: Int, filePort: Int)
    fun stop()
}

/** A per-ViewModel graph. Platform resources are constructed lazily and have explicit owners. */
class MainDependencies(
    val preferences: SharedPreferences,
    val chat: RoomClient,
    val discover: () -> Flow<DiscoveredPeer>,
    val resolveAddresses: () -> List<String>,
    val host: HostRoom,
    val createCalls: (CoroutineScope, () -> String, () -> Boolean, suspend (RtcSignalEnvelope) -> Unit) -> CallSession,
    val application: () -> Application = { error("Android platform is unavailable") },
    val createSender: () -> SecureFileSender = { error("File sender is unavailable") },
    val createReceiver: () -> IncomingFileReceiver = { error("File receiver is unavailable") },
    val createQuickShare: () -> AndroidQuickShareServer = { error("Quick Share is unavailable") },
    val configureFileNotifications: (AppLanguage) -> Unit = {},
    val hasCamera: () -> Boolean = { false },
    val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    val monotonicMillis: () -> Long = { System.nanoTime() / 1_000_000 },
)

/** The single production composition root; never retained across an unrelated workspace lifetime. */
class MainViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    @SuppressLint("AppBundleLocaleChanges") // Language splitting is disabled in android.bundle.language.
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == MainViewModel::class.java)
        val discovery = PeerDiscoveryRepository()
        val addresses = AndroidLocalNetworkAddressResolver(application)
        val host = object : HostRoom {
            override val state = AndroidChatHostService.state
            override fun password() = AndroidChatHostService.currentSessionPassword()
            override fun start(peerId: String, nickname: String, password: String, chatPort: Int, filePort: Int) {
                AndroidChatHostService.start(application, peerId, nickname, password, chatPort, filePort)
            }
            override fun stop() { AndroidChatHostService.stop(application) }
        }
        return MainViewModel(MainDependencies(
            preferences = application.getSharedPreferences("secure_lan_preferences", Context.MODE_PRIVATE),
            chat = SecureChatClient(), discover = discovery::discoverPeers, resolveAddresses = addresses::resolve,
            host = host, createCalls = { scope, nickname, available, send -> AndroidCallSession(application, scope, nickname, available, send) },
            application = { application },
            createSender = { SecureFileSender(application.contentResolver, application.cacheDir) },
            createReceiver = { SecureFileReceiver(application) },
            createQuickShare = { AndroidQuickShareServer(application.contentResolver, application.cacheDir) },
            hasCamera = { application.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_CAMERA_ANY) },
            configureFileNotifications = { language ->
                val locale = when (language) { AppLanguage.SYSTEM -> null; AppLanguage.ENGLISH -> Locale.ENGLISH; AppLanguage.RUSSIAN -> Locale("ru") }
                val context = if (locale == null) application else application.createConfigurationContext(
                    Configuration(application.resources.configuration).apply { setLocales(LocaleList(locale)) })
                application.getSystemService(NotificationManager::class.java).createNotificationChannel(
                    NotificationChannel("secure_lan_files", context.getString(R.string.notification_channel_files), NotificationManager.IMPORTANCE_DEFAULT))
            },
        )) as T
    }
}
