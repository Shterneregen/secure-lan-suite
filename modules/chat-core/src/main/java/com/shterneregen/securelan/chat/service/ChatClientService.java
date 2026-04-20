package com.shterneregen.securelan.chat.service;

import com.shterneregen.securelan.common.model.rtc.RtcSignalEnvelope;

public interface ChatClientService {
    boolean connect(ChatClientConnectRequest request);
    void disconnect();
    void sendMessage(String text);
    void sendSignal(RtcSignalEnvelope signal);
    boolean isConnected();
}
