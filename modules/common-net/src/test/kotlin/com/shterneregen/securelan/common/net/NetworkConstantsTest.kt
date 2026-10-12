package com.shterneregen.securelan.common.net

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NetworkConstantsTest {
    @Test
    fun shouldDefineExpectedDefaultPorts() {
        assertEquals(5050, NetworkConstants.DEFAULT_CHAT_PORT)
        assertEquals(5051, NetworkConstants.DEFAULT_FILE_TRANSFER_PORT)
        assertEquals(5052, NetworkConstants.DEFAULT_DISCOVERY_PORT)
        assertEquals(5053, NetworkConstants.DEFAULT_QUICK_SHARE_PORT)
    }
}
