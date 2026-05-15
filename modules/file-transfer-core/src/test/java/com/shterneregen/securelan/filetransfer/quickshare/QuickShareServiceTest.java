package com.shterneregen.securelan.filetransfer.quickshare;

import com.shterneregen.securelan.filetransfer.quickshare.impl.DefaultQuickShareService;
import org.junit.jupiter.api.Test;

import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuickShareServiceTest {
    @Test
    void shouldServeTextShareAndEnforceAccessLimit() throws Exception {
        List<String> events = new CopyOnWriteArrayList<>();
        DefaultQuickShareService service = new DefaultQuickShareService(event -> events.add(event.message()));
        service.start(new QuickShareServerConfig(findAvailablePort(), List.of("127.0.0.1")));
        try {
            QuickShareSnapshot snapshot = service.share(QuickShareCreateRequest.text("hello from LAN", "Greeting", Duration.ofMinutes(5), 1));
            HttpClient client = HttpClient.newHttpClient();

            HttpResponse<String> first = client.send(HttpRequest.newBuilder(URI.create(snapshot.primaryUrl())).GET().build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, first.statusCode());
            assertTrue(first.body().contains("hello from LAN"));

            HttpResponse<String> second = client.send(HttpRequest.newBuilder(URI.create(snapshot.primaryUrl())).GET().build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(410, second.statusCode());
            assertEquals(QuickShareStatus.LIMIT_REACHED, service.findShare(snapshot.id()).orElseThrow().status());
            assertTrue(events.stream().anyMatch(message -> message.contains("Text share opened")));
        } finally {
            service.stop();
        }
    }

    @Test
    void shouldServeFileDownloadAndKeepPageViewFree() throws Exception {
        Path sourceFile = Files.createTempFile("quick-share", ".txt");
        Files.writeString(sourceFile, "download payload");

        DefaultQuickShareService service = new DefaultQuickShareService(QuickShareEventPublisher.noOp());
        service.start(new QuickShareServerConfig(findAvailablePort(), List.of("127.0.0.1")));
        try {
            QuickShareSnapshot snapshot = service.share(QuickShareCreateRequest.file(sourceFile, "Payload", Duration.ofMinutes(5), 1));
            HttpClient client = HttpClient.newHttpClient();

            HttpResponse<String> page = client.send(HttpRequest.newBuilder(URI.create(snapshot.primaryUrl())).GET().build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, page.statusCode());
            assertEquals(0, service.findShare(snapshot.id()).orElseThrow().accessCount());

            HttpResponse<String> download = client.send(HttpRequest.newBuilder(URI.create(snapshot.primaryUrl() + "/download")).GET().build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, download.statusCode());
            assertEquals("download payload", download.body());
            assertEquals(QuickShareStatus.LIMIT_REACHED, service.findShare(snapshot.id()).orElseThrow().status());
        } finally {
            service.stop();
        }
    }

    @Test
    void shouldStopShareManually() throws Exception {
        DefaultQuickShareService service = new DefaultQuickShareService(QuickShareEventPublisher.noOp());
        service.start(new QuickShareServerConfig(findAvailablePort(), List.of("127.0.0.1")));
        try {
            QuickShareSnapshot snapshot = service.share(QuickShareCreateRequest.text("manual stop", "Manual", Duration.ofMinutes(5), 2));
            assertTrue(service.stopShare(snapshot.id()));
            assertFalse(service.findShare(snapshot.id()).orElseThrow().active());
            assertEquals(QuickShareStatus.STOPPED, service.findShare(snapshot.id()).orElseThrow().status());
        } finally {
            service.stop();
        }
    }

    private static int findAvailablePort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
