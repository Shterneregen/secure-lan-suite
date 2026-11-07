package com.shterneregen.securelan.androidclient.lantools

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.shterneregen.securelan.androidclient.lantools.network.*
import com.shterneregen.securelan.androidclient.network.discovery.*
import com.shterneregen.securelan.lantools.diagnostics.*
import com.shterneregen.securelan.lantools.wol.*

/** Constructor-injected feature services, with no reference to chat, host, transfer or call owners. */
class LanToolsServices(val platform: AndroidLanPlatform, val network: DiagnosticsNetworkInfoProvider,
    val wake: WakeOnLanService, val diagnostics: NetworkDiagnosticsService, val browser: AndroidDeviceBrowser,
    val release: () -> Unit = {})

/** Platform composition root. All resources are owned by this feature ViewModel, not the Activity. */
class LanToolsDependencies(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(LanToolsViewModel::class.java))
        val platform = SystemLanPlatform(context.applicationContext)
        val network = AndroidLanNetworkInfoProvider(platform)
        val resolver = AndroidHostResolver()
        val facade = NsdFacade(context.applicationContext)
        val multicast = MulticastLease(context.applicationContext)
        val browser = AndroidLanDeviceDiscovery(platform,
            { type, key, resolved, lost, failed -> facade.browseType(type, key, resolved, lost, failed) },
            multicast, NsdApiCapabilities.current())
        val services = LanToolsServices(platform, DiagnosticsNetworkInfoProvider(network::diagnostics),
            DefaultWakeOnLanService(network, AndroidDatagramSender(platform)),
            DefaultNetworkDiagnosticsService(resolver, network, AndroidTcpConnector(platform)), browser,
            // The browse coroutine releases its own multicast lease in finally, including after cancellation.
            release = { resolver.close(); facade.close() })
        return LanToolsViewModel(services, PreferencesSavedLanDeviceStore(context.applicationContext)) as T
    }
}
