package com.shterneregen.securelan.desktop.compose.state.shell

internal fun compactRoomNetworkStatus(localNetworkInfo: String): String? {
    val normalized = localNetworkInfo.trim()
    val addresses = when {
        normalized.startsWith("local network IP: ", ignoreCase = true) ->
            listOf(normalized.substringAfter(": ").trim())

        normalized.startsWith("local network IPs: ", ignoreCase = true) ->
            normalized.substringAfter(": ")
                .split(',')
                .map(String::trim)
                .filter(String::isNotEmpty)

        else -> emptyList()
    }
    return when (addresses.size) {
        0 -> null
        1 -> addresses.first()
        else -> "${addresses.first()} +${addresses.size - 1}"
    }
}
