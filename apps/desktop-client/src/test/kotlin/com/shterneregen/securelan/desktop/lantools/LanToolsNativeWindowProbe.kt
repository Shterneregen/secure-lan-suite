package com.shterneregen.securelan.desktop.lantools

import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.shterneregen.securelan.chat.discovery.DiscoveredPeer
import com.shterneregen.securelan.chat.discovery.PeerDiscoveryService
import com.shterneregen.securelan.chat.service.ChatClientService
import com.shterneregen.securelan.chat.service.ChatServerService
import com.shterneregen.securelan.chat.service.RandomNicknameService
import com.shterneregen.securelan.desktop.compose.ComposeDesktopHostAdapter
import com.shterneregen.securelan.desktop.compose.SecureLanComposeApp
import com.shterneregen.securelan.desktop.compose.state.lantools.DiagnosticKind
import com.shterneregen.securelan.desktop.compose.state.lantools.DiagnosticPhase
import com.shterneregen.securelan.desktop.compose.state.lantools.LanToolsTab
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareService
import com.shterneregen.securelan.filetransfer.service.FileTransferServerService
import com.shterneregen.securelan.lantools.diagnostics.*
import com.shterneregen.securelan.lantools.discovery.LanDeviceDiscovery
import com.shterneregen.securelan.lantools.model.LanNetworkInfoProvider
import com.shterneregen.securelan.lantools.model.LanNetworkSnapshot
import com.shterneregen.securelan.lantools.model.LanResult
import com.shterneregen.securelan.lantools.wol.DatagramSender
import com.shterneregen.securelan.lantools.wol.DefaultWakeOnLanService
import com.shterneregen.securelan.lantools.wol.JvmLanNetworkInfoProvider
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.*
import java.awt.Component
import java.awt.Container
import java.awt.Dialog
import java.awt.event.KeyEvent
import java.lang.reflect.Proxy
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import javax.swing.SwingUtilities

/** Explicit graphical-session probe, isolated from headless checks and the single-scene test harness. */
object LanToolsNativeWindowProbe {
    @JvmStatic
    fun main(args: Array<String>) {
        val info = runBlocking { withTimeout(12_000) { DesktopNetworkInfoProvider(JvmLanNetworkInfoProvider()).snapshot() } }
        check(info is LanResult.Success) { "Local interface snapshot failed" }
        val snapshot = info.value
        println("Local metadata: interfaces=${snapshot.interfaces.size}, gateways=${snapshot.interfaces.sumOf { it.gateways.size }}, DNS=${snapshot.interfaces.sumOf { it.dnsServers.size }}, availability=${snapshot.interfaces.map { it.metadata }.distinct()}")
        utilityClosesWithEscapeAndLeavesHostingAndWorkspaceServicesRunning(snapshot)
        println("LAN Tools native window, Escape and workspace service preservation: passed")
    }

