package com.shterneregen.securelan.filetransfer.quickshare.impl;

import com.shterneregen.securelan.filetransfer.quickshare.QuickShareCreateRequest;
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareEvent;
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareEventPublisher;
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareServerConfig;
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareService;
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareSnapshot;
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareStatus;
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareType;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class DefaultQuickShareService implements QuickShareService {
    private static final int PORT_RETRY_COUNT = 20;

    private final QuickShareEventPublisher eventPublisher;
    private final Clock clock;
    private final Object lifecycleLock = new Object();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong threadSequence = new AtomicLong(0);
    private final Map<String, SharedItem> items = new ConcurrentHashMap<>();

    private volatile HttpServer server;
    private volatile ExecutorService executor;
    private volatile ScheduledExecutorService expiryExecutor;
    private volatile int activePort;
    private volatile List<String> advertisedHosts = List.of("127.0.0.1");

    public DefaultQuickShareService(QuickShareEventPublisher eventPublisher) {
        this(eventPublisher, Clock.systemUTC());
    }

    DefaultQuickShareService(QuickShareEventPublisher eventPublisher, Clock clock) {
        this.eventPublisher = eventPublisher == null ? QuickShareEventPublisher.noOp() : eventPublisher;
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public void start(QuickShareServerConfig config) throws IOException {
        Objects.requireNonNull(config, "config must not be null");
        synchronized (lifecycleLock) {
            if (running.get()) {
                return;
            }
            HttpServer createdServer = createHttpServer(config.port());
            ExecutorService createdExecutor = Executors.newCachedThreadPool(runnable -> {
                Thread thread = new Thread(runnable, "securelan-quick-share-http-" + threadSequence.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            });
            ScheduledExecutorService createdExpiryExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "securelan-quick-share-expiry");
                thread.setDaemon(true);
                return thread;
            });
            createdServer.createContext("/", this::handleHttpExchange);
            createdServer.setExecutor(createdExecutor);

            this.server = createdServer;
            this.executor = createdExecutor;
            this.expiryExecutor = createdExpiryExecutor;
            this.activePort = createdServer.getAddress().getPort();
            this.advertisedHosts = resolveAdvertisedHosts(config.advertisedHosts());
            running.set(true);
            createdServer.start();
            createdExpiryExecutor.scheduleAtFixedRate(this::expireInactiveShares, 1, 1, TimeUnit.SECONDS);
            eventPublisher.publish(new QuickShareEvent("", null, "Quick-share server started on port " + activePort, ""));
        }
    }

    private HttpServer createHttpServer(int requestedPort) throws IOException {
        if (requestedPort == 0) {
            return HttpServer.create(new InetSocketAddress("0.0.0.0", 0), 32);
        }
        IOException lastError = null;
        for (int offset = 0; offset <= PORT_RETRY_COUNT; offset++) {
            int candidate = requestedPort + offset;
            if (candidate > 65535) {
                break;
            }
            try {
                return HttpServer.create(new InetSocketAddress("0.0.0.0", candidate), 32);
            } catch (IOException ex) {
                lastError = ex;
            }
        }
        throw lastError == null ? new IOException("Unable to bind quick-share server") : lastError;
    }

    @Override
    public void stop() {
        synchronized (lifecycleLock) {
            running.set(false);
            HttpServer activeServer = server;
            if (activeServer != null) {
                activeServer.stop(0);
            }
            ExecutorService activeExecutor = executor;
            if (activeExecutor != null) {
                activeExecutor.shutdownNow();
            }
            ScheduledExecutorService activeExpiryExecutor = expiryExecutor;
            if (activeExpiryExecutor != null) {
                activeExpiryExecutor.shutdownNow();
            }
            server = null;
            executor = null;
            expiryExecutor = null;
            activePort = 0;
            advertisedHosts = List.of("127.0.0.1");
            eventPublisher.publish(new QuickShareEvent("", null, "Quick-share server stopped", ""));
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public int port() {
        return activePort;
    }

    @Override
    public QuickShareSnapshot share(QuickShareCreateRequest request) throws IOException {
        Objects.requireNonNull(request, "request must not be null");
        if (!running.get()) {
            throw new IllegalStateException("Quick-share server is not running");
        }
        String id;
        synchronized (lifecycleLock) {
            id = uniqueId(request.displayName());
            SharedItem item = createSharedItem(id, request, clock.instant(), urlsFor(id));
            items.put(id, item);
            QuickShareSnapshot snapshot = item.snapshot();
            eventPublisher.publish(new QuickShareEvent(id, snapshot, "Quick-share created: " + snapshot.displayName(), ""));
            return snapshot;
        }
    }

    private String uniqueId(String displayName) {
        String base = sanitizeId(displayName);
        String candidate = base;
        int index = 2;
        while (items.containsKey(candidate)) {
            candidate = base + "-" + index;
            index++;
        }
        return candidate;
    }

    private String sanitizeId(String value) {
        String sanitized = value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        if (sanitized.isBlank()) {
            sanitized = "share";
        }
        return sanitized.length() > 48 ? sanitized.substring(0, 48).replaceAll("-+$", "") : sanitized;
    }

    @Override
    public Optional<QuickShareSnapshot> findShare(String id) {
        SharedItem item = items.get(id);
        if (item == null) {
            return Optional.empty();
        }
        item.expireIfNeeded(clock.instant());
        return Optional.of(item.snapshot());
    }

    @Override
    public List<QuickShareSnapshot> shares() {
        Instant now = clock.instant();
        return items.values().stream()
                .peek(item -> item.expireIfNeeded(now))
                .map(SharedItem::snapshot)
                .sorted(Comparator.comparing(QuickShareSnapshot::createdAt).reversed())
                .toList();
    }

    @Override
    public boolean stopShare(String id) {
        SharedItem item = items.get(id);
        if (item == null) {
            return false;
        }
        QuickShareSnapshot snapshot = item.stop();
        eventPublisher.publish(new QuickShareEvent(id, snapshot, "Quick-share stopped: " + snapshot.displayName(), ""));
        return true;
    }

    @Override
    public List<String> landingUrls() {
        if (!running.get()) {
            return List.of();
        }
        return advertisedHosts.stream()
                .map(host -> "http://" + host + ":" + activePort + "/")
                .toList();
    }

    private List<String> urlsFor(String id) {
        return advertisedHosts.stream()
                .map(host -> "http://" + host + ":" + activePort + "/" + id)
                .toList();
    }

    private void expireInactiveShares() {
        Instant now = clock.instant();
        for (SharedItem item : items.values()) {
            QuickShareSnapshot snapshot = item.expireIfNeeded(now);
            if (snapshot != null) {
                eventPublisher.publish(new QuickShareEvent(snapshot.id(), snapshot, "Quick-share expired: " + snapshot.displayName(), ""));
            }
        }
    }

    private void handleHttpExchange(HttpExchange exchange) throws IOException {
        try (exchange) {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendText(exchange, 405, "Method Not Allowed", "text/plain; charset=utf-8");
                return;
            }
            String path = exchange.getRequestURI().getPath();
            if ("/".equals(path) || path.isBlank()) {
                sendText(exchange, 200, indexHtml(), "text/html; charset=utf-8");
                return;
            }
            String[] parts = path.split("/");
            ParsedSharePath sharePath = parseSharePath(parts);
            if (sharePath == null) {
                sendText(exchange, 404, notFoundHtml(), "text/html; charset=utf-8");
                return;
            }
            if (sharePath.action().isBlank()) {
                handleItemPage(exchange, sharePath.id());
            } else if ("download".equals(sharePath.action())) {
                handleDownload(exchange, sharePath.id());
            } else if ("text".equals(sharePath.action())) {
                handleText(exchange, sharePath.id());
            } else {
                sendText(exchange, 404, notFoundHtml(), "text/html; charset=utf-8");
            }
        }
    }

    private ParsedSharePath parseSharePath(String[] parts) {
        if (parts.length == 2 && !parts[1].isBlank()) {
            return new ParsedSharePath(URLDecoder.decode(parts[1], StandardCharsets.UTF_8), "");
        }
        if (parts.length == 3 && "s".equals(parts[1])) {
            return new ParsedSharePath(URLDecoder.decode(parts[2], StandardCharsets.UTF_8), "");
        }
        if (parts.length == 3 && !parts[1].isBlank()) {
            return new ParsedSharePath(URLDecoder.decode(parts[1], StandardCharsets.UTF_8), parts[2]);
        }
        if (parts.length == 4 && "s".equals(parts[1])) {
            return new ParsedSharePath(URLDecoder.decode(parts[2], StandardCharsets.UTF_8), parts[3]);
        }
        return null;
    }

    private void handleItemPage(HttpExchange exchange, String id) throws IOException {
        SharedItem item = items.get(id);
        if (item == null) {
            sendText(exchange, 404, notFoundHtml(), "text/html; charset=utf-8");
            return;
        }
        if (item.type == QuickShareType.TEXT) {
            AccessResult access = item.registerAccess(clock.instant());
            if (!access.allowed()) {
                sendText(exchange, 410, unavailableHtml(access.snapshot()), "text/html; charset=utf-8");
                return;
            }
            eventPublisher.publish(new QuickShareEvent(id, access.snapshot(), "Text share opened: " + access.snapshot().displayName(), exchange.getRemoteAddress().toString()));
            sendText(exchange, 200, textItemHtml(access.snapshot(), item.text), "text/html; charset=utf-8");
            return;
        }
        QuickShareSnapshot snapshot = item.snapshotAfterExpiry(clock.instant());
        if (!snapshot.active()) {
            sendText(exchange, 410, unavailableHtml(snapshot), "text/html; charset=utf-8");
            return;
        }
        sendText(exchange, 200, fileItemHtml(snapshot), "text/html; charset=utf-8");
    }

    private void handleText(HttpExchange exchange, String id) throws IOException {
        SharedItem item = items.get(id);
        if (item == null || item.type != QuickShareType.TEXT) {
            sendText(exchange, 404, "Not Found", "text/plain; charset=utf-8");
            return;
        }
        AccessResult access = item.registerAccess(clock.instant());
        if (!access.allowed()) {
            sendText(exchange, 410, "Share is no longer available", "text/plain; charset=utf-8");
            return;
        }
        eventPublisher.publish(new QuickShareEvent(id, access.snapshot(), "Text share opened: " + access.snapshot().displayName(), exchange.getRemoteAddress().toString()));
        sendText(exchange, 200, item.text, "text/plain; charset=utf-8");
    }

    private void handleDownload(HttpExchange exchange, String id) throws IOException {
        SharedItem item = items.get(id);
        if (item == null || item.type != QuickShareType.FILE) {
            sendText(exchange, 404, "Not Found", "text/plain; charset=utf-8");
            return;
        }
        AccessResult access = item.registerAccess(clock.instant());
        if (!access.allowed()) {
            sendText(exchange, 410, "Share is no longer available", "text/plain; charset=utf-8");
            return;
        }
        eventPublisher.publish(new QuickShareEvent(id, access.snapshot(), "File downloaded: " + access.snapshot().displayName(), exchange.getRemoteAddress().toString()));

        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "application/octet-stream");
        headers.set("Content-Disposition", "attachment; filename=\"" + contentDispositionFileName(item.fileName) + "\"");
        headers.set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(200, item.fileSize);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            Files.copy(item.file, outputStream);
        }
    }

    private String indexHtml() {
        List<QuickShareSnapshot> activeShares = shares().stream()
                .filter(QuickShareSnapshot::active)
                .toList();
        StringBuilder body = new StringBuilder(baseHtmlStart("SecureLanSuite quick shares"));
        body.append("<h1>SecureLanSuite quick shares</h1>");
        body.append("<p class='warn'>No authorization is required. Anyone on this LAN who opens this page can access active shares.</p>");
        if (activeShares.isEmpty()) {
            body.append("<p>No active shares.</p>");
        } else {
            body.append("<ul>");
            for (QuickShareSnapshot snapshot : activeShares) {
                body.append("<li><a href='/").append(escapeHtml(snapshot.id())).append("'>")
                        .append(escapeHtml(snapshot.displayName()))
                        .append("</a> <span>")
                        .append(escapeHtml(snapshot.type().name().toLowerCase(Locale.ROOT)))
                        .append(" — ").append(snapshot.accessCount()).append("/").append(snapshot.accessLimit())
                        .append(" accesses</span></li>");
            }
            body.append("</ul>");
        }
        body.append(baseHtmlEnd());
        return body.toString();
    }

    private String fileItemHtml(QuickShareSnapshot snapshot) {
        return baseHtmlStart(snapshot.displayName())
                + "<h1>" + escapeHtml(snapshot.displayName()) + "</h1>"
                + "<p>File: <strong>" + escapeHtml(snapshot.fileName()) + "</strong></p>"
                + "<p>Size: " + snapshot.fileSize() + " bytes</p>"
                + "<p>Accesses: " + snapshot.accessCount() + " / " + snapshot.accessLimit() + "</p>"
                + "<a class='button' href='/" + escapeHtml(snapshot.id()) + "/download'>Download file</a>"
                + "<p><a href='/'>Back to active shares</a></p>"
                + baseHtmlEnd();
    }

    private String textItemHtml(QuickShareSnapshot snapshot, String text) {
        return baseHtmlStart(snapshot.displayName())
                + "<h1>" + escapeHtml(snapshot.displayName()) + "</h1>"
                + "<p>Accesses: " + snapshot.accessCount() + " / " + snapshot.accessLimit() + "</p>"
                + "<textarea id='sharedText' readonly>" + escapeHtml(text) + "</textarea>"
                + "<p><button class='button' onclick='navigator.clipboard.writeText(document.getElementById(\"sharedText\").value)'>Copy text</button></p>"
                + "<p><a href='/'>Back to active shares</a></p>"
                + baseHtmlEnd();
    }

    private String unavailableHtml(QuickShareSnapshot snapshot) {
        String status = snapshot == null ? "unavailable" : snapshot.status().name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return baseHtmlStart("Share unavailable")
                + "<h1>Share unavailable</h1>"
                + "<p>This share is " + escapeHtml(status) + ".</p>"
                + "<p><a href='/'>Back to active shares</a></p>"
                + baseHtmlEnd();
    }

    private String notFoundHtml() {
        return baseHtmlStart("Not found")
                + "<h1>Not found</h1><p>The requested share does not exist.</p><p><a href='/'>Back to active shares</a></p>"
                + baseHtmlEnd();
    }

    private String baseHtmlStart(String title) {
        return "<!doctype html><html><head><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'>"
                + "<title>" + escapeHtml(title) + "</title>"
                + "<style>body{font-family:system-ui,sans-serif;max-width:820px;margin:32px auto;padding:0 16px;background:#0f172a;color:#e2e8f0}"
                + "a{color:#93c5fd}.button,button{display:inline-block;background:#2563eb;color:white;border:0;border-radius:8px;padding:10px 14px;text-decoration:none;cursor:pointer}"
                + "textarea{width:100%;min-height:220px;border-radius:8px;padding:12px;background:#020617;color:#e2e8f0;border:1px solid #334155}"
                + ".warn{background:#451a03;color:#fed7aa;padding:10px 12px;border-radius:8px}li{margin:10px 0}</style></head><body>";
    }

    private String baseHtmlEnd() {
        return "</body></html>";
    }

    private void sendText(HttpExchange exchange, int status, String value, String contentType) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", contentType);
        headers.set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    private List<String> resolveAdvertisedHosts(List<String> configuredHosts) {
        if (!configuredHosts.isEmpty()) {
            return configuredHosts;
        }
        try {
            List<String> localHosts = resolveLocalLanIps();
            if (!localHosts.isEmpty()) {
                return localHosts;
            }
        } catch (Exception ignored) {
            // Fallback below keeps localhost links available even when adapter inspection fails.
        }
        return List.of("127.0.0.1");
    }

    private SharedItem createSharedItem(String id, QuickShareCreateRequest request, Instant now, List<String> urls) throws IOException {
        Path file = request.file();
        String fileName = request.type() == QuickShareType.FILE ? file.getFileName().toString() : "";
        long fileSize = request.type() == QuickShareType.FILE ? Files.size(file) : request.text().getBytes(StandardCharsets.UTF_8).length;
        return new SharedItem(
                id,
                request.type(),
                request.displayName(),
                file,
                fileName,
                fileSize,
                request.text(),
                now,
                now.plus(request.expiresAfter()),
                request.accessLimit(),
                urls
        );
    }

    private List<String> resolveLocalLanIps() throws IOException {
        List<String> siteLocalAddresses = new ArrayList<>();
        List<String> otherNonLoopbackAddresses = new ArrayList<>();
        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface networkInterface = networkInterfaces.nextElement();
            if (!networkInterface.isUp() || networkInterface.isLoopback() || networkInterface.isVirtual()) {
                continue;
            }
            Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
            while (inetAddresses.hasMoreElements()) {
                InetAddress inetAddress = inetAddresses.nextElement();
                if (!(inetAddress instanceof Inet4Address) || inetAddress.isLoopbackAddress()) {
                    continue;
                }
                if (inetAddress.isSiteLocalAddress()) {
                    siteLocalAddresses.add(inetAddress.getHostAddress());
                } else {
                    otherNonLoopbackAddresses.add(inetAddress.getHostAddress());
                }
            }
        }
        List<String> result = !siteLocalAddresses.isEmpty() ? siteLocalAddresses : otherNonLoopbackAddresses;
        result.sort(Comparator.naturalOrder());
        return result.stream().distinct().toList();
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String contentDispositionFileName(String value) {
        return value.replace("\\", "_").replace("\"", "_").replace("\r", "_").replace("\n", "_");
    }

    private record ParsedSharePath(String id, String action) {
    }

    private record AccessResult(boolean allowed, QuickShareSnapshot snapshot) {
    }

    private final class SharedItem {
        private final String id;
        private final QuickShareType type;
        private final String displayName;
        private final Path file;
        private final String fileName;
        private final long fileSize;
        private final String text;
        private final Instant createdAt;
        private final Instant expiresAt;
        private final int accessLimit;
        private final List<String> urls;

        private int accessCount;
        private QuickShareStatus status = QuickShareStatus.ACTIVE;

        private SharedItem(String id,
                           QuickShareType type,
                           String displayName,
                           Path file,
                           String fileName,
                           long fileSize,
                           String text,
                           Instant createdAt,
                           Instant expiresAt,
                           int accessLimit,
                           List<String> urls) {
            this.id = id;
            this.type = type;
            this.displayName = displayName;
            this.file = file;
            this.fileName = fileName;
            this.fileSize = fileSize;
            this.text = text;
            this.createdAt = createdAt;
            this.expiresAt = expiresAt;
            this.accessLimit = accessLimit;
            this.urls = urls;
        }

        private synchronized AccessResult registerAccess(Instant now) {
            expireIfNeeded(now);
            if (status != QuickShareStatus.ACTIVE) {
                return new AccessResult(false, snapshot());
            }
            if (accessCount >= accessLimit) {
                status = QuickShareStatus.LIMIT_REACHED;
                return new AccessResult(false, snapshot());
            }
            accessCount++;
            if (accessCount >= accessLimit) {
                status = QuickShareStatus.LIMIT_REACHED;
            }
            return new AccessResult(true, snapshot());
        }

        private synchronized QuickShareSnapshot snapshotAfterExpiry(Instant now) {
            expireIfNeeded(now);
            return snapshot();
        }

        private synchronized QuickShareSnapshot expireIfNeeded(Instant now) {
            if (status == QuickShareStatus.ACTIVE && !now.isBefore(expiresAt)) {
                status = QuickShareStatus.EXPIRED;
                return snapshot();
            }
            return null;
        }

        private synchronized QuickShareSnapshot stop() {
            status = QuickShareStatus.STOPPED;
            return snapshot();
        }

        private synchronized QuickShareSnapshot snapshot() {
            return new QuickShareSnapshot(id, type, displayName, fileName, fileSize, createdAt, expiresAt, accessLimit, accessCount, status, urls);
        }
    }
}
