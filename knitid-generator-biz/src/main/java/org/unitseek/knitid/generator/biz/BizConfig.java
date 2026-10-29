package org.unitseek.knitid.generator.biz;

import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Pattern;

import org.unitseek.knitid.core.exception.IdConfigurationException;
import org.unitseek.knitid.core.exception.IdErrorCodes;
import org.unitseek.knitid.core.spi.GeneratorConfig;

/**
 * Immutable business-ID configuration.
 */
public record BizConfig(
        String template,
        String prefix,
        ZoneId zoneId,
        Long nodeId,
        String sequenceProviderName,
        SequenceReset reset,
        CheckAlgorithm checkAlgorithm) {

    public static final int MAX_TEMPLATE_LENGTH = 128;

    public static final int MAX_PREFIX_LENGTH = 16;

    private static final Pattern PREFIX_PATTERN = Pattern.compile("[A-Za-z0-9_-]{1," + MAX_PREFIX_LENGTH + "}");

    public BizConfig {
        if (template == null || template.isBlank()) {
            throw invalid("biz.template must not be blank");
        }
        if (template.length() > MAX_TEMPLATE_LENGTH) {
            throw invalid("biz.template must be <= " + MAX_TEMPLATE_LENGTH + " characters");
        }
        if (prefix != null && !PREFIX_PATTERN.matcher(prefix).matches()) {
            throw invalid("biz.prefix must match " + PREFIX_PATTERN.pattern() + ": " + prefix);
        }
        if (zoneId == null) {
            throw invalid("biz.zone-id is required");
        }
        if (nodeId != null && nodeId < 0) {
            throw invalid("biz.node-id must be >= 0");
        }
        if (sequenceProviderName == null || sequenceProviderName.isBlank()) {
            throw invalid("biz.sequence-provider must not be blank");
        }
        if (reset == null) {
            throw invalid("biz.reset must not be null");
        }
        if (checkAlgorithm == null) {
            throw invalid("biz.check-algorithm must not be null");
        }
    }

    public static BizConfig from(GeneratorConfig config) {
        String template = config.getString("biz.template", null);
        String prefix = config.getString("biz.prefix", null);
        String zone = config.getString("biz.zone-id", null);
        Long nodeId = config.get("biz.node-id")
                .map(value -> {
                    try {
                        return value instanceof Number number ? number.longValue() : Long.parseLong(String.valueOf(value).trim());
                    }
                    catch (NumberFormatException ex) {
                        throw invalid("Invalid biz.node-id: " + value);
                    }
                })
                .orElse(null);
        String provider = config.getString("biz.sequence-provider", "memory");
        SequenceReset reset = parseEnum(SequenceReset.class,
                config.getString("biz.reset", SequenceReset.DAILY.name()), "biz.reset");
        CheckAlgorithm algorithm = parseEnum(CheckAlgorithm.class,
                config.getString("biz.check-algorithm", CheckAlgorithm.MOD37_36.name()), "biz.check-algorithm");
        ZoneId zoneId;
        try {
            zoneId = zone == null || zone.isBlank() ? null : ZoneId.of(zone.trim());
        }
        catch (java.time.DateTimeException ex) {
            throw invalid("Invalid biz.zone-id: " + zone);
        }
        return new BizConfig(template, prefix, zoneId, nodeId, provider, reset, algorithm);
    }

    static boolean isValidPrefix(String value) {
        return value == null || PREFIX_PATTERN.matcher(value).matches();
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
