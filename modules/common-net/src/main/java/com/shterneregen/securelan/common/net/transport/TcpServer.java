package com.shterneregen.securelan.common.net.transport;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public final class TcpServer implements AutoCloseable {
    private final String threadNamePrefix;
    private final ServerSocketFactory serverSocketFactory;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private volatile ServerSocket serverSocket;
    private volatile ExecutorService clientExecutor;
    private volatile Thread acceptThread;

    public TcpServer(String threadNamePrefix) {
        this(threadNamePrefix, ServerSocketFactory.systemDefault());
    }

    public TcpServer(String threadNamePrefix, ServerSocketFactory serverSocketFactory) {
        this.threadNamePrefix = Objects.requireNonNull(threadNamePrefix, "threadNamePrefix");
        this.serverSocketFactory = Objects.requireNonNull(serverSocketFactory, "serverSocketFactory");
    }

    public void start(int port, SocketHandler socketHandler, ErrorHandler errorHandler) throws IOException {
        Objects.requireNonNull(socketHandler, "socketHandler");
        Objects.requireNonNull(errorHandler, "errorHandler");
        if (!running.compareAndSet(false, true)) {
            return;
        }
        ServerSocket createdSocket = null;
        ExecutorService createdExecutor = null;
        try {
            createdSocket = serverSocketFactory.open(port);
            createdExecutor = Executors.newCachedThreadPool(runnable -> {
                Thread thread = new Thread(runnable, threadNamePrefix + "-client");
                thread.setDaemon(true);
                return thread;
            });
            serverSocket = createdSocket;
            clientExecutor = createdExecutor;
            ServerSocket activeSocket = createdSocket;
            ExecutorService activeExecutor = createdExecutor;
            acceptThread = new Thread(
                    () -> acceptLoop(activeSocket, activeExecutor, socketHandler, errorHandler),
                    threadNamePrefix + "-accept"
            );
            acceptThread.setDaemon(true);
            acceptThread.start();
        } catch (IOException | RuntimeException e) {
            running.set(false);
            stop(createdSocket, createdExecutor);
            serverSocket = null;
            clientExecutor = null;
            acceptThread = null;
            throw e;
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    @Override
    public void close() {
        running.set(false);
        stop(serverSocket, clientExecutor);
        serverSocket = null;
        clientExecutor = null;
        acceptThread = null;
    }

    private void acceptLoop(ServerSocket activeSocket,
                            ExecutorService activeExecutor,
                            SocketHandler socketHandler,
                            ErrorHandler errorHandler) {
        while (isActive(activeSocket, activeExecutor)) {
            try {
                Socket socket = activeSocket.accept();
                try {
                    activeExecutor.submit(() -> socketHandler.handle(socket));
                } catch (RejectedExecutionException e) {
                    SocketClose.closeQuietly(socket);
                    if (isActive(activeSocket, activeExecutor)) {
                        errorHandler.onError("Unable to schedule TCP client handler", e);
                    }
                }
            } catch (SocketException e) {
                if (isActive(activeSocket, activeExecutor)) {
                    errorHandler.onError("TCP server socket error", e);
                }
            } catch (IOException e) {
                if (isActive(activeSocket, activeExecutor)) {
                    errorHandler.onError("Unable to accept TCP client", e);
                }
            }
        }
    }

    private boolean isActive(ServerSocket activeSocket, ExecutorService activeExecutor) {
        return running.get()
                && serverSocket == activeSocket
                && clientExecutor == activeExecutor
                && !activeSocket.isClosed()
                && !activeExecutor.isShutdown();
    }

    private void stop(ServerSocket socket, ExecutorService executor) {
        SocketClose.closeQuietly(socket);
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @FunctionalInterface
    public interface SocketHandler {
        void handle(Socket socket);
    }

    @FunctionalInterface
    public interface ErrorHandler {
        void onError(String message, Throwable cause);
    }
}
