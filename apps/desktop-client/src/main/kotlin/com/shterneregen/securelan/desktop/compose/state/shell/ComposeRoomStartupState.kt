package com.shterneregen.securelan.desktop.compose.state.shell

import com.shterneregen.securelan.common.net.NetworkConstants
import com.shterneregen.securelan.desktop.compose.state.connection.ComposeConnectionHubMode
import com.shterneregen.securelan.desktop.compose.state.connection.ComposeConnectionHubState
import com.shterneregen.securelan.desktop.compose.state.connection.ComposeConnectionJoinTarget
import com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerListItem

data class ComposeRoomStartupState(
    val connectionHubState: ComposeConnectionHubState,
    val nearbyRooms: List<ComposePeerListItem> = emptyList(),
    val selectedJoinTarget: ComposeConnectionJoinTarget? = null,
) {
    val title: String = "Start a secure room"
    val subtitle: String = "Choose a nearby room, or create one for trusted people on this LAN."
    val primaryHero: String = "Room startup"
    val primaryActionLabel: String = if (selectedJoinTarget != null) "Join selected room" else "Create secure room"
    val primaryMode: ComposeConnectionHubMode = if (selectedJoinTarget != null) ComposeConnectionHubMode.JOIN else ComposeConnectionHubMode.HOST
    val secondaryActionLabel: String = if (selectedJoinTarget != null) "Create a new room instead" else "Join a hidden room"
    val nearbyTitle: String = "Nearby secure rooms"
    val nearbyEmptyTitle: String = "No nearby rooms yet"
    val nearbyEmptyDetail: String = "Create a room now, or join a hidden room from Advanced connection."
    val showHostControls: Boolean = selectedJoinTarget == null
    val showJoinControls: Boolean = selectedJoinTarget != null
    val relevantControlSummary: String = if (showJoinControls) {
        "Join controls only"
    } else {
        "Room creation controls only"
    }
    val advancedActionLabel: String = connectionHubState.advancedSettingsTitle
    val advancedHiddenByDefault: Boolean = true
    val avoidsDualHostJoinLayout: Boolean = true
    val hasSinglePrimaryAction: Boolean = true
    val nearbyRoomsFirst: Boolean = true
    val reducesFirstScreenCognitiveLoad: Boolean =
        advancedHiddenByDefault && avoidsDualHostJoinLayout && hasSinglePrimaryAction && nearbyRoomsFirst
    val visibleRooms: List<ComposeRoomStartupRoom> = nearbyRooms
        .filter { it.online && it.discovered }
        .map { peer ->
            ComposeRoomStartupRoom(
                name = peer.nickname,
                host = peer.listMeta.substringAfter("@", missingDelimiterValue = "").substringBefore(" · ").ifBlank { "nearby" },
                chatPortText = NetworkConstants.DEFAULT_CHAT_PORT.toString(),
                filePortText = peer.filePort.takeIf { it > 0 }?.toString() ?: NetworkConstants.DEFAULT_FILE_TRANSFER_PORT.toString(),
                capabilitySummary = peer.listMeta.substringAfter(" · ", missingDelimiterValue = "Secure room"),
                selected = selectedJoinTarget?.nickname == peer.nickname,
            )
        }
    val hasNearbyRooms: Boolean = visibleRooms.isNotEmpty()
    val roomSelectionSummary: String = selectedJoinTarget?.let { "Selected ${it.nickname}" }
        ?: if (hasNearbyRooms) "Choose a nearby room to join" else nearbyEmptyTitle
}
