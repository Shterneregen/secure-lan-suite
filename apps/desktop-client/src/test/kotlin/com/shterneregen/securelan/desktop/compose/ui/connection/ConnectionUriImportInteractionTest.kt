package com.shterneregen.securelan.desktop.compose.ui.connection

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import com.shterneregen.securelan.chat.connection.RoomConnectionDescriptor
import com.shterneregen.securelan.common.net.address.IpLiteral
import com.shterneregen.securelan.common.net.interfaces.NetworkBinding
import com.shterneregen.securelan.desktop.compose.SecureLanTheme
import com.shterneregen.securelan.desktop.compose.SecureLanThemeMode
import org.jetbrains.skia.Image
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

/** Exercises production import content without network probes or a chat connection. */
@OptIn(ExperimentalTestApi::class)
class ConnectionUriImportInteractionTest {
    private val localBindings = listOf(NetworkBinding("test", "test", IpLiteral.parse("192.168.1.10")!!, 24))
    private val externalLink = "securelan://join?v=1&proto=2&host=198.51.100.23&port=5050"

    @Test fun pastedLinkNeedsExplicitAddressReviewAndNeverConnects() = runDesktopComposeUiTest(width = 700, height = 640) {
        var imported: RoomConnectionDescriptor? = null
        setContent { SecureLanTheme { ConnectionUriImportContent(onUse = { imported = it }, onClose = {}, loadBindings = { localBindings }) } }
        onNodeWithText("Use for Join").assertIsNotEnabled().assertIsDisplayed()
        onNode(hasSetTextAction()).assertIsFocused().performTextInput(externalLink)
        onNodeWithText("Use for Join").assertIsNotEnabled()
        onNode(isToggleable()).performScrollTo().performClick()
        onNodeWithText("Use for Join").assertIsEnabled().performClick()
        runOnIdle { assertEquals("198.51.100.23", imported?.primaryAddress?.value); assertNull(imported?.filePortHint) }
    }

    @Test fun malformedLinkKeepsImportDisabled() = runDesktopComposeUiTest(width = 700, height = 640) {
        var imported = false
        setContent { SecureLanTheme { ConnectionUriImportContent(onUse = { imported = true }, onClose = {}, loadBindings = { localBindings }) } }
        onNode(hasSetTextAction()).performTextInput("securelan://join?v=99&proto=2&host=192.168.1.42&port=5050")
        onNodeWithText("This room link requires a newer client.").assertIsDisplayed()
        onNodeWithText("Use for Join").assertIsNotEnabled()
        runOnIdle { assertFalse(imported) }
    }

