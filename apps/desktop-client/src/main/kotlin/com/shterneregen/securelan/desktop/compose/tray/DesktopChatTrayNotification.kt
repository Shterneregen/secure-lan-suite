package com.shterneregen.securelan.desktop.compose.tray

/** A UI-neutral chat notification that can be delivered by the platform tray. */
data class DesktopChatTrayNotification(
    val id: Long,
    val title: String,
    val message: String,
) {
    companion object {
        private const val MAX_SENDER_LENGTH = 48
        private const val MAX_MESSAGE_LENGTH = 180

        fun create(id: Long, senderNickname: String?, text: String?): DesktopChatTrayNotification {
            val sender = senderNickname
                ?.trim()
                ?.replace(Regex("\\s+"), " ")
                ?.takeIf(String::isNotEmpty)
                ?.ellipsize(MAX_SENDER_LENGTH)
                ?: "Unknown sender"
            val message = text
                ?.trim()
                ?.replace(Regex("\\s+"), " ")
                ?.takeIf(String::isNotEmpty)
                ?.ellipsize(MAX_MESSAGE_LENGTH)
                ?: "New message"
            return DesktopChatTrayNotification(
                id = id,
                title = "Message from $sender",
                message = message,
            )
        }

        private fun String.ellipsize(maxLength: Int): String =
            if (length <= maxLength) this else take(maxLength - 1).trimEnd() + "…"
    }
}

fun shouldPublishChatTrayNotification(
    systemLikeMessage: Boolean,
    localSender: Boolean,
    notificationsEnabled: Boolean,
    messageNotificationsEnabled: Boolean,
): Boolean = !systemLikeMessage &&
    !localSender &&
    notificationsEnabled &&
    messageNotificationsEnabled