    private fun utilityClosesWithEscapeAndLeavesHostingAndWorkspaceServicesRunning(snapshot: DiagnosticsNetworkSnapshot) {
        val calls = java.util.concurrent.CopyOnWriteArrayList<String>()
        val host = workspaceHost(calls)
        val networks = LanNetworkInfoProvider { LanResult.Success(LanNetworkSnapshot(emptyList())) }
        val browseStarted = CountDownLatch(1)
        val browseCancelled = CountDownLatch(1)
        val controller = DesktopLanToolsController(MemoryLanStore(), networks,
            DefaultWakeOnLanService(networks, DatagramSender { _, _ -> error("Native UI smoke must not broadcast") }),
            diagnostics = DesktopDiagnosticsController(DefaultNetworkDiagnosticsService(HostResolver { error("No DNS in native UI smoke") }, networks,
                TcpConnector { _, _, _ -> error("No TCP in native UI smoke") }), DiagnosticsNetworkInfoProvider { LanResult.Success(snapshot) }),
            explorer = DesktopExplorerController(LanDeviceDiscovery {
                browseStarted.countDown()
                try { awaitCancellation() } finally { browseCancelled.countDown() }
            }))
        val exit = AtomicReference<(() -> Unit)?>(null)
        val failure = AtomicReference<Throwable?>(null)
        val worker = Executors.newSingleThreadExecutor()
        val task = worker.submit {
            try {
                await { exit.get() != null }
                var baseline: List<Any?> = emptyList()
                var serviceCalls = emptyList<String>()
                SwingUtilities.invokeAndWait {
                    baseline = listOf(host.statusState, host.currentRoomPassword, host.transferEntries, host.mediaVoiceState, host.experimentalVideoState, host.discoveredPeers)
                    serviceCalls = calls.toList()
                    controller.open()
                    controller.explorer!!.refresh()
                    controller.selectTab(LanToolsTab.DIAGNOSTICS)
                }
                await { java.awt.Window.getWindows().filterIsInstance<Dialog>().any { it.isShowing && it.title.endsWith("LAN Tools") } }
                val dialog = java.awt.Window.getWindows().filterIsInstance<Dialog>().first { it.isShowing && it.title.endsWith("LAN Tools") }
                // A completed interface snapshot may legitimately have unavailable OS gateway/DNS metadata.
                val expectedPhase = if (snapshot.interfaces.any {
                    it.metadata != com.shterneregen.securelan.lantools.diagnostics.MetadataAvailability.AVAILABLE
                }) DiagnosticPhase.PARTIAL else DiagnosticPhase.SUCCESS
                await { controller.diagnostics!!.state.value.outcome(DiagnosticKind.NETWORK).phase == expectedPhase }
                assertTrue(browseStarted.await(3, TimeUnit.SECONDS))
                // Dispatch only into this test's AWT listeners; do not steal global keyboard focus.
                SwingUtilities.invokeAndWait { escape(dialog) }
                await { !controller.state.value.visible }
                assertTrue(browseCancelled.await(3, TimeUnit.SECONDS))
                SwingUtilities.invokeAndWait {
                    assertEquals(baseline, listOf(host.statusState, host.currentRoomPassword, host.transferEntries, host.mediaVoiceState, host.experimentalVideoState, host.discoveredPeers))
                    assertEquals(serviceCalls, calls.toList(), "Opening a utility must not restart or stop workspace services")
                    assertEquals(DiagnosticPhase.CANCELLED, controller.explorer!!.state.value.phase)
                    assertTrue(host.statusState.localServerRunning)
                    assertTrue(host.statusState.clientConnected)
                }
            } catch (error: Throwable) { failure.set(error) }
            finally { SwingUtilities.invokeAndWait { exit.get()?.invoke() } }
        }
        try {
            application(exitProcessOnExit = false) {
                DisposableEffect(Unit) { exit.set { exitApplication() }; onDispose { } }
                Window(onCloseRequest = ::exitApplication, state = rememberWindowState(width = 1360.dp, height = 860.dp), title = "LAN Tools smoke") {
                    SecureLanComposeApp(hostAdapter = host, lanToolsController = controller)
                }
            }
            task.get(20, TimeUnit.SECONDS)
            failure.get()?.let { throw AssertionError("Native dialog smoke failed", it) }
        } finally { controller.close(); host.shutdown(); worker.shutdownNow() }
    }

    private fun await(condition: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (!condition()) { if (System.nanoTime() > deadline) error("Native window condition timed out"); Thread.sleep(20) }
    }

    private fun escape(component: Component) {
        val event = KeyEvent(component, KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, KeyEvent.VK_ESCAPE, KeyEvent.CHAR_UNDEFINED)
        component.keyListeners.forEach { it.keyReleased(event) }
        if (component is Container) component.components.forEach(::escape)
    }

    /** Real workspace state owner with local service doubles, shared by context-preservation tests. */
    internal fun workspaceHost(calls: MutableList<String> = java.util.concurrent.CopyOnWriteArrayList()): ComposeDesktopHostAdapter {
        val peers = listOf(DiscoveredPeer("peer", "Nearby room", "192.168.1.50", 5050, 5051, Instant.EPOCH),
            DiscoveredPeer("other", "Other room", "192.168.1.51", 5050, 5051, Instant.EPOCH))
        return ComposeDesktopHostAdapter(
            proxy(ChatServerService::class.java, mapOf("isRunning" to true, "connectedUsers" to 0), calls),
            proxy(ChatClientService::class.java, mapOf("isConnected" to true), calls),
            proxy(FileTransferServerService::class.java, mapOf("isRunning" to true), calls),
            proxy(QuickShareService::class.java, mapOf("isRunning" to true, "port" to 8080, "shares" to emptyList<Any>(), "landingUrls" to emptyList<String>()), calls),
            proxy(PeerDiscoveryService::class.java, mapOf("isRunning" to true, "snapshot" to peers), calls),
            proxy(RandomNicknameService::class.java, mapOf("generate" to "Native test"), calls),
            uiStateDispatcher = { it() },
        )
    }

    /** Strictly local service doubles: no ports, media devices or network listeners are opened. */
    private fun <T> proxy(type: Class<T>, values: Map<String, Any?>, calls: MutableList<String>): T = type.cast(
        Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { _, method, _ ->
            if (method.name in listOf("start", "stop", "connect", "disconnect", "setAnnounceEnabled")) calls += "${type.simpleName}.${method.name}"
            when {
                values.containsKey(method.name) -> values[method.name]
                method.returnType == java.lang.Boolean.TYPE -> false
                method.returnType == Integer.TYPE -> 0
                method.name == "toString" -> type.simpleName
                else -> null
            }
        },
    )
}
