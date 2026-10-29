package org.unitseek.knitid.autoconfigure.parser;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.unitseek.knitid.core.api.IdType;
import org.unitseek.knitid.core.parser.IdInfo;
import org.unitseek.knitid.core.parser.IdTypeParser;

/**
 * Parser for canonical UUIDv7 strings.
 */
final class Uuid7IdTypeParser implements IdTypeParser {

    @Override
    public IdType type() {
        return IdType.UUID7;
    }

    @Override
    public Optional<IdInfo> tryParse(String raw) {
        UUID uuid = parseUuid(raw);
        if (uuid == null || uuid.version() != 7 || uuid.variant() != 2) {
            return Optional.empty();
        }
        long timestamp = uuid.getMostSignificantBits() >>> 16;
        return Optional.of(new IdInfo(IdType.UUID7, raw, Instant.ofEpochMilli(timestamp), 7,
                null, null, null, Map.of()));
    }

    static UUID parseUuid(String raw) {
        if (raw == null || raw.length() != 36) {
            return null;
        }
        try {
            UUID uuid = UUID.fromString(raw);
            // UUID.fromString accepts non-canonical group lengths; require canonical text.
            return uuid.toString().equalsIgnoreCase(raw) ? uuid : null;
        }
        catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
