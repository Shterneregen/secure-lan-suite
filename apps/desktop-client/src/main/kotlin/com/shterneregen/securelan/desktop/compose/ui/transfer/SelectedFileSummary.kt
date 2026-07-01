package com.shterneregen.securelan.desktop.compose.ui.transfer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import java.nio.file.Paths

@Composable
internal fun SelectedFileSummary(
    filePath: String,
    fallbackSummary: String,
    modifier: Modifier = Modifier,
) {
    val trimmedPath = filePath.trim()
    val fileName = trimmedPath
        .takeIf(String::isNotEmpty)
        ?.let { runCatching { Paths.get(it).fileName?.toString() ?: it }.getOrDefault(it) }
    Surface(
        modifier = modifier.heightIn(min = 34.dp),
        shape = RoundedCornerShape(LocalSecureLanDesignTokens.current.radius.small),
        color = LocalSecureLanDesignTokens.current.colors.surfaceLevel2,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = LocalSecureLanDesignTokens.current.spacing.sm, vertical = LocalSecureLanDesignTokens.current.spacing.xs),
            verticalArrangement = Arrangement.spacedBy(LocalSecureLanDesignTokens.current.spacing.xxs),
        ) {
            Text(
                text = fileName ?: fallbackSummary,
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = if (fileName == null) 0.58f else 0.86f),
                maxLines = 1,
            )
            if (fileName != null && trimmedPath != fileName) {
                Text(
                    text = trimmedPath,
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.54f),
                    maxLines = 1,
                )
            }
        }
    }
}
