package com.shterneregen.securelan.lantools

import com.shterneregen.securelan.lantools.model.*
import com.shterneregen.securelan.lantools.wol.JvmLanNetworkInfoProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.net.SocketException

class JvmLanNetworkInfoProviderTest {
    @Test
    fun preservesSourceBindingsForCallerPolicyAndSupportsEmptySnapshot() = runTest {
        val entries = listOf(network(), network("down").let { it.copy(binding = it.binding.copy(up = false)) })
        val provider = JvmLanNetworkInfoProvider(StandardTestDispatcher(testScheduler), { null }) { entries.asSequence() }
        assertEquals(entries, (provider.snapshot() as LanResult.Success).value.interfaces)
        val empty = JvmLanNetworkInfoProvider(StandardTestDispatcher(testScheduler), { null }) { emptySequence() }
        assertTrue((empty.snapshot() as LanResult.Success).value.interfaces.isEmpty())
    }

    @Test
    fun returnsTypedEnumerationFailureInsteadOfPartialSnapshot() = runTest {
        for ((error, code) in listOf(SecurityException("private") to LanError.PERMISSION_DENIED,
            SocketException("private") to LanError.NETWORK_UNAVAILABLE)) {
            val provider = JvmLanNetworkInfoProvider(StandardTestDispatcher(testScheduler), { null }) {
                sequence { yield(network()); throw error }
            }
            assertEquals(LanResult.Failure(code), provider.snapshot())
        }
    }

    @Test
    fun boundsUnendingSourceWithoutReturningPartialInventory() = runTest {
        var reads = 0
        val provider = JvmLanNetworkInfoProvider(StandardTestDispatcher(testScheduler), { null }) {
            generateSequence { reads++; network() }
        }
        assertEquals(LanResult.Failure(LanError.RESOURCE_LIMIT), provider.snapshot())
        assertEquals(513, reads)
    }

    @Test
    fun propagatesCancellationInsteadOfContinuingEnumeration() = runTest {
        val provider = JvmLanNetworkInfoProvider(StandardTestDispatcher(testScheduler), { null }) { error("enumerated") }
        val operation = launch { currentCoroutineContext().cancel(); provider.snapshot() }
        operation.join()
        assertTrue(operation.isCancelled)
    }

    @Test
    fun refreshesDefaultRouteRecommendationWithoutCaching() = runTest {
        var hint = "eth0"
        val provider = JvmLanNetworkInfoProvider(StandardTestDispatcher(testScheduler), { hint }) {
            sequenceOf(network(), network("wifi0"))
        }
        assertEquals("eth0", (provider.snapshot() as LanResult.Success).value.defaultRouteInterfaceKey)
        hint = "wifi0"
        assertEquals("wifi0", (provider.snapshot() as LanResult.Success).value.defaultRouteInterfaceKey)
    }

    @Test
    fun ignoresMissingDownAndMalformedRouteRecommendations() = runTest {
        val down = network("down").let { it.copy(binding = it.binding.copy(up = false)) }
        for (hint in listOf(null, "missing", "down", "", "white space", "a".repeat(257))) {
            val provider = JvmLanNetworkInfoProvider(StandardTestDispatcher(testScheduler), { hint }) {
                sequenceOf(network(), down)
            }
            assertNull((provider.snapshot() as LanResult.Success).value.defaultRouteInterfaceKey)
        }
    }

    @Test
    fun optionalRouteReadFailureDoesNotInvalidateInterfaceSnapshot() = runTest {
        for (error in listOf(java.io.IOException("unavailable"), SecurityException("denied"))) {
            val provider = JvmLanNetworkInfoProvider(StandardTestDispatcher(testScheduler), { throw error }) {
                sequenceOf(network())
            }
            val snapshot = (provider.snapshot() as LanResult.Success).value
            assertNull(snapshot.defaultRouteInterfaceKey)
            assertEquals(listOf(network()), snapshot.interfaces)
        }
    }
}
