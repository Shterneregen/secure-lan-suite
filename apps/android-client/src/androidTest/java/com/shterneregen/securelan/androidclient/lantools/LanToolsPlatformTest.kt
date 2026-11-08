package com.shterneregen.securelan.androidclient.lantools

import android.Manifest
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.shterneregen.securelan.androidclient.lantools.network.*
import com.shterneregen.securelan.androidclient.network.discovery.*
import com.shterneregen.securelan.lantools.diagnostics.*
import com.shterneregen.securelan.lantools.model.*
import com.shterneregen.securelan.lantools.wol.WakeRoute
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.net.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** Real per-socket Android binding against in-process endpoints; no public hosts or equipment. */
@RunWith(AndroidJUnit4::class)
class LanToolsPlatformTest {
    @Test fun selectedAndroidNetworkSupportsBoundTcpAndUdpAndReleasesSubscription() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val platform = SystemLanPlatform(context)
        platform.start {}
        val resolver = AndroidHostResolver()
        try {
            val link = awaitLink(platform)
            val provider = AndroidLanNetworkInfoProvider(platform)
            val metadata = provider.diagnostics() as LanResult.Success
            assertTrue(metadata.value.interfaces.any { it.source.binding == link.binding })
            val local = InetAddress.getByAddress(link.binding.address.bytes())
            withContext(Dispatchers.IO) {
                ServerSocket(0, 1, local).use { server ->
                    server.soTimeout = 3000
                    val service = DefaultNetworkDiagnosticsService(resolver, provider, AndroidTcpConnector(platform))
                    val result = service.tcp(TcpCheckRequest(link.binding.address.value, server.localPort, link.binding.key, 3000))
                    assertTrue("Bound TCP must connect locally: $result", result is LanResult.Success)
                    server.accept().use { it.soTimeout = 1000; assertEquals(-1, it.getInputStream().read()) }
                }
                DatagramSocket(0, local).use { receiver ->
                    receiver.soTimeout = 3000
                    val route = WakeRoute(LanInterfaceSnapshot(link.binding, link.name, true, link.broadcast), link.binding.address, receiver.localPort)
                    val bytes = byteArrayOf(1, 2, 3)
                    AndroidDatagramSender(platform).send(route, bytes)
                    val packet = DatagramPacket(ByteArray(32), 32); receiver.receive(packet)
                    assertArrayEquals(bytes, packet.data.copyOf(packet.length))
                }
            }
        } finally { resolver.close(); platform.close() }
    }

    @Test fun genericNsdFacadeFindsLocalAdvertisementWithoutRoomTxt() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        if (Build.VERSION.SDK_INT >= 33) instrumentation.uiAutomation.executeShellCommand(
            "pm grant ${context.packageName} ${Manifest.permission.NEARBY_WIFI_DEVICES}").use { android.os.ParcelFileDescriptor.AutoCloseInputStream(it).readBytes() }
        val platform = SystemLanPlatform(context); platform.start {}
        val manager = context.getSystemService(android.net.nsd.NsdManager::class.java)
        val registered = CompletableDeferred<Unit>()
        val registration = object : android.net.nsd.NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: android.net.nsd.NsdServiceInfo) { registered.complete(Unit) }
            override fun onRegistrationFailed(info: android.net.nsd.NsdServiceInfo, error: Int) { registered.completeExceptionally(IllegalStateException("NSD registration failed: $error")) }
            override fun onServiceUnregistered(info: android.net.nsd.NsdServiceInfo) = Unit
            override fun onUnregistrationFailed(info: android.net.nsd.NsdServiceInfo, error: Int) = Unit
        }
        val facade = NsdFacade(context)
        val found = CompletableDeferred<NsdFacade.Resolved>()
        var browser: NsdFacade.Browser? = null
        var registrationStarted = false
        try {
            val link = awaitLink(platform)
            val name = "SecureLanToolsTest-${System.nanoTime()}"
            val info = android.net.nsd.NsdServiceInfo().apply { serviceName = name; serviceType = "_http._tcp."; port = 18080; setAttribute("untrusted", "discard-me") }
            manager.registerService(info, android.net.nsd.NsdManager.PROTOCOL_DNS_SD, registration); registrationStarted = true
            withTimeout(10.seconds) { registered.await() }
            browser = facade.browseType("_http._tcp.", link.binding.interfaceKey.takeIf { NsdApiCapabilities.current().networkScoped },
                resolved = { if (it.name == name) found.complete(it) }, lost = {}, failed = {})
            val value = withTimeout(20.seconds) { found.await() }
            assertTrue(value.addresses.isNotEmpty()); assertEquals(18080, value.port); assertTrue(value.txt.isEmpty())
            val active = browser
            active.close()
            // A successful discovery stop alone is insufficient while service-info callbacks retire.
            withTimeout(5.seconds) { active.awaitClosed() }
        } finally {
            browser?.close()
            withContext(NonCancellable) { withTimeoutOrNull(2.seconds) { browser?.awaitClosed() } }
            if (registrationStarted) runCatching { manager.unregisterService(registration) }
            facade.close(); platform.close()
        }
    }

    private suspend fun awaitLink(platform: AndroidLanPlatform): AndroidLanLink = withTimeout(10.seconds) {
        // Network callbacks can replace the snapshot between reads. Select from the same
        // snapshot that established availability instead of querying again before first().
        var link = platform.links().firstOrNull()
        while (link == null) {
            delay(50.milliseconds)
            link = platform.links().firstOrNull()
        }
        link
    }
}
