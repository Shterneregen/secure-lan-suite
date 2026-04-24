package com.shterneregen.securelan.webrtc.runtime;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

public final class RtcFileLogger {
    private static final Object LOCK = new Object();
    private static final String LOG_FILE_PROPERTY = "securelan.rtc.logFile";
    private static final Path DEFAULT_LOG_FILE = Path.of("logs", "rtc.log");

    private RtcFileLogger() {
    }

    public static void warn(String message) {
        write("WARN", message, null);
    }

    public static void error(String message) {
        write("ERROR", message, null);
    }

    public static void error(String message, Throwable error) {
        write("ERROR", message, error);
    }

    private static void write(String level, String message, Throwable error) {
        synchronized (LOCK) {
            try {
                Path logFile = logFile();
                Path parent = logFile.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }

                Files.writeString(
                        logFile,
                        format(level, message, error),
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND
                );
            } catch (Throwable ignored) {
                // Logging must never break realtime media flow.
            }
        }
    }

    private static Path logFile() {
        String configuredPath = System.getProperty(LOG_FILE_PROPERTY);
        if (configuredPath == null || configuredPath.isBlank()) {
            return DEFAULT_LOG_FILE;
        }
        return Path.of(configuredPath.trim());
    }

    private static String format(String level, String message, Throwable error) {
        StringBuilder builder = new StringBuilder()
                .append(Instant.now())
                .append(' ')
                .append(level)
                .append(" [rtc] ")
                .append(message == null ? "" : message)
                .append(System.lineSeparator());

        if (error != null) {
            StringWriter buffer = new StringWriter();
            error.printStackTrace(new PrintWriter(buffer));
            builder.append(buffer);
        }

        return builder.toString();
    }
}
