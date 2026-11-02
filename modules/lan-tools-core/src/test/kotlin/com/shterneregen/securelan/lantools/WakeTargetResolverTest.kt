package com.shterneregen.securelan.lantools

import com.shterneregen.securelan.common.net.interfaces.NetworkKind
import com.shterneregen.securelan.lantools.model.*
import com.shterneregen.securelan.lantools.wol.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class WakeTargetResolverTest {
    private fun resolve(req: WakeRequest = request(), vararg entries: LanInterfaceSnapshot) =
        WakeTargetResolver.resolve(req, LanNetworkSnapshot(entries.toList()))

    @Test
    fun selectsSingleLanWithoutRequiringMulticast() {
        val selected = network()
        val route = (resolve(request(), selected) as LanResult.Success).value
        assertEquals(selected, route.network)
        assertEquals(ip("192.168.10.255"), route.target)
        assertEquals(9, route.port)
    }

    @Test
    fun requiresExplicitSelectionAcrossInterfacesOrSourceAliases() {
        val first = network()
        val second = network("wifi0")
        assertEquals(LanResult.Failure(LanError.INTERFACE_SELECTION_REQUIRED), resolve(request(), first, second))
        assertEquals(first, (resolve(request().copy(bindingKey = first.binding.key), first, second) as LanResult.Success).value.network)
        val alias = network(address = "192.168.10.43")
        val preferred = request().copy(configuration = WakeConfiguration(mac, preferredInterfaceKey = "eth0"))
        assertEquals(LanResult.Failure(LanError.INTERFACE_SELECTION_REQUIRED), resolve(preferred, first, alias))
    }

    @Test
    fun neverFallsBackFromMissingOrUnsuitablePreference() {
        val preferred = request().copy(configuration = WakeConfiguration(mac, preferredInterfaceKey = "missing"))
        assertEquals(LanResult.Failure(LanError.INTERFACE_UNAVAILABLE), resolve(preferred, network()))
        val down = network("down").let { it.copy(binding = it.binding.copy(up = false)) }
        assertEquals(LanResult.Failure(LanError.INTERFACE_UNSUITABLE), resolve(request().copy(bindingKey = down.binding.key), down, network()))
        // An explicit source can override a saved hint, but only through a deliberate caller selection.
        assertTrue(resolve(preferred.copy(bindingKey = network().binding.key), network()) is LanResult.Success)
    }

    @Test
    fun rejectsIneligibleInterfacesWithoutOpeningAnyTransport() {
        val base = network()
        val cases = listOf(
            base.copy(binding = base.binding.copy(up = false)),
            base.copy(binding = base.binding.copy(kind = NetworkKind.VPN)),
            base.copy(binding = base.binding.copy(kind = NetworkKind.VIRTUAL)),
            base.copy(binding = base.binding.copy(kind = NetworkKind.CELLULAR)),
            base.copy(binding = base.binding.copy(address = ip("127.0.0.1"))),
            base.copy(binding = base.binding.copy(address = ip("0.0.0.0"))),
            base.copy(binding = base.binding.copy(address = ip("192.168.10.0"))),
            base.copy(binding = base.binding.copy(address = ip("192.168.10.255"))),
            base.copy(binding = base.binding.copy(address = ip("fe80::1"))),
            base.copy(broadcastCapable = false),
            base.copy(broadcast = ip("10.0.0.255")),
        )
        cases.forEach { entry ->
            assertEquals(LanResult.Failure(LanError.INTERFACE_UNSUITABLE), resolve(request().copy(bindingKey = entry.binding.key), entry))
        }
        assertEquals(LanResult.Failure(LanError.NETWORK_UNAVAILABLE), resolve())
    }

    @ParameterizedTest
    @ValueSource(ints = [-1, 0, 31, 32, 33])
    fun rejectsPrefixesWithoutUsableDirectedBroadcast(prefix: Int) {
        assertEquals(LanResult.Failure(LanError.NETWORK_UNAVAILABLE), resolve(request(), network(prefix = prefix)))
    }

    @Test
    fun validatesDirectedBroadcastAgainstSelectedSubnet() {
        val net = network(address = "192.168.11.42", prefix = 23).copy(broadcast = ip("192.168.11.255"))
        assertTrue(resolve(request(), net) is LanResult.Success)
        for (raw in listOf("192.168.10.255", "192.168.11.42", "10.0.0.255", "224.0.0.1", "0.0.0.0")) {
            val req = request().copy(configuration = WakeConfiguration(mac, broadcast = ip(raw)))
            assertEquals(LanResult.Failure(LanError.INVALID_INPUT, "broadcast"), resolve(req, net))
        }
    }

    @Test
    fun permitsLimitedBroadcastOnlyWithExplicitInterface() {
        val req = request().copy(configuration = WakeConfiguration(mac, broadcast = ip("255.255.255.255"), port = 7))
        assertEquals(LanResult.Failure(LanError.INTERFACE_SELECTION_REQUIRED), resolve(req, network()))
        val route = (resolve(req.copy(bindingKey = network().binding.key), network()) as LanResult.Success).value
        assertEquals(ip("255.255.255.255"), route.target)
        assertEquals(7, route.port)
    }

    @Test
    fun derivesMissingBroadcastOnlyWhenPlatformConfirmsCapability() {
        val source = network().copy(broadcast = null)
        val route = (resolve(request(), source) as LanResult.Success).value
        assertEquals(ip("192.168.10.255"), route.target)
        assertEquals(LanResult.Failure(LanError.NETWORK_UNAVAILABLE),
            resolve(request(), source.copy(broadcastCapable = false)))
    }

    @Test
    fun defaultRouteHintNeverOverridesUserChoiceOrResolvesAmbiguity() {
        val first = network()
        val second = network("wifi0")
        val snapshot = LanNetworkSnapshot(listOf(first, second), defaultRouteInterfaceKey = "wifi0")
        assertEquals(LanResult.Failure(LanError.INTERFACE_SELECTION_REQUIRED), WakeTargetResolver.resolve(request(), snapshot))
        val selected = request().copy(configuration = WakeConfiguration(mac, preferredInterfaceKey = "eth0"))
        assertEquals(first, (WakeTargetResolver.resolve(selected, snapshot) as LanResult.Success).value.network)
    }
}
