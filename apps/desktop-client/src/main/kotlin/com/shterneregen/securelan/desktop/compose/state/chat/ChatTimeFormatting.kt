package com.shterneregen.securelan.desktop.compose.state.chat

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val ComposeChatTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

internal fun formatComposeChatTimestamp(timestamp: Instant): String = ComposeChatTimeFormatter.format(timestamp)
