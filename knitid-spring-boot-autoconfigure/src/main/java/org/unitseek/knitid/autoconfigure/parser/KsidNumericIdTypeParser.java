package org.unitseek.knitid.autoconfigure.parser;

import java.util.Map;
import java.util.Optional;

import org.unitseek.knitid.core.api.IdType;
import org.unitseek.knitid.core.parser.IdInfo;
import org.unitseek.knitid.core.parser.IdTypeParser;

/**
 * Fallback parser for numeric KSID values. The bit layout is configuration
 * dependent, so only the type and raw value are exposed; register a custom
 * {@link IdTypeParser} bean to decode timestamp/node/sequence.
 */
final class KsidNumericIdTypeParser implements IdTypeParser {

    @Override
    public IdType type() {
        return IdType.KSID;
    }

    @Override
    public Optional<IdInfo> tryParse(String raw) {
        if (raw == null || raw.isEmpty() || raw.length() > 19) {
            return Optional.empty();
        }
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            if (ch < '0' || ch > '9') {
                return Optional.empty();
            }
        }
        try {
            long value = Long.parseLong(raw);
            if (value < 0) {
                return Optional.empty();
            }
            return Optional.of(new IdInfo(IdType.KSID, raw, null, null, null, null, null, Map.of()));
        }
        catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }
}
