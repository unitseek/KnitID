package org.unitseek.knitid.core.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Snapshot returned by generators that expose health details.
 */
public record IdGeneratorHealthSnapshot(HealthState state, String message, Map<String, Object> details) {

    public IdGeneratorHealthSnapshot {
        if (state == null) {
            state = HealthState.UNKNOWN;
        }
        if (details == null) {
            details = Map.of();
        }
        else {
            details = Collections.unmodifiableMap(new LinkedHashMap<>(details));
        }
    }
}
