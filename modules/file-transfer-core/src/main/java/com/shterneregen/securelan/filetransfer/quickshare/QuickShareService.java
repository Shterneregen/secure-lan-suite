package com.shterneregen.securelan.filetransfer.quickshare;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface QuickShareService extends AutoCloseable {
    void start(QuickShareServerConfig config) throws IOException;

    void stop();

    boolean isRunning();

    int port();

    QuickShareSnapshot share(QuickShareCreateRequest request) throws IOException;

    Optional<QuickShareSnapshot> findShare(String id);

    List<QuickShareSnapshot> shares();

    boolean stopShare(String id);

    List<String> landingUrls();

    @Override
    default void close() {
        stop();
    }
}
