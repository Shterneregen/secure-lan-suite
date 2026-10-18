package com.shterneregen.securelan.desktop.compose.ui.connection

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.chat.connection.RoomConnectionDescriptor
import com.shterneregen.securelan.desktop.compose.SecureLanTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** The actual dialog only returns a validated target after explicit review. */
class ConnectionUriImportInteractionTest {
    @get:Rule val compose = createComposeRule()
    @Test fun pastedLinkNeedsExplicitAddressReviewAndNeverConnects() {
        var imported: RoomConnectionDescriptor? = null
        compose.setContent { SecureLanTheme { ConnectionUriImportContent(onUse = { imported = it }, onClose = {}) } }
        compose.onNodeWithText("Use for Join").assertIsNotEnabled()
        compose.onNode(hasSetTextAction()).assertIsFocused()
        compose.onNode(hasSetTextAction()).performTextInput("securelan://join?v=1&proto=2&host=198.51.100.23&port=5050")
        compose.onNodeWithText("Use for Join").assertIsNotEnabled()
        compose.onNode(isToggleable()).performScrollTo().performClick()
        compose.onNodeWithText("Use for Join").assertIsEnabled().performScrollTo().performClick()
        compose.runOnIdle { assertEquals("198.51.100.23", imported?.primaryAddress?.value); assertNull(imported?.filePortHint) }
    }
    @Test fun malformedLinkKeepsImportDisabled() {
        var imported = false
        compose.setContent { SecureLanTheme { ConnectionUriImportContent(onUse = { imported = true }, onClose = {}) } }
        compose.onNode(hasSetTextAction()).performTextInput("securelan://join?v=99&proto=2&host=192.168.1.42&port=5050")
        compose.onNodeWithText("This room link requires a newer client.").assertExists()
        compose.onNodeWithText("Use for Join").assertIsNotEnabled()
        compose.runOnIdle { assertFalse(imported) }
    }

    @Test fun includedPasswordIsCarriedToJoinOnlyAfterReview() {
        var imported: RoomConnectionDescriptor? = null
        compose.setContent { SecureLanTheme { ConnectionUriImportContent(onUse = { imported = it }, onClose = {}) } }
        compose.onNode(hasSetTextAction()).performTextInput("securelan://join?v=1&proto=2&host=198.51.100.23&port=5050&password=invite%2B%26")
        compose.onNodeWithText("The room password is included and will be filled in for Join.").assertExists()
        compose.runOnIdle { assertNull(imported) }
        compose.onNode(isToggleable()).performScrollTo().performClick()
        compose.onNodeWithText("Use for Join").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("invite+&", imported?.password) }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun compactDialogScrollsToReviewAndActions() {
        compose.setContent { SecureLanTheme {
            Box(Modifier.size(360.dp, 360.dp).testTag("import-dialog")) { ConnectionUriImportContent({}, {}) }
        } }
        compose.onNode(hasSetTextAction()).performTextInput("securelan://join?v=1&proto=2&host=198.51.100.23&port=5050")
        compose.onNode(isToggleable()).performScrollTo().performClick()
        compose.onNodeWithText("Use for Join").performScrollTo().assertIsDisplayed().assertIsEnabled()
        val bitmap = compose.onNodeWithTag("import-dialog").captureToImage()
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.readPixels(pixels)
        val image = java.awt.image.BufferedImage(bitmap.width, bitmap.height, java.awt.image.BufferedImage.TYPE_INT_ARGB)
        image.setRGB(0, 0, bitmap.width, bitmap.height, pixels, 0, bitmap.width)
        val output = java.io.File("build/reports/mdns/ui/import-room-link-360.png")
        output.parentFile.mkdirs()
        javax.imageio.ImageIO.write(image, "png", output)
    }
}
