package com.shterneregen.securelan.androidclient

import android.content.SharedPreferences
import androidx.lifecycle.ViewModelStore
import com.shterneregen.securelan.androidclient.call.*
import com.shterneregen.securelan.androidclient.network.*
import com.shterneregen.securelan.androidclient.protocol.WireMessage
import com.shterneregen.securelan.chat.protocol.handshake.PeerCapabilities
import com.shterneregen.securelan.common.model.rtc.RtcSignalEnvelope
import com.shterneregen.securelan.crypto.protocol.PeerIdentity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Proxy

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    @Test fun `workspace orchestration runs with fakes without Android or native initialization`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val values = mutableMapOf<String, Any?>()
        val editor = Proxy.newProxyInstance(javaClass.classLoader, arrayOf(SharedPreferences.Editor::class.java)) { proxy, method, args ->
            when {
                method.name.startsWith("put") -> { values[args!![0] as String] = args[1]; proxy }
                method.name == "commit" -> true
                method.name == "apply" -> null
                else -> proxy
            }
        } as SharedPreferences.Editor
        val preferences = Proxy.newProxyInstance(javaClass.classLoader, arrayOf(SharedPreferences::class.java)) { _, method, args ->
            when (method.name) {
                "edit" -> editor
                "getString", "getInt", "getBoolean" -> values[args!![0]] ?: args[1]
                else -> null
            }
        } as SharedPreferences
        var chatCloses = 0
        var callCloses = 0
        val chat = object : RoomClient {
            override val fileIdentity = PeerIdentity()
            override suspend fun connect(host: String, port: Int, requestedNickname: String, sessionPassword: String, capabilities: PeerCapabilities): String = error("Injected connection failure")
            override suspend fun sendMessage(text: String) = Unit
            override suspend fun readMessage(): WireMessage? = null
            override suspend fun sendSignal(signal: RtcSignalEnvelope) = Unit
            override suspend fun disconnect() = Unit
            override fun close() { chatCloses++ }
        }
        val callState = MutableStateFlow(CallState())
        val callUi = MutableStateFlow(CallUiState())
        val calls = Proxy.newProxyInstance(javaClass.classLoader, arrayOf(CallSession::class.java)) { _, method, _ ->
            when (method.name) { "getState" -> callState; "getUiState" -> callUi; "close" -> { callCloses++; null }; else -> null }
        } as CallSession
        val host = object : HostRoom {
            override val state = MutableStateFlow(AndroidHostServiceState())
            override fun password() = "password"
            override fun start(peerId: String, nickname: String, password: String, chatPort: Int, filePort: Int) { error("Injected host failure") }
            override fun stop() = Unit
        }
        val store = ViewModelStore()
        try {
            val model = MainViewModel(MainDependencies(preferences, chat, { emptyFlow() }, { listOf("192.168.1.2") }, host,
                { _, _, _, _ -> calls }, ioDispatcher = StandardTestDispatcher(testScheduler)))
            store.put("workspace", model)
            runCurrent()
            model.updateNickname("Alice")
            model.updateSessionPassword("password")
            model.startHosting("5050", "5051")
            assertFalse(model.uiState.value.hostingStarting)
            assertEquals("Injected host failure", model.uiState.value.error)
            assertEquals(listOf("192.168.1.2"), model.uiState.value.localRoomAddresses)
            assertEquals("Alice", values["nickname"])
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
        assertEquals(1, callCloses)
        assertEquals(1, chatCloses)
    }
}
