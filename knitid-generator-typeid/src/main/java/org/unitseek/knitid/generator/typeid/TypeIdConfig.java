package org.unitseek.knitid.generator.typeid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.unitseek.knitid.core.exception.IdConfigurationException;
import org.unitseek.knitid.core.exception.IdErrorCodes;
import org.unitseek.knitid.core.spi.GeneratorConfig;

/**
 * TypeID configuration. Prefixes follow TypeID Specification v0.3.0.
 */
public record TypeIdConfig(Set<String> prefixes, String defaultPrefix, boolean allowEmptyPrefix) {

    private static final Pattern PREFIX_PATTERN = Pattern.compile("^([a-z]([a-z_]{0,61}[a-z])?)?$");

    public TypeIdConfig {
        if (prefixes == null || prefixes.isEmpty()) {
            throw invalid("typeid.prefixes must not be empty");
        }
        Set<String> validated = new LinkedHashSet<>();
        for (String prefix : prefixes) {
            if (!isValidPrefix(prefix)) {
                throw invalid("Invalid typeid prefix: " + prefix);
            }
            if (prefix.isEmpty() && !allowEmptyPrefix) {
                throw invalid("Empty typeid prefix requires typeid.allow-empty-prefix=true");
            }
            validated.add(prefix);
        }
        prefixes = Collections.unmodifiableSet(validated);
        if (defaultPrefix == null || !prefixes.contains(defaultPrefix)) {
            throw invalid("typeid.default-prefix must be one of " + prefixes);
        }
    }

    public static TypeIdConfig from(GeneratorConfig config) {
        Object raw = config.get("typeid.prefixes").orElse(null);
        List<String> values = new ArrayList<>();
        if (raw instanceof Collection<?> collection) {
            collection.forEach(value -> values.add(String.valueOf(value)));
        }
        else if (raw instanceof java.util.Map<?, ?> map) {
            map.entrySet().stream()
                    .sorted(java.util.Map.Entry.comparingByKey(java.util.Comparator.comparing(String::valueOf)))
                    .forEach(entry -> values.add(String.valueOf(entry.getValue())));
        }
        else if (raw instanceof Object[] array) {
            for (Object value : array) {
                values.add(String.valueOf(value));
            }
        }
        else if (raw != null) {
            for (String part : String.valueOf(raw).split(",")) {
                values.add(part.trim());
            }
        }
        if (values.isEmpty()) {
            throw invalid("typeid.prefixes must be configured");
        }
        Set<String> prefixes = new LinkedHashSet<>();
        for (String value : values) {
            String prefix = value.trim();
            if (!isValidPrefix(prefix)) {
                throw invalid("Invalid typeid prefix: " + value);
            }
            prefixes.add(prefix);
        }
        boolean allowEmpty = config.getBoolean("typeid.allow-empty-prefix", false);
        if (prefixes.contains("") && !allowEmpty) {
            throw invalid("Empty typeid prefix requires typeid.allow-empty-prefix=true");
        }
        String defaultPrefix = config.getString("typeid.default-prefix", null);
        if (defaultPrefix == null) {
            defaultPrefix = prefixes.iterator().next();
        }
        return new TypeIdConfig(prefixes, defaultPrefix, allowEmpty);
    }

    public void validateForGeneration(String prefix) {
        if (prefix == null || !isValidPrefix(prefix) || !prefixes.contains(prefix)
                || (prefix.isEmpty() && !allowEmptyPrefix)) {
            throw invalid("TypeID prefix is not registered: " + prefix + " (registered: " + prefixes + ")");
        }
    }

    public static boolean isValidPrefix(String prefix) {
        return prefix != null && PREFIX_PATTERN.matcher(prefix).matches();
    }

    private static IdConfigurationException invalid(String message) {
        return new IdConfigurationException(IdErrorCodes.CONFIG_INVALID, message);
    }
}
