package org.unitseek.knitid.core.util;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Locale;

/**
 * Parses Spring Boot style durations (for example {@code 5s}, {@code 1m},
 * {@code 2d}) as well as ISO-8601 durations.
 */
public final class DurationParser {

    private DurationParser() {
    }

    public static Duration parse(String value) {
        if (value == null) {
            throw new IllegalArgumentException("duration must not be null");
        }
        String text = value.trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("duration must not be empty");
        }
        if (text.startsWith("P") || text.startsWith("p")) {
            return Duration.parse(text.toUpperCase(Locale.ROOT));
        }
        int index = 0;
        while (index < text.length()) {
            char ch = text.charAt(index);
            if (Character.isDigit(ch) || ch == '.' || ch == '-' || ch == '+') {
                index++;
            }
            else {
                break;
            }
        }
        if (index == 0 || index == text.length()) {
            throw new IllegalArgumentException("invalid duration: " + value);
        }
        BigDecimal number = new BigDecimal(text.substring(0, index));
        String unit = text.substring(index).trim().toLowerCase(Locale.ROOT);
        BigDecimal nanosPerUnit = switch (unit) {
            case "ns" -> BigDecimal.ONE;
            case "us", "µs" -> BigDecimal.valueOf(1_000L);
            case "ms" -> BigDecimal.valueOf(1_000_000L);
            case "s" -> BigDecimal.valueOf(1_000_000_000L);
            case "m" -> BigDecimal.valueOf(60_000_000_000L);
            case "h" -> BigDecimal.valueOf(3_600_000_000_000L);
            case "d" -> BigDecimal.valueOf(86_400_000_000_000L);
            default -> throw new IllegalArgumentException("unsupported duration unit: " + unit);
        };
        return Duration.ofNanos(number.multiply(nanosPerUnit).longValueExact());
    }
}
