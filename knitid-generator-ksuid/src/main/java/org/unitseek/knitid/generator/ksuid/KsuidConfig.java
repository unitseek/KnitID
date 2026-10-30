package org.unitseek.knitid.generator.ksuid;

import java.util.Locale;

import org.unitseek.knitid.core.exception.IdConfigurationException;
import org.unitseek.knitid.core.exception.IdErrorCodes;
import org.unitseek.knitid.core.spi.GeneratorConfig;

/**
 * KSUID configuration.
 */
public record KsuidConfig(KsuidRandomSource randomSource) {

    public KsuidConfig {
        if (randomSource == null) {
            throw invalid("ksuid.random-source must not be null");
        }
    }

    public static KsuidConfig defaults() {
        return new KsuidConfig(KsuidRandomSource.SECURE);
    }

    public static KsuidConfig from(GeneratorConfig config) {
        String raw = config.getString("ksuid.random-source", KsuidRandomSource.SECURE.name());
        try {
            return new KsuidConfig(Enum.valueOf(KsuidRandomSource.class, raw.trim().toUpperCase(Locale.ROOT)));
        }
        catch (IllegalArgumentException ex) {
            throw invalid("Invalid ksuid.random-source: " + raw + " (expected SECURE or PERFORMANCE)");
        }
    }

    private static IdConfigurationException invalid(String message) {
        return new IdConfigurationException(IdErrorCodes.CONFIG_INVALID, message);
    }
}
