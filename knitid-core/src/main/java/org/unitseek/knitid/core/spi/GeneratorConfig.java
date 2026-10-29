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
        return get(key).map(value -> value instanceof Boolean bool
                ? bool
                : Boolean.parseBoolean(String.valueOf(value))).orElse(defaultValue);
    }

    default int getInt(String key, int defaultValue) {
        return get(key).map(value -> value instanceof Number number
                ? number.intValue()
                : Integer.parseInt(String.valueOf(value))).orElse(defaultValue);
    }

    default long getLong(String key, long defaultValue) {
        return get(key).map(value -> value instanceof Number number
                ? number.longValue()
                : Long.parseLong(String.valueOf(value))).orElse(defaultValue);
    }

    default Duration getDuration(String key, Duration defaultValue) {
        return get(key).map(value -> value instanceof Duration duration
                ? duration
                : org.unitseek.knitid.core.util.DurationParser.parse(String.valueOf(value)))
                .orElse(defaultValue);
    }
}
