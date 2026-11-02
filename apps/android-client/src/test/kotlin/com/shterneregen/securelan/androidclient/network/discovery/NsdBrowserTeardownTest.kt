package com.shterneregen.securelan.androidclient.network.discovery

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NsdBrowserTeardownTest {
    @Test fun stopRequestWithoutAcknowledgementRemainsOwnedAfterCallerTimeout() = runTest {
        val teardown = NsdBrowserTeardown()
        teardown.closeRequested()
        assertNull(withTimeoutOrNull(2_000) { teardown.awaitClosed(); true })
        // A later successful acknowledgement, not the caller timeout, releases the resource.
        teardown.discoveryStopped()
        assertEquals(true, withTimeoutOrNull(1) { teardown.awaitClosed(); true })
    }

    @Test fun discoveryStopWaitsForAllServiceUnregistrationsIncludingRetiredOnes() = runTest {
        val teardown = NsdBrowserTeardown()
        val first = Any(); val second = Any()
        teardown.registered(first); teardown.registered(second)
        teardown.closeRequested(); teardown.discoveryStopped()
        teardown.unregistered(first)
        assertEquals(1, teardown.pendingSubscriptions)
        assertNull(withTimeoutOrNull(2_000) { teardown.awaitClosed(); true })
        teardown.unregistered(second)
        assertEquals(true, withTimeoutOrNull(1) { teardown.awaitClosed(); true })
    }

    @Test fun oldRegistrationAcknowledgementCannotReleaseReplacement() = runTest {
        val teardown = NsdBrowserTeardown()
        val old = Any(); val replacement = Any()
        teardown.registered(old); teardown.unregistered(old); teardown.registered(replacement)
        teardown.unregistered(old)
        teardown.closeRequested(); teardown.discoveryStopped()
        assertEquals(1, teardown.pendingSubscriptions)
        assertNull(withTimeoutOrNull(1) { teardown.awaitClosed(); true })
        teardown.unregistered(replacement)
        teardown.awaitClosed()
    }

    @Test fun serviceAcknowledgementsBeforeCloseStillRequireDiscoveryStop() = runTest {
        val teardown = NsdBrowserTeardown()
        val token = Any()
        teardown.registered(token); teardown.unregistered(token); teardown.closeRequested()
        val waiter = launch { teardown.awaitClosed() }
        runCurrent(); assertTrue(waiter.isActive)
        waiter.cancelAndJoin()
        assertNull(withTimeoutOrNull(1) { teardown.awaitClosed(); true })
        teardown.discoveryStopped(); teardown.awaitClosed()
    }
}
