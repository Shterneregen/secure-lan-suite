package com.shterneregen.securelan.desktop.compose.ui.connection

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.Density
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
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.math.roundToInt

@OptIn(ExperimentalTestApi::class)
@RunWith(Parameterized::class)
class RoomInvitationInteractionTest(private val scale: Float) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "density={0}")
        fun densities() = listOf(arrayOf(1f), arrayOf(1.1f), arrayOf(1.25f), arrayOf(1.5f), arrayOf(1.75f), arrayOf(2f))
    }

    private val address = IpLiteral.parse("192.168.1.42")!!
    private val descriptor = RoomConnectionDescriptor(address, 5050, 5051,
        "2da1e450-798a-401f-990b-cb1cc4f61d92", "ab817e84-78af-40b4-994d-41bfa5ff20fb", "Secure room", password = "synthetic+&%25")
    private val uri = RoomConnectionUriCodec.encode(descriptor)

    @Test fun compactDefaultShowsScannablePasswordQrAndActionsInOneRow() = invitationTest {
        val qr = Result.success(renderQrCode(uri))
        var copies = 0
        setContent { CompositionLocalProvider(LocalDensity provides Density(scale)) { SecureLanTheme {
            Box(Modifier.size(400.dp, 470.dp).testTag("invitation")) {
                RoomInvitationContent(listOf(address), address, 5050, uri, qr, false, {}, { copies++ }, {})
            }
        } } }
        val copy = onNodeWithText("Copy room link").assertIsDisplayed().assertIsFocused()
        val close = onNodeWithText("Close").assertIsDisplayed()
        assertEquals(copy.fetchSemanticsNode().boundsInRoot.center.y, close.fetchSemanticsNode().boundsInRoot.center.y, 1f)
        val qrImage = onNodeWithContentDescription("Room invitation QR").assertIsDisplayed().captureToImage()
        val pixels = IntArray(qrImage.width * qrImage.height)
        qrImage.readPixels(pixels)
        screenshot("invite-room-400-$scale")
        val decoded = QRCodeReader().decode(BinaryBitmap(HybridBinarizer(RGBLuminanceSource(qrImage.width, qrImage.height, pixels)))).text
        assertEquals(uri, decoded)
        assertEquals(descriptor.password, (RoomConnectionUriCodec.decode(decoded) as ConnectionUriParseResult.Success).descriptor.password)
        copy.performClick()
        runOnIdle { assertEquals(1, copies) }
    }

    @Test fun narrowWindowScrollsAddressesWhileKeepingActionsVisible() = invitationTest {
        val addresses = (1..12).map { IpLiteral.parse("192.168.$it.42")!! }
        val qr = Result.success(renderQrCode(uri))
        var selected: IpLiteral? = null
        setContent { CompositionLocalProvider(LocalDensity provides Density(scale)) { SecureLanTheme {
            Box(Modifier.size(320.dp, 300.dp).testTag("invitation")) {
                RoomInvitationContent(addresses, addresses.first(), 5050, uri, qr, true,
                    { selected = it }, {}, {})
            }
        } } }
        onNodeWithText("Copy room link").assertIsDisplayed()
        onNodeWithText(addresses.last().value).performScrollTo().performClick()
        runOnIdle { assertEquals(addresses.last(), selected) }
        onNodeWithText("Copy room link").assertIsDisplayed()
        onNodeWithText("Close").assertIsDisplayed()
        screenshot("invite-room-320-$scale")
    }

    @Test fun stoppedHostDisablesCopyAndStillAllowsClose() = invitationTest {
        var closed = false
        setContent { CompositionLocalProvider(LocalDensity provides Density(scale)) { SecureLanTheme {
            Box(Modifier.size(320.dp, 300.dp)) {
                RoomInvitationContent(emptyList(), null, null, null, null, false, {}, {}, { closed = true })
            }
        } } }
        onNodeWithText("The room has stopped.").assertIsDisplayed()
        onNodeWithText("Copy room link").assertIsNotEnabled()
        onNodeWithText("Close").performClick()
        runOnIdle { assertTrue(closed) }
    }

    private fun invitationTest(block: suspend DesktopComposeUiTest.() -> Unit) = runDesktopComposeUiTest(
        width = (400 * scale).roundToInt(), height = (470 * scale).roundToInt(), block = block,
    )

    private fun DesktopComposeUiTest.screenshot(name: String) {
        val bitmap = onNodeWithTag("invitation").captureToImage()
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.readPixels(pixels)
        val output = java.io.File("build/reports/mdns/ui/$name.png")
        output.parentFile.mkdirs()
        val image = java.awt.image.BufferedImage(bitmap.width, bitmap.height, java.awt.image.BufferedImage.TYPE_INT_ARGB)
        image.setRGB(0, 0, bitmap.width, bitmap.height, pixels, 0, bitmap.width)
        javax.imageio.ImageIO.write(image, "png", output)
    }
}
