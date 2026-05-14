package com.shterneregen.securelan.common.net.transport;

import java.io.IOException;
import java.net.ServerSocket;

@FunctionalInterface
public interface ServerSocketFactory {
    ServerSocket open(int port) throws IOException;

    static ServerSocketFactory systemDefault() {
        return ServerSocket::new;
    }
}
