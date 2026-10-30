package org.unitseek.knitid.core.util;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Locale;

/**
 * Parses Spring Boot style durations (for example {@code 5s}, {@code 1m},
 * {@code 2d}) as well as ISO-8601 durations.
 *
 * @since 1.0.0
 */
public final class DurationParser {

    private DurationParser() {
    }

    /**
     * Parses a duration.
     *
     * <p>Supported units are {@code ns}, {@code us}/{@code µs}, {@code ms},
     * {@code s}, {@code m}, {@code h} and {@code d}; ISO-8601 {@code P...}
     * values are also accepted.</p>
     *
     * @param value duration text
     * @return parsed duration
     * @throws IllegalArgumentException if the value is null, empty, malformed
     *         or out of range
     */
    public static Duration parse(String value) {
        if (value == null) {
            throw new IllegalArgumentException("duration must not be null");
        }
        String text = value.trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("duration must not be empty");
        }
        // Support both P... and signed +P.../-P... ISO-8601 forms.
        boolean signedIso = text.length() > 1
                && (text.charAt(0) == '-' || text.charAt(0) == '+')
                && Character.toUpperCase(text.charAt(1)) == 'P';
        if (text.startsWith("P") || text.startsWith("p") || signedIso) {
            try {
                return Duration.parse(text);
            }
            catch (java.time.format.DateTimeParseException ex) {
                throw new IllegalArgumentException("invalid ISO-8601 duration: " + value, ex);
            }
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
        BigDecimal number;
        try {
            number = new BigDecimal(text.substring(0, index));
        }
        catch (NumberFormatException ex) {
            throw new IllegalArgumentException("invalid duration number: " + value, ex);
        }
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
        try {
            return Duration.ofNanos(number.multiply(nanosPerUnit).longValueExact());
        }
        catch (ArithmeticException ex) {
            throw new IllegalArgumentException("duration out of range: " + value, ex);
        }
    }
}
