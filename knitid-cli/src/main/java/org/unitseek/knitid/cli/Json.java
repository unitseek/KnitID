package org.unitseek.knitid.cli;

import java.time.Instant;
import java.util.Map;

/**
 * Minimal JSON writer so the CLI has no runtime JSON dependency.
 *
 * @since 1.0.0
 */
final class Json {

    private Json() {
    }

    static String value(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof Instant instant) {
            return quote(instant.toString());
        }
        return quote(String.valueOf(value));
    }

    static String object(Map<String, ?> values) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            if (!first) {
                json.append(',');
            }
            first = false;
            json.append(quote(entry.getKey())).append(':').append(value(entry.getValue()));
        }
        return json.append('}').toString();
    }

    static String quote(String value) {
        StringBuilder json = new StringBuilder(value.length() + 2).append('"');
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '"' -> json.append("\\\"");
                case '\\' -> json.append("\\\\");
                case '\n' -> json.append("\\n");
                case '\r' -> json.append("\\r");
                case '\t' -> json.append("\\t");
                default -> {
                    if (ch < 0x20) {
                        json.append(String.format("\\u%04x", (int) ch));
                    }
                    else {
                        json.append(ch);
                    }
                }
            }
        }
        return json.append('"').toString();
    }
}
