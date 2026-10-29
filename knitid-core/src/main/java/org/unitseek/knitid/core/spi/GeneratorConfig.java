package org.unitseek.knitid.core.spi;

import java.time.Duration;
import java.util.Optional;

import org.unitseek.knitid.core.api.IdType;

/**
 * Read-only configuration view passed to an {@link IdGeneratorFactory}.
 *
 * <p>Keys are flattened using dot notation, for example
 * {@code uuid7.monotonic}.</p>
 */
public interface GeneratorConfig {

    String name();

    IdType type();

    Optional<Object> get(String key);

    default String getString(String key, String defaultValue) {
        return get(key).map(String::valueOf).orElse(defaultValue);
    }

    default boolean getBoolean(String key, boolean defaultValue) {
        return get(key).map(value -> {
            if (value instanceof Boolean bool) {
                return bool;
            }
            String text = String.valueOf(value).trim();
            if ("true".equalsIgnoreCase(text)) {
                return true;
            }
            if ("false".equalsIgnoreCase(text)) {
                return false;
            }
            throw new IllegalArgumentException("Invalid boolean for " + key + ": " + value);
        }).orElse(defaultValue);
    }

    default int getInt(String key, int defaultValue) {
        return get(key).map(value -> {
            try {
                if (value instanceof Number number) {
                    return Math.toIntExact(number.longValue());
                }
                return Integer.parseInt(String.valueOf(value).trim());
            }
            catch (ArithmeticException | NumberFormatException ex) {
                throw new IllegalArgumentException("Invalid int for " + key + ": " + value, ex);
            }
        }).orElse(defaultValue);
    }

    default long getLong(String key, long defaultValue) {
        return get(key).map(value -> {
            try {
                if (value instanceof Number number) {
                    return number.longValue();
                }
                return Long.parseLong(String.valueOf(value).trim());
            }
            catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Invalid long for " + key + ": " + value, ex);
            }
        }).orElse(defaultValue);
    }

    default Duration getDuration(String key, Duration defaultValue) {
        return get(key).map(value -> value instanceof Duration duration
                ? duration
                : org.unitseek.knitid.core.util.DurationParser.parse(String.valueOf(value)))
                .orElse(defaultValue);
    }
}
