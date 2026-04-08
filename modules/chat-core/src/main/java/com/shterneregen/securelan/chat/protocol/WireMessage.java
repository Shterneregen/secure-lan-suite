package com.shterneregen.securelan.chat.protocol;

import java.util.Objects;

public record WireMessage(
        WireMessageType type,
        String sender,
        String payload
) {
    public WireMessage {
        Objects.requireNonNull(type, "type must not be null");
        sender = sender == null ? "" : sender;
        payload = payload == null ? "" : payload;
    }

    public String serialize() {
        return escape(type.name()) + "|" + escape(sender) + "|" + escape(payload);
    }

    public static WireMessage deserialize(String line) {
        String[] parts = splitEscaped(line, '|', 3);
        return new WireMessage(
                WireMessageType.valueOf(unescape(parts[0])),
                unescape(parts[1]),
                unescape(parts[2])
        );
    }

    private static String[] splitEscaped(String input, char separator, int expectedParts) {
        String[] result = new String[expectedParts];
        StringBuilder current = new StringBuilder();
        boolean escaping = false;
        int index = 0;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (escaping) {
                current.append(c);
                escaping = false;
            } else if (c == '\\') {
                escaping = true;
            } else if (c == separator && index < expectedParts - 1) {
                result[index++] = current.toString();
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        result[index] = current.toString();
        for (int i = 0; i < result.length; i++) {
            if (result[i] == null) {
                result[i] = "";
            }
        }
        return result;
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("|", "\\|").replace("\n", "\\n");
    }

    private static String unescape(String value) {
        StringBuilder out = new StringBuilder();
        boolean escaping = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (escaping) {
                if (c == 'n') {
                    out.append('\n');
                } else {
                    out.append(c);
                }
                escaping = false;
            } else if (c == '\\') {
                escaping = true;
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
