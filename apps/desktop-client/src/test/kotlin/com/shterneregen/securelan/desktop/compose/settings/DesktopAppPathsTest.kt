package com.shterneregen.securelan.desktop.compose.settings

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.file.Path

class DesktopAppPathsTest {
    @Test
    fun shouldResolveDirectoryNextToJpackageLauncher() {
        val launcher = Path.of("portable", "SecureLanSuite", "SecureLanSuite.exe").toAbsolutePath().normalize()

        val directory = DesktopAppPaths.resolveApplicationDirectory { key ->
            if (key == DesktopAppPaths.JPACKAGE_APP_PATH_PROPERTY) launcher.toString() else null
        }

        assertEquals(launcher.parent, directory)
    }

    @Test
    fun explicitApplicationDirectoryShouldOverridePackagedLauncher() {
        val explicit = Path.of("custom-portable-data").toAbsolutePath().normalize()
        val launcher = Path.of("portable", "SecureLanSuite.exe").toAbsolutePath().normalize()

        val directory = DesktopAppPaths.resolveApplicationDirectory { key ->
            when (key) {
                DesktopAppPaths.APP_DIRECTORY_PROPERTY -> explicit.toString()
                DesktopAppPaths.JPACKAGE_APP_PATH_PROPERTY -> launcher.toString()
                else -> null
            }
        }

        assertEquals(explicit, directory)
    }

    @Test
    fun shouldUseWorkingDirectoryForIdeLaunch() {
        val workingDirectory = Path.of("ide-workspace").toAbsolutePath().normalize()

        val directory = DesktopAppPaths.resolveApplicationDirectory { key ->
            if (key == "user.dir") workingDirectory.toString() else null
        }

        assertEquals(workingDirectory, directory)
    }
}
