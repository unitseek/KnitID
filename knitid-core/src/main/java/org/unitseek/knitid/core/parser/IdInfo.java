package org.unitseek.knitid.core.parser;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.unitseek.knitid.core.api.IdType;

/**
 * Parsed identifier information.
 */
public record IdInfo(
        IdType type,
        String raw,
        Instant timestamp,
        Integer version,
        Long nodeId,
        Long sequence,
        String businessType,
        Map<String, Object> attributes) {

    public IdInfo {
        java.util.Objects.requireNonNull(type, "type must not be null");
        java.util.Objects.requireNonNull(raw, "raw must not be null");
        if (attributes == null) {
            attributes = Map.of();
        }
        else {
            attributes = Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
        }
    }

    public static IdInfo of(IdType type, String raw) {
        return new IdInfo(type, raw, null, null, null, null, null, Map.of());
    }
}
