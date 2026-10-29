package org.unitseek.knitid.autoconfigure.parser;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.unitseek.knitid.core.api.IdType;
import org.unitseek.knitid.core.parser.IdInfo;
import org.unitseek.knitid.core.parser.IdTypeParser;

/**
 * Parser for canonical UUIDv8 strings. UUIDv8 custom fields are
 * application-defined, so only version/variant metadata is exposed.
 */
final class Uuid8IdTypeParser implements IdTypeParser {

    @Override
    public IdType type() {
        return IdType.UUID8;
    }

    @Override
    public Optional<IdInfo> tryParse(String raw) {
        UUID uuid = Uuid7IdTypeParser.parseUuid(raw);
        if (uuid == null || uuid.version() != 8) {
            return Optional.empty();
        }
        return Optional.of(new IdInfo(IdType.UUID8, raw, null, 8, null, null, null, Map.of()));
    }
}
