package com.shterneregen.securelan.common.net

/**
 * Standard network port constants used across SecureLanSuite services and applications.
 */
class NetworkConstants private constructor() {
    companion object {
        /**
         * Default TCP port used by the chat server and client services.
         */
        const val DEFAULT_CHAT_PORT: Int = 5050

        /**
         * Default TCP port used for binary file transfer sessions.
         */
        const val DEFAULT_FILE_TRANSFER_PORT: Int = 5051

        /**
         * Default UDP port used for LAN peer discovery broadcasts.
         */
        const val DEFAULT_DISCOVERY_PORT: Int = 5052

        /**
         * Default TCP port used for QuickShare file transfer sessions.
         */
        const val DEFAULT_QUICK_SHARE_PORT: Int = 5053
    }
}
