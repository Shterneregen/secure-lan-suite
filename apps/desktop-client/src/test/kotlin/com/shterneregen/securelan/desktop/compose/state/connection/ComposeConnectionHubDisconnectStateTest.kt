package com.shterneregen.securelan.desktop.compose.state.connection

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ComposeConnectionHubDisconnectStateTest {
    @Test
    fun remoteDisconnectShouldDisableDisconnectAction() {
        val connected = ComposeStatusConnectionState(
            clientConnected = true,
            connectionStatus = "Connected",
        )

        val disconnected = connected.withClientDisconnected()
        val hub = ComposeConnectionHubState(
            statusState = disconnected,
            mode = ComposeConnectionHubMode.JOIN,
        )

        assertFalse(disconnected.clientConnected)
        assertEquals("Connection idle", disconnected.connectionStatus)
        assertFalse(hub.secondaryActionEnabled)
    }

    @Test
    fun runningServerShouldDisableCredentialFieldsUntilStopped() {
        val running = ComposeConnectionHubState(
            statusState = ComposeStatusConnectionState(localServerRunning = true),
            mode = ComposeConnectionHubMode.HOST,
        )
        val stopped = ComposeConnectionHubState(
            statusState = running.statusState.copy(localServerRunning = false),
            mode = ComposeConnectionHubMode.HOST,
        )

        assertFalse(running.credentialFieldsEnabled)
        assertTrue(stopped.credentialFieldsEnabled)
    }

    @Test
    fun connectedClientShouldDisableCredentialFieldsUntilDisconnected() {
        val connected = ComposeConnectionHubState(
            statusState = ComposeStatusConnectionState(clientConnected = true),
            mode = ComposeConnectionHubMode.JOIN,
        )
        val disconnected = ComposeConnectionHubState(
            statusState = connected.statusState.withClientDisconnected(),
            mode = ComposeConnectionHubMode.JOIN,
        )

        assertFalse(connected.credentialFieldsEnabled)
        assertTrue(disconnected.credentialFieldsEnabled)
    }
}
