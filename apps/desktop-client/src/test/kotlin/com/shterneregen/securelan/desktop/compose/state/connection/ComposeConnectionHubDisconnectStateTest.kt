package com.shterneregen.securelan.desktop.compose.state.connection

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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
}
