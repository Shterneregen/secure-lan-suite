package com.shterneregen.securelan.androidclient.protocol

typealias WireMessage = com.shterneregen.securelan.chat.protocol.WireMessage
typealias WireMessageType = com.shterneregen.securelan.chat.protocol.WireMessageType

val WireMessage.type: WireMessageType
    get() = type()

val WireMessage.sender: String
    get() = sender()

val WireMessage.payload: String
    get() = payload()
