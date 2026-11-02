package com.shterneregen.securelan.androidclient.ui.lantools

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.androidclient.R
import com.shterneregen.securelan.androidclient.ui.appStringResource as text

/** Secondary entry on Devices; opening tools leaves the workspace and its ViewModel alive. */
@Composable
internal fun NetworkUtilitiesEntry(open: () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(text(R.string.lan_utilities), style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = open, modifier = Modifier.testTag("lan-open")) { Text(text(R.string.lan_title)) }
        }
    }
}
