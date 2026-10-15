package com.shterneregen.securelan.desktop.compose.settings

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class DesktopSettingsPersistenceTest {
    @Test fun failedSaveRemainsAppliedAndRetryClearsTheError() {
        val completions = LinkedBlockingQueue<() -> Unit>()
        var writable = false
        val writes = LinkedBlockingQueue<DesktopAppSettings>()
        val store = object : DesktopAppSettingsStore {
            override fun load() = DesktopAppSettings()
            override fun save(settings: DesktopAppSettings): Boolean { writes.add(settings); return writable }
        }
        DesktopAppSettingsController(store = store, dispatch = { completions.add(it) }).use { controller ->
            controller.update { it.copy(displayName = "Alice") }
            completions.poll(2, TimeUnit.SECONDS)!!.invoke()
            assertEquals("Alice", controller.settings.displayName)
            assertNotNull(controller.persistenceError)
            writable = true
            controller.retryPersistence()
            completions.poll(2, TimeUnit.SECONDS)!!.invoke()
            assertNull(controller.persistenceError)
            assertEquals(listOf("Alice", "Alice"), List(2) { writes.poll().displayName })
        }
    }

    @Test fun writesAreOrderedAndOlderCompletionsCannotClearNewerState() {
        val completions = LinkedBlockingQueue<() -> Unit>()
        val writes = LinkedBlockingQueue<String?>()
        val store = object : DesktopAppSettingsStore {
            override fun load() = DesktopAppSettings()
            override fun save(settings: DesktopAppSettings): Boolean { writes.add(settings.displayName); return settings.displayName == "old" }
        }
        DesktopAppSettingsController(store = store, dispatch = { completions.add(it) }).use { controller ->
            controller.update { it.copy(displayName = "old") }
            controller.update { it.copy(displayName = "latest") }
            completions.poll(2, TimeUnit.SECONDS)!!.invoke()
            assertTrue(controller.saving)
            completions.poll(2, TimeUnit.SECONDS)!!.invoke()
            assertEquals("latest", controller.settings.displayName)
            assertNotNull(controller.persistenceError)
            assertEquals(listOf("old", "latest"), List(2) { writes.poll() })
        }
    }
}
