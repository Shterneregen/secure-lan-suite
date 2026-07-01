package com.shterneregen.securelan.desktop.compose.state.peer

import com.shterneregen.securelan.common.net.NetworkConstants
import com.shterneregen.securelan.desktop.compose.state.connection.ComposeConnectionJoinTarget

public fun ComposePeerListItem.toJoinTarget(): ComposeConnectionJoinTarget? {
    if (!online || !discovered) return null
    val host = listMeta.substringAfter("@", missingDelimiterValue = "").substringBefore(" · ").ifBlank { return null }
    val chatPort = NetworkConstants.DEFAULT_CHAT_PORT
    val filePort = filePort.takeIf { it > 0 } ?: NetworkConstants.DEFAULT_FILE_TRANSFER_PORT
    return ComposeConnectionJoinTarget(nickname, host, chatPort.toString(), filePort.toString())
}
