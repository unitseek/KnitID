package org.unitseek.knitid.generator.ulid;

import java.util.Locale;

import org.unitseek.knitid.core.exception.IdConfigurationException;
import org.unitseek.knitid.core.exception.IdErrorCodes;
import org.unitseek.knitid.core.spi.GeneratorConfig;

/**
 * Immutable ULID configuration.
 */
public record UlidConfig(boolean monotonic, UlidMonotonicOverflow monotonicOverflow, UlidRandomSource randomSource) {

    public UlidConfig {
        if (monotonicOverflow == null) {
            throw invalid("ulid.monotonic-overflow must not be null");
        }
        if (randomSource == null) {
            throw invalid("ulid.random-source must not be null");
        }
    }

    public static UlidConfig defaults() {
        return new UlidConfig(true, UlidMonotonicOverflow.WAIT, UlidRandomSource.PERFORMANCE);
    }

    public static UlidConfig from(GeneratorConfig config) {
        return new UlidConfig(
                config.getBoolean("ulid.monotonic", true),
                parseEnum(UlidMonotonicOverflow.class,
                        config.getString("ulid.monotonic-overflow", UlidMonotonicOverflow.WAIT.name()),
                        "ulid.monotonic-overflow"),
                parseEnum(UlidRandomSource.class,
                        config.getString("ulid.random-source", UlidRandomSource.PERFORMANCE.name()),
                        "ulid.random-source"));
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String value, String key) {
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        }
        catch (IllegalArgumentException ex) {
            throw invalid("Invalid " + key + ": " + value);
        }
    }

    private static IdConfigurationException invalid(String message) {
        return new IdConfigurationException(IdErrorCodes.CONFIG_INVALID, message);
    }
}
