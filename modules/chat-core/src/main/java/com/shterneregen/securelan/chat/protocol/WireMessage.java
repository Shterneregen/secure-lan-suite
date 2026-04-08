package com.shterneregen.securelan.chat.protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record WireMessage(
        WireMessageType type,
        String sender,
        String payload
) {
    private static final char SEPARATOR = '|';
    private static final char ESCAPE = '\\';

    public WireMessage {
        Objects.requireNonNull(type, "type must not be null");
        sender = sender == null ? "" : sender;
        payload = payload == null ? "" : payload;
    }

    public String serialize() {
        return escape(type.name()) + SEPARATOR + escape(sender) + SEPARATOR + escape(payload);
    }

    public static WireMessage deserialize(String line) {
        Objects.requireNonNull(line, "line must not be null");

        List<String> parts = splitEscaped(line, SEPARATOR);
        if (parts.size() != 3) {
            throw new IllegalArgumentException("Invalid wire message format: expected 3 parts, got " + parts.size());
        }

        String typeRaw = unescape(parts.get(0));
        String senderRaw = unescape(parts.get(1));
        String payloadRaw = unescape(parts.get(2));

        return new WireMessage(
                WireMessageType.valueOf(typeRaw),
                senderRaw,
                payloadRaw
        );
    }

    private static List<String> splitEscaped(String input, char separator) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean escaping = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (escaping) {
                current.append(c);
                escaping = false;
                continue;
            }

            if (c == ESCAPE) {
                escaping = true;
                continue;
            }

            if (c == separator) {
                parts.add(current.toString());
                current.setLength(0);
                continue;
            }

            current.append(c);
        }

        if (escaping) {
            current.append(ESCAPE);
        }

        parts.add(current.toString());
        return parts;
    }

    private static String escape(String value) {
        StringBuilder out = new StringBuilder(value.length());

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

            if (c == ESCAPE) {
                out.append("\\\\");
            } else if (c == SEPARATOR) {
                out.append("\\|");
            } else if (c == '\n') {
                out.append("\\n");
            } else if (c == '\r') {
                out.append("\\r");
            } else {
                out.append(c);
            }
        }

        return out.toString();
    }

    private static String unescape(String value) {
        StringBuilder out = new StringBuilder(value.length());
        boolean escaping = false;

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

            if (escaping) {
                switch (c) {
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case '\\' -> out.append('\\');
                    case '|' -> out.append('|');
                    default -> {
                        out.append('\\');
                        out.append(c);
                    }
                }
                escaping = false;
                continue;
            }

            if (c == ESCAPE) {
                escaping = true;
            } else {
                out.append(c);
            }
        }

        if (escaping) {
            out.append(ESCAPE);
        }

        return out.toString();
    }
}