package com.shterneregen.securelan.androidclient.ui.lantools

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.androidclient.R
import com.shterneregen.securelan.androidclient.ui.IconTile
import com.shterneregen.securelan.androidclient.ui.appStringResource as text

/** Secondary entry leaves the workspace and its ViewModel alive. */
@Composable
internal fun NetworkUtilitiesEntry(open: () -> Unit) {
    Surface(onClick = open, modifier = Modifier.fillMaxWidth().testTag("lan-open"),
        shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconTile(Icons.Outlined.Build)
            Column(Modifier.weight(1f)) {
                Text(text(R.string.lan_utilities), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text(R.string.lan_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text(R.string.lan_utilities_hint), style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Outlined.ChevronRight, null)
        }
    }
}
