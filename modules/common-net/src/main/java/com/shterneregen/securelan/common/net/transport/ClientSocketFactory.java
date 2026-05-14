package com.shterneregen.securelan.common.net.transport;

import java.io.IOException;
import java.net.Socket;

@FunctionalInterface
public interface ClientSocketFactory {
    Socket connect(TransportEndpoint endpoint) throws IOException;

    static ClientSocketFactory systemDefault() {
        return endpoint -> new Socket(endpoint.host(), endpoint.port());
    }
}
