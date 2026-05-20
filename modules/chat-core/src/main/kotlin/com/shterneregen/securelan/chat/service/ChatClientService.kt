package com.shterneregen.securelan.chat.service

import com.shterneregen.securelan.common.model.rtc.RtcSignalEnvelope

interface ChatClientService {
    fun connect(request: ChatClientConnectRequest): Boolean
    fun disconnect()
    fun sendMessage(text: String?)
    fun sendSignal(signal: RtcSignalEnvelope?)
    fun isConnected(): Boolean
}
