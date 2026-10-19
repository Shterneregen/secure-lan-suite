package com.shterneregen.securelan.androidclient

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.core.content.edit
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.shterneregen.securelan.androidclient.network.AndroidChatHostService
import com.shterneregen.securelan.androidclient.network.SecureFileReceiver
import com.shterneregen.securelan.filetransfer.protocol.FileTransferPeerAuthentication
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.net.ServerSocket

/** Real Android storage, provider, permission and foreground-service boundaries on min/current API. */
@RunWith(AndroidJUnit4::class)
class PlatformBoundaryTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test
    fun interruptedReceiveIsRemovedByTheRealDownloadsProvider() = runBlocking {
        if (Build.VERSION.SDK_INT <= 28) {
            grantPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        val journal = context.getSharedPreferences("pending_received_files", Context.MODE_PRIVATE)
        val uri = if (Build.VERSION.SDK_INT >= 29) {
            context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, android.content.ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, "securelan-device-test-${System.nanoTime()}.txt")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/SecureLan")
                put(MediaStore.Downloads.IS_PENDING, 1)
            })!!.also { target -> journal.edit(commit = true) { putString("uri", target.toString()) } }
        } else null
        val temp = if (uri == null) {
            val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "SecureLan").apply { mkdirs() }
            File.createTempFile(".securelan-incoming-", ".tmp", directory).also { target ->
                journal.edit(commit = true) { putString("temp", target.absolutePath) }
            }
        } else null
        val receiver = SecureFileReceiver(context)
        val port = ServerSocket(0).use { it.localPort }
        val job = launch(Dispatchers.IO) {
            receiver.serve(port, "device-test", {}, { _, _ -> }, { _, _ -> }, { _, error -> throw AssertionError(error) },
                FileTransferPeerAuthentication("device-test", com.shterneregen.securelan.crypto.protocol.PeerIdentity()) { null })
        }
        try {
            withTimeout(10_000) { while (journal.all.isNotEmpty()) delay(25) }
            if (uri != null) {
                context.contentResolver.query(uri, null, null, null, null)?.use { assertEquals(0, it.count) }
            }
            if (temp != null) assertFalse(temp.exists())
        } finally {
            receiver.close()
            job.cancelAndJoin()
            uri?.let { context.contentResolver.delete(it, null, null) }
            temp?.delete()
        }
    }

    @Test
    fun cameraPermissionDenialAndGrantAllowActivityRecreation() {
        // Orchestrator clears package data and permission state between tests.
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { assertEquals(android.content.pm.PackageManager.PERMISSION_DENIED, it.checkSelfPermission(Manifest.permission.CAMERA)) }
            grantPermission(Manifest.permission.CAMERA)
            scenario.recreate()
            scenario.onActivity { assertEquals(android.content.pm.PackageManager.PERMISSION_GRANTED, it.checkSelfPermission(Manifest.permission.CAMERA)) }
        }
    }

    private fun grantPermission(permission: String) {
        if (Build.VERSION.SDK_INT >= 28) {
            instrumentation.uiAutomation.grantRuntimePermission(context.packageName, permission)
        } else {
            // UiAutomation.grantRuntimePermission is unavailable on Android 8/8.1.
            val command = "pm grant ${context.packageName} $permission"
            ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command)).use { stream ->
                assertEquals("", stream.bufferedReader().readText().trim())
            }
        }
        assertEquals(PackageManager.PERMISSION_GRANTED, context.checkSelfPermission(permission))
    }

    @Test
    fun foregroundHostSurvivesActivityRecreationAndReleasesItsPort() = runBlocking {
        val (port, filePort) = ServerSocket(0).use { chat ->
            ServerSocket(0).use { files -> chat.localPort to files.localPort }
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            try {
                scenario.onActivity { AndroidChatHostService.start(it, "device-test", "device-test", "device-test-password", port, filePort) }
                withTimeout(10_000) { while (!AndroidChatHostService.state.value.running) delay(25) }
                scenario.recreate()
                assertTrue(AndroidChatHostService.state.value.running)
            } finally {
                AndroidChatHostService.stop(context)
                withTimeout(10_000) { while (AndroidChatHostService.state.value.running || AndroidChatHostService.state.value.starting) delay(25) }
            }
        }
        ServerSocket(port).use { assertTrue(it.isBound) }
    }
}
