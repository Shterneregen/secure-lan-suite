package com.shterneregen.securelan.desktop.compose.ui.connection

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.SecureLanTheme
import com.shterneregen.securelan.desktop.compose.state.connection.ComposeConnectionHubMode
import com.shterneregen.securelan.desktop.compose.state.connection.ComposeConnectionHubState
import com.shterneregen.securelan.desktop.compose.state.connection.ComposeStatusConnectionState
import org.junit.Rule
import org.junit.Test

/** Exercises the production form, mode controls, semantics and keyboard on a real Compose tree. */
@OptIn(ExperimentalTestApi::class)
class ConnectionInteractionTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun hostJoinRetainsCredentialsAndValidatesThePrimaryAction() {
        var opened = 0
        var joined = 0
        compose.setContent {
            SecureLanTheme {
                var mode by remember { mutableStateOf(ComposeConnectionHubMode.HOST) }
                var nickname by remember { mutableStateOf("") }
                var password by remember { mutableStateOf("") }
                val status = ComposeStatusConnectionState(nickname = nickname, roomPasswordPlaceholder = password)
                Column(Modifier.width(900.dp)) {
                    ConnectionModeSelector(mode, "Host", "Join", { mode = it })
                    ConnectionHubContent(
                        state = ComposeConnectionHubState(status, mode), actionInFlight = null,
                        nickname = nickname, onNicknameChange = { nickname = it },
                        password = password, onPasswordChange = { password = it },
                        mode = mode, serverChatPort = "5050", onServerChatPortChange = {},
                        serverFilePort = "5051", onServerFilePortChange = {},
                        discoverable = true, onDiscoverableChange = {},
                        manualHost = "127.0.0.1", onManualHostChange = {},
                        clientChatPort = "5050", onClientChatPortChange = {},
                        clientFilePort = "5051", onClientFilePortChange = {},
                        onOpenRoom = { opened++ }, onStopHosting = {},
                        onConnect = { joined++ }, onDisconnect = {}, onSetDiscoverable = {},
                    )
                }
            }
        }
        compose.onNodeWithText("Start secure room").assertIsNotEnabled()
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("Alice")
        compose.onAllNodes(hasSetTextAction())[0].assertIsFocused()
        compose.onAllNodes(hasSetTextAction())[1].performTextInput("room-secret")
        compose.onNodeWithText("Start secure room").assertIsEnabled().performClick()
        compose.onNodeWithText("Join", substring = false).performClick()
        compose.onAllNodes(hasSetTextAction())[0].assertTextContains("Alice")
        compose.onNodeWithText("Join", substring = false).assertIsSelected()
        compose.onAllNodes(hasSetTextAction())[1].assertTextContains("room-secret")
        compose.onNodeWithText("Join Room", substring = false).assertIsEnabled().performClick()
        compose.runOnIdle {
            org.junit.Assert.assertEquals(1, opened)
            org.junit.Assert.assertEquals(1, joined)
        }
    }

    @Test
    fun disabledJoinCannotBeActivatedByKeyboard() {
        var changes = 0
        compose.setContent {
            SecureLanTheme {
                ConnectionModeSelector(ComposeConnectionHubMode.HOST, "Host", "Join", { changes++ }, joinEnabled = false)
            }
        }
        compose.onNodeWithText("Join").assertIsNotEnabled().performKeyInput { pressKey(Key.Enter) }
        compose.runOnIdle { org.junit.Assert.assertEquals(0, changes) }
    }
}