    @Test fun includedPasswordIsConcealedAndCarriedToJoinOnlyAfterReview() = runDesktopComposeUiTest(width = 700, height = 640) {
        var imported: RoomConnectionDescriptor? = null
        setContent { SecureLanTheme { ConnectionUriImportContent(onUse = { imported = it }, onClose = {}, loadBindings = { localBindings }) } }
        onNode(hasSetTextAction()).performTextInput("$externalLink&password=invite%2B%26")
        onNode(hasSetTextAction()).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Password))
        onNodeWithContentDescription("Show room link").performClick()
        onNodeWithContentDescription("Hide room link").assertExists()
        val displayed = mutableListOf<TextLayoutResult>()
        onNode(hasSetTextAction()).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(displayed) }
        assertEquals("$externalLink&password=invite%2B%26", displayed.single().layoutInput.text.text)
        onNodeWithContentDescription("Hide room link").performClick()
        onNodeWithText("The room password is included and will be filled in for Join.").performScrollTo().assertIsDisplayed()
        runOnIdle { assertNull(imported) }
        onNode(isToggleable()).performScrollTo().performClick()
        onNodeWithText("Use for Join").performClick()
        runOnIdle { assertEquals("invite+&", imported?.password) }
    }

    @Test fun addressChangesResetConfirmationAndBroadcastNeverImports() = runDesktopComposeUiTest(width = 700, height = 640) {
        var imported: RoomConnectionDescriptor? = null
        setContent { SecureLanTheme { ConnectionUriImportContent(onUse = { imported = it }, onClose = {}, loadBindings = { localBindings }) } }
        onNode(hasSetTextAction()).performTextInput("$externalLink&alt=203.0.113.2")
        onNode(isToggleable()).performScrollTo().performClick()
        onNodeWithText("Use for Join").assertIsEnabled()
        onNodeWithContentDescription("Use address 203.0.113.2").performScrollTo().performClick()
        onNodeWithText("Use for Join").assertIsNotEnabled()
        onNode(isToggleable()).performScrollTo().performClick()
        onNodeWithText("Use for Join").performClick()
        runOnIdle { assertEquals("203.0.113.2", imported?.primaryAddress?.value); imported = null }
        onNode(hasSetTextAction()).performScrollTo().performTextReplacement("securelan://join?v=1&proto=2&host=192.168.1.255&port=5050")
        onNodeWithText("A network broadcast address cannot be used.").performScrollTo().assertIsDisplayed()
        onNodeWithText("Use for Join").assertIsNotEnabled()
        onNode(isToggleable()).assertDoesNotExist()
        runOnIdle { assertNull(imported) }
    }

    @Test fun editedInvitationRequiresFreshExternalConfirmation() = runDesktopComposeUiTest(width = 700, height = 640) {
        setContent { SecureLanTheme { ConnectionUriImportContent({}, {}, loadBindings = { localBindings }) } }
        onNode(hasSetTextAction()).performTextInput(externalLink)
        onNode(isToggleable()).performScrollTo().performClick()
        onNodeWithText("Use for Join").assertIsEnabled()
        onNode(hasSetTextAction()).performScrollTo().performTextReplacement(externalLink.replace("5050", "5053"))
        onNodeWithText("Use for Join").assertIsNotEnabled()
        onNode(isToggleable()).performScrollTo().assertIsOff()
    }

    @Test fun scaledDialogKeepsExternalConfirmationReachable() = runDesktopComposeUiTest(width = 700, height = 640) {
        setContent { CompositionLocalProvider(LocalDensity provides Density(1.25f, 1.2f)) { SecureLanTheme {
            Box(Modifier.fillMaxSize().testTag("import-dialog")) { ConnectionUriImportContent({}, {}, loadBindings = { emptyList() }) }
        } } }
        onNode(hasSetTextAction()).performTextInput(externalLink)
        onNodeWithText("Use for Join").assertIsDisplayed().assertIsNotEnabled()
        onNode(isToggleable()).performScrollTo().performClick()
        onNodeWithText("Use for Join").assertIsDisplayed().assertIsEnabled()
        val bitmap = onNodeWithTag("import-dialog").captureToImage().asSkiaBitmap()
        val directory = Path.of("build/reports/target-ui")
        Files.createDirectories(directory)
        Image.makeFromBitmap(bitmap).use { image -> image.encodeToData()!!.use { Files.write(directory.resolve("import-room-link-scaled.png"), it.bytes) } }
    }

    @Test fun compactDialogKeepsFooterVisibleAndSupportsThemes() {
        for (width in listOf(360, 700)) runDesktopComposeUiTest(width = width, height = if (width == 360) 360 else 640) {
            var theme by mutableStateOf(SecureLanThemeMode.DARK)
            var closes = 0
            setContent { SecureLanTheme(themeMode = theme) { Box(Modifier.fillMaxSize().testTag("import-dialog")) {
                ConnectionUriImportContent({}, { closes++ }, loadBindings = { localBindings })
            } } }
            onNode(hasSetTextAction()).performTextInput("securelan://join?v=1&proto=2&host=192.168.1.42&port=5050&filePort=5051&name=Atlas&password=synthetic-secret")
            for (mode in SecureLanThemeMode.entries) {
                runOnIdle { theme = mode }
                onNodeWithText("Use for Join").assertIsDisplayed().assertIsEnabled()
                onNodeWithText("Cancel").assertIsDisplayed()
                onNodeWithText("Room name").performScrollTo().assertIsDisplayed()
                val bitmap = onNodeWithTag("import-dialog").captureToImage().asSkiaBitmap()
                val directory = Path.of("build/reports/target-ui")
                Files.createDirectories(directory)
                Image.makeFromBitmap(bitmap).use { image -> image.encodeToData()!!.use { Files.write(directory.resolve("import-room-link-$width-${mode.name.lowercase()}.png"), it.bytes) } }
            }
            onNodeWithText("Cancel").performClick()
            onNodeWithContentDescription("Close import room link").performClick()
            onNode(hasSetTextAction()).performKeyInput { pressKey(Key.Escape) }
            runOnIdle { assertEquals(3, closes) }
        }
    }
}
