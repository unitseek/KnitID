package org.unitseek.knitid.generator.uuid8;

import java.util.Locale;

import org.unitseek.knitid.core.exception.IdConfigurationException;
import org.unitseek.knitid.core.exception.IdErrorCodes;
import org.unitseek.knitid.core.spi.GeneratorConfig;

/**
 * Immutable UUIDv8 configuration.
 *
 * @param preset fixed UUIDv8 layout
 * @param datacenterId 4-bit datacenter id, used only by
 *        {@link Uuid8Preset#TIME_NODE_RANDOM}
 * @param workerId 8-bit worker id, used only by
 *        {@link Uuid8Preset#TIME_NODE_RANDOM}
 * @since 1.0.0
 */
public record Uuid8Config(Uuid8Preset preset, int datacenterId, int workerId) {

    public Uuid8Config {
        if (preset == null) {
            throw invalid("uuid8.preset must not be null");
        }
        if (datacenterId < 0 || datacenterId > 15) {
            throw invalid("uuid8.datacenter-id must be between 0 and 15: " + datacenterId);
        }
        if (workerId < 0 || workerId > 255) {
            throw invalid("uuid8.worker-id must be between 0 and 255: " + workerId);
        }
    }

    /**
     * Returns the default configuration: {@link Uuid8Preset#TIME_RANDOM},
     * datacenter 0 and worker 0.
     *
     * @return default UUIDv8 configuration
     */
    public static Uuid8Config defaults() {
        return new Uuid8Config(Uuid8Preset.TIME_RANDOM, 0, 0);
    }

    /**
     * Parses UUIDv8 configuration from a generator configuration.
     *
     * @param config generator configuration
     * @return parsed configuration
     * @throws IdConfigurationException if the preset or an id is invalid
     */
    public static Uuid8Config from(GeneratorConfig config) {
        try {
            return new Uuid8Config(
                    parseEnum(Uuid8Preset.class, config.getString("uuid8.preset", Uuid8Preset.TIME_RANDOM.name()),
                            "uuid8.preset"),
                    config.getInt("uuid8.datacenter-id", 0),
                    config.getInt("uuid8.worker-id", 0));
        }
        catch (NumberFormatException ex) {
            throw invalid("Invalid numeric UUID8 configuration: " + ex.getMessage());
        }
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
