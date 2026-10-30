package org.unitseek.knitid.generator.ksuid;

import java.util.Locale;

import org.unitseek.knitid.core.exception.IdConfigurationException;
import org.unitseek.knitid.core.exception.IdErrorCodes;
import org.unitseek.knitid.core.spi.GeneratorConfig;

/**
 * KSUID configuration.
 *
 * @param randomSource random source; defaults to {@link KsuidRandomSource#SECURE}
 * @since 1.0.0
 */
public record KsuidConfig(KsuidRandomSource randomSource) {

    public KsuidConfig {
        if (randomSource == null) {
            throw invalid("ksuid.random-source must not be null");
        }
    }

    /**
     * Returns the default configuration ({@link KsuidRandomSource#SECURE}).
     *
     * @return default KSUID configuration
     */
    public static KsuidConfig defaults() {
        return new KsuidConfig(KsuidRandomSource.SECURE);
    }

    /**
     * Parses KSUID configuration from a generator configuration.
     *
     * @param config generator configuration
     * @return parsed configuration
     * @throws IdConfigurationException if {@code ksuid.random-source} is
     *         unknown, blank or not a supported enum value
     */
    public static KsuidConfig from(GeneratorConfig config) {
        validateKeys(config);
        String raw = config.getString("ksuid.random-source", KsuidRandomSource.SECURE.name());
        try {
            return new KsuidConfig(Enum.valueOf(KsuidRandomSource.class, raw.trim().toUpperCase(Locale.ROOT)));
        }
        catch (IllegalArgumentException ex) {
            throw invalid("Invalid ksuid.random-source: " + raw + " (expected SECURE or PERFORMANCE)");
        }
    }

    private static void validateKeys(GeneratorConfig config) {
        for (String key : config.keys()) {
            if (!"type".equals(key) && !"enabled".equals(key) && !"ksuid.random-source".equals(key)) {
                throw invalid("Unknown ksuid property: " + key);
            }
        }
    }

    private static IdConfigurationException invalid(String message) {
        return new IdConfigurationException(IdErrorCodes.CONFIG_INVALID, message);
    }
}
