package com.shterneregen.securelan.desktop.compose.ui.steganography

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.state.steganography.ComposeSteganographyState
import com.shterneregen.securelan.desktop.compose.LocalReducedMotion
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.motionTween
import com.shterneregen.securelan.desktop.compose.state.steganography.ComposeSteganographyMode
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButton
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButtonTone
import com.shterneregen.securelan.desktop.compose.ui.icons.SecureLanIcons
import java.nio.file.Path

@Composable
internal fun SteganographyCardContent(
    state: ComposeSteganographyState,
    mode: ComposeSteganographyMode,
    onModeChange: (ComposeSteganographyMode) -> Unit,
    coverPath: String,
    onChooseCover: () -> Unit,
    onPasteCover: () -> Unit,
    onCoverSelected: (Path) -> Unit,
    inputPath: String,
    onChooseInput: () -> Unit,
    onPasteInput: () -> Unit,
    onInputSelected: (Path) -> Unit,
    outputPath: String,
    onOutputPathChange: (String) -> Unit,
    onChooseOutput: () -> Unit,
    message: String,
    onMessageChange: (String) -> Unit,
    hidePassword: String,
    onHidePasswordChange: (String) -> Unit,
    extractPassword: String,
    onExtractPasswordChange: (String) -> Unit,
    encrypt: Boolean,
    onEncryptChange: (Boolean) -> Unit,
    encryptedExtract: Boolean,
    onEncryptedExtractChange: (Boolean) -> Unit,
    onHide: () -> Unit,
    savedOutput: Path?,
    onOpenOutputFolder: () -> Unit,
    onSendOutput: (() -> Unit)?,
    sendOutputLabel: String?,
    onExtract: () -> Unit,
    onCopyResult: () -> Unit,
    onSaveResult: () -> Unit,
    onClear: () -> Unit,
    previewOnly: Boolean = false,
) {
    val tokens = LocalSecureLanDesignTokens.current
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colors.background,
    ) {
        Column(
            modifier = Modifier.padding(tokens.spacing.md),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(tokens.radius.medium),
                    color = MaterialTheme.colors.primary.copy(alpha = 0.12f),
                ) {
                    Icon(
                        imageVector = SecureLanIcons.Steganography,
                        contentDescription = null,
                        modifier = Modifier.padding(tokens.spacing.xs).size(24.dp),
                        tint = MaterialTheme.colors.primary,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.xxs)) {
                    Text("Steganography", style = MaterialTheme.typography.h6)
                    Text(
                        text = "Hide and recover text without changing the original image.",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.68f),
                    )
                }
            }

            TabRow(
                selectedTabIndex = mode.ordinal,
                backgroundColor = tokens.colors.surfaceLevel1,
                contentColor = MaterialTheme.colors.primary,
            ) {
                Tab(
                    selected = mode == ComposeSteganographyMode.HIDE,
                    onClick = { onModeChange(ComposeSteganographyMode.HIDE) },
                    modifier = Modifier.heightIn(min = 54.dp),
                    text = { SteganographyTabLabel("Hide", "Create a BMP") },
                )
                Tab(
                    selected = mode == ComposeSteganographyMode.EXTRACT,
                    onClick = { onModeChange(ComposeSteganographyMode.EXTRACT) },
                    modifier = Modifier.heightIn(min = 54.dp),
                    text = { SteganographyTabLabel("Extract", "Read a message") },
                )
                Tab(
                    selected = mode == ComposeSteganographyMode.INSPECT,
                    onClick = { onModeChange(ComposeSteganographyMode.INSPECT) },
                    modifier = Modifier.heightIn(min = 54.dp),
                    text = { SteganographyTabLabel("Inspect", "Check an image") },
                )
            }

            val reduced = LocalReducedMotion.current
            val hideScrollState = rememberScrollState()
            val extractScrollState = rememberScrollState()
            val inspectScrollState = rememberScrollState()
            AnimatedContent(
                targetState = mode,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    fadeIn(motionTween(reduced)) togetherWith fadeOut(motionTween(reduced))
                },
                label = "SteganographyMode",
            ) { currentMode ->
                val scrollState = when (currentMode) {
                    ComposeSteganographyMode.HIDE -> hideScrollState
                    ComposeSteganographyMode.EXTRACT -> extractScrollState
                    ComposeSteganographyMode.INSPECT -> inspectScrollState
                }
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopStart,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(end = tokens.spacing.xs)
                            .verticalScroll(scrollState),
                    ) {
                        when (currentMode) {
                            ComposeSteganographyMode.HIDE -> SteganographyHidePanel(
                                state = state.copy(
                                    passwordDraft = hidePassword,
                                    encryptPayload = encrypt,
                                ),
                                coverPath = coverPath,
                                onChooseCover = onChooseCover,
                                onPasteCover = onPasteCover,
                                onCoverSelected = onCoverSelected,
                                outputPath = outputPath,
                                onOutputPathChange = onOutputPathChange,
                                onChooseOutput = onChooseOutput,
                                message = message,
                                onMessageChange = onMessageChange,
                                password = hidePassword,
                                onPasswordChange = onHidePasswordChange,
                                onEncryptChange = onEncryptChange,
                                onHide = onHide,
                                savedOutput = savedOutput,
                                onOpenOutputFolder = onOpenOutputFolder,
                                onSendOutput = onSendOutput,
                                sendOutputLabel = sendOutputLabel,
                                previewOnly = previewOnly,
                            )

                            ComposeSteganographyMode.EXTRACT -> SteganographyExtractPanel(
                                state = state.copy(
                                    passwordDraft = extractPassword,
                                    encryptedExtract = encryptedExtract,
                                ),
                                inputPath = inputPath,
                                onChooseInput = onChooseInput,
                                onPasteInput = onPasteInput,
                                onInputSelected = onInputSelected,
                                password = extractPassword,
                                onPasswordChange = onExtractPasswordChange,
                                onEncryptedExtractChange = onEncryptedExtractChange,
                                onExtract = onExtract,
                                onCopyResult = onCopyResult,
                                onSaveResult = onSaveResult,
                                previewOnly = previewOnly,
                            )

                            ComposeSteganographyMode.INSPECT -> SteganographyInspectPanel(
                                inputPath = inputPath,
                                onChooseInput = onChooseInput,
                                onPasteInput = onPasteInput,
                                onInputSelected = onInputSelected,
                                previewOnly = previewOnly,
                            )
                        }
                    }
                    VerticalScrollbar(
                        adapter = rememberScrollbarAdapter(scrollState),
                        modifier = Modifier.align(Alignment.CenterEnd),
                    )
                }
            }

            if (!previewOnly) {
                Divider(color = tokens.colors.borderSubtle)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "The source image is never overwritten.",
                        style = MaterialTheme.typography.caption,
                        color = tokens.colors.textSecondary,
                    )
                    CompactButton(
                        onClick = onClear,
                        tone = CompactButtonTone.TERTIARY,
                    ) {
                        Text("Clear form")
                    }
                }
            }
        }
    }
}

@Composable
private fun SteganographyTabLabel(title: String, detail: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.button)
        Text(
            text = detail,
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.58f),
        )
    }
}
