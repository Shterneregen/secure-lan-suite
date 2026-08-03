package com.shterneregen.securelan.desktop.compose.ui.steganography

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButton
import com.shterneregen.securelan.desktop.compose.ui.components.CompactTextField
import com.shterneregen.securelan.desktop.compose.ui.components.TitleWithHelp
import com.shterneregen.securelan.desktop.ui.DesktopMainViewHelpers
import com.shterneregen.securelan.stego.StegoServices
import com.shterneregen.securelan.stego.model.StegoInspectionPoint
import com.shterneregen.securelan.stego.model.StegoInspectionResult
import org.jetbrains.skia.Image as SkiaImage
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.file.Path
import javax.imageio.ImageIO
import java.util.Locale

private const val MOVING_AVERAGE_HELP =
    "Each RGB pixel contributes a value from 0 to 7 formed by its three least-significant bits. " +
        "The chart shows the average value in every interval. A sudden level change can indicate where embedded data begins or ends."

private const val ENTROPY_HELP =
    "For every interval, the analyzer counts zeros and ones in the least-significant RGB bits and calculates Shannon entropy. " +
        "Values near 1 mean an almost even, noise-like bit distribution; abrupt changes may indicate hidden data."

private const val LAST_BIT_IMAGE_HELP =
    "The preview replaces every RGB channel with either 0 or 255 according to its least-significant bit. " +
        "Regular textures and sharp boundaries can reveal structure; noise-like areas can indicate randomized or encrypted payloads."

@Composable
internal fun SteganographyInspectPanel(
    inputPath: String,
    onChooseInput: () -> Unit,
    onPasteInput: () -> Unit,
    onInputSelected: (Path) -> Unit,
    previewOnly: Boolean,
) {
    val tokens = LocalSecureLanDesignTokens.current
    var start by remember { mutableStateOf("0") }
    var end by remember { mutableStateOf("") }
    var interval by remember { mutableStateOf("1024") }
    var result by remember { mutableStateOf<StegoInspectionResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val inspectionService = remember { StegoServices.createDefault().inspectionService() }
    val preview = remember(result) { result?.toImageBitmap() }
    LaunchedEffect(inputPath) {
        result = null
        error = null
    }

    Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
        SteganographyImageDropZone(
            label = "Image to inspect",
            value = inputPath,
            emptyHint = "Choose an image for statistical and last-bit analysis",
            onChoose = onChooseInput,
            onPaste = onPasteInput,
            onImageSelected = { result = null; error = null; onInputSelected(it) },
            enabled = !previewOnly,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs)) {
            CompactTextField(start, { start = it.filter(Char::isDigit) }, "Start", Modifier.weight(1f), placeholder = "0")
            CompactTextField(end, { end = it.filter(Char::isDigit) }, "End", Modifier.weight(1f), placeholder = "image end")
            CompactTextField(interval, { interval = it.filter(Char::isDigit) }, "Interval", Modifier.weight(1f), placeholder = "1024")
        }
        Text(
            "The range uses zero-based pixel numbers. Entropy and moving average are calculated for each interval from RGB least-significant bits.",
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.65f),
        )
        CompactButton(
            enabled = !previewOnly && inputPath.isNotBlank(),
            onClick = {
                runCatching {
                    inspectionService.inspect(
                        DesktopMainViewHelpers.readImageAsBmpBytes(Path.of(inputPath)),
                        start.toIntOrNull() ?: 0,
                        end.toIntOrNull(),
                        interval.toIntOrNull() ?: 1024,
                    )
                }.onSuccess { result = it; error = null }
                    .onFailure { error = it.message ?: "Image inspection failed"; result = null }
            },
        ) { Text("Run analysis") }
        error?.let {
            SteganographyWorkflowStatus(
                label = "Analysis failed",
                detail = it,
                error = true,
                completed = false,
                ready = false,
            )
        }
        result?.let { analysis ->
            Text("Analyzed ${analysis.intervalEnd - analysis.intervalStart} pixels · ${analysis.width} × ${analysis.height}", style = MaterialTheme.typography.body2)
            InspectionChart(
                title = "Moving average of LSB values",
                tooltip = MOVING_AVERAGE_HELP,
                points = analysis.movingAverage,
                lineColor = Color(0xff4d7cff),
                intervalStart = analysis.intervalStart,
                intervalEnd = analysis.intervalEnd,
            )
            InspectionChart(
                title = "Pixel entropy",
                tooltip = ENTROPY_HELP,
                points = analysis.entropy,
                lineColor = Color(0xff20a879),
                intervalStart = analysis.intervalStart,
                intervalEnd = analysis.intervalEnd,
            )
            TitleWithHelp("Last-bit image", LAST_BIT_IMAGE_HELP)
            preview?.let {
                Image(it, "RGB least-significant-bit visualization", Modifier.fillMaxWidth().heightIn(max = 320.dp), contentScale = ContentScale.Fit)
            }
        }
    }
}

@Composable
private fun InspectionChart(
    title: String,
    tooltip: String,
    points: List<StegoInspectionPoint>,
    lineColor: Color,
    intervalStart: Int,
    intervalEnd: Int,
) {
    val minY = points.minOfOrNull { it.value } ?: 0.0
    val maxY = points.maxOfOrNull { it.value } ?: 0.0
    TitleWithHelp(title, tooltip)
    Canvas(Modifier.fillMaxWidth().height(190.dp).padding(8.dp)) {
        if (points.isEmpty()) return@Canvas
        val range = (maxY - minY).takeIf { it > 0.0 } ?: 1.0
        val axis = Color.Gray.copy(alpha = 0.45f)
        drawLine(axis, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
        drawLine(axis, Offset(0f, 0f), Offset(0f, size.height), 1.dp.toPx())
        if (points.size == 1) drawCircle(lineColor, 3.dp.toPx(), Offset(size.width / 2f, size.height / 2f))
        points.zipWithNext().forEachIndexed { index, (a, b) ->
            fun at(point: StegoInspectionPoint, i: Int) = Offset(
                i.toFloat() / (points.size - 1) * size.width,
                size.height - ((point.value - minY) / range).toFloat() * size.height,
            )
            drawLine(lineColor, at(a, index), at(b, index + 1), 2.dp.toPx())
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            "Pixels ${formatInteger(intervalStart)}-${formatInteger(intervalEnd)}",
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.58f),
        )
        Text(
            "Min ${formatDecimal(minY)}  ·  Max ${formatDecimal(maxY)}",
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.58f),
        )
    }
}

private fun formatInteger(value: Int): String = String.format(Locale.US, "%,d", value)

private fun formatDecimal(value: Double): String = String.format(Locale.US, "%.4f", value)

private fun StegoInspectionResult.toImageBitmap(): ImageBitmap {
    val buffered = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    buffered.setRGB(0, 0, width, height, lastBitArgb, 0, width)
    val encoded = ByteArrayOutputStream().use { output -> ImageIO.write(buffered, "png", output); output.toByteArray() }
    return SkiaImage.makeFromEncoded(encoded).toComposeImageBitmap()
}
