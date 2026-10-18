package com.shterneregen.securelan.desktop.compose.ui.connection

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.shterneregen.securelan.chat.connection.ConnectionUriParseResult
import com.shterneregen.securelan.chat.connection.RoomConnectionDescriptor
import com.shterneregen.securelan.chat.connection.RoomConnectionUriCodec
import com.shterneregen.securelan.common.net.address.IpLiteral
import com.shterneregen.securelan.desktop.compose.SecureLanTheme
import com.shterneregen.securelan.desktop.compose.ui.qr.renderQrCode
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class RoomInvitationInteractionTest {
    @get:Rule val compose = createComposeRule()
    private val address = IpLiteral.parse("192.168.1.42")!!
    private val descriptor = RoomConnectionDescriptor(address, 5050, 5051,
        "2da1e450-798a-401f-990b-cb1cc4f61d92", "ab817e84-78af-40b4-994d-41bfa5ff20fb", "Secure room", password = "synthetic+&%25")
    private val uri = RoomConnectionUriCodec.encode(descriptor)

    @Test fun compactDefaultShowsScannablePasswordQrAndActionsInOneRow() {
        val qr = Result.success(renderQrCode(uri))
        var copies = 0
        compose.setContent { SecureLanTheme {
            Box(Modifier.size(400.dp, 470.dp).testTag("invitation")) {
                RoomInvitationContent(listOf(address), address, 5050, uri, qr, false, {}, { copies++ }, {})
            }
        } }
        val copy = compose.onNodeWithText("Copy room link").assertIsDisplayed().assertIsFocused()
        val close = compose.onNodeWithText("Close").assertIsDisplayed()
        assertEquals(copy.fetchSemanticsNode().boundsInRoot.center.y, close.fetchSemanticsNode().boundsInRoot.center.y, 1f)
        val qrImage = compose.onNodeWithContentDescription("Room invitation QR").assertIsDisplayed().captureToImage()
        val pixels = IntArray(qrImage.width * qrImage.height)
        qrImage.readPixels(pixels)
        val decoded = QRCodeReader().decode(BinaryBitmap(HybridBinarizer(RGBLuminanceSource(qrImage.width, qrImage.height, pixels)))).text
        assertEquals(uri, decoded)
        assertEquals(descriptor.password, (RoomConnectionUriCodec.decode(decoded) as ConnectionUriParseResult.Success).descriptor.password)
        copy.performClick()
        compose.runOnIdle { assertEquals(1, copies) }
        screenshot("invite-room-400")
    }

    @Test fun narrowWindowScrollsAddressesWhileKeepingActionsVisible() {
        val addresses = (1..12).map { IpLiteral.parse("192.168.$it.42")!! }
        val qr = Result.success(renderQrCode(uri))
        var selected: IpLiteral? = null
        compose.setContent { SecureLanTheme {
            Box(Modifier.size(320.dp, 300.dp).testTag("invitation")) {
                RoomInvitationContent(addresses, addresses.first(), 5050, uri, qr, true,
                    { selected = it }, {}, {})
            }
        } }
        compose.onNodeWithText("Copy room link").assertIsDisplayed()
        compose.onNodeWithText(addresses.last().value).performScrollTo().performClick()
        compose.runOnIdle { assertEquals(addresses.last(), selected) }
        compose.onNodeWithText("Copy room link").assertIsDisplayed()
        compose.onNodeWithText("Close").assertIsDisplayed()
        screenshot("invite-room-320")
    }

    @Test fun stoppedHostDisablesCopyAndStillAllowsClose() {
        var closed = false
        compose.setContent { SecureLanTheme {
            Box(Modifier.size(320.dp, 300.dp)) {
                RoomInvitationContent(emptyList(), null, null, null, null, false, {}, {}, { closed = true })
            }
        } }
        compose.onNodeWithText("The room has stopped.").assertIsDisplayed()
        compose.onNodeWithText("Copy room link").assertIsNotEnabled()
        compose.onNodeWithText("Close").performClick()
        compose.runOnIdle { assertTrue(closed) }
    }

    private fun screenshot(name: String) {
        val bitmap = compose.onNodeWithTag("invitation").captureToImage()
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.readPixels(pixels)
        val output = java.io.File("build/reports/mdns/ui/$name.png")
        output.parentFile.mkdirs()
        val image = java.awt.image.BufferedImage(bitmap.width, bitmap.height, java.awt.image.BufferedImage.TYPE_INT_ARGB)
        image.setRGB(0, 0, bitmap.width, bitmap.height, pixels, 0, bitmap.width)
        javax.imageio.ImageIO.write(image, "png", output)
    }
}
