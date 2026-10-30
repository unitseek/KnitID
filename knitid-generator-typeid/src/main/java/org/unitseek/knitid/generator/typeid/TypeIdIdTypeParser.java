package org.unitseek.knitid.generator.typeid;

import java.util.Map;
import java.util.Optional;

import org.unitseek.knitid.core.api.IdType;
import org.unitseek.knitid.core.parser.IdInfo;
import org.unitseek.knitid.core.parser.IdTypeParser;

/**
 * Parser for prefixed TypeID strings. Suffix-only TypeIDs are intentionally
 * not accepted by the built-in parser because they would collide with ULID.
 */
public final class TypeIdIdTypeParser implements IdTypeParser {

    @Override
    public IdType type() {
        return IdType.TYPEID;
    }

    @Override
    public Optional<IdInfo> tryParse(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        int separator = raw.indexOf('_');
        if (separator <= 0 || separator == raw.length() - 1) {
            return Optional.empty();
        }
        String prefix = raw.substring(0, separator);
        if (!TypeIdConfig.isValidPrefix(prefix)) {
            return Optional.empty();
        }
        String suffix = raw.substring(separator + 1);
        java.util.UUID uuid = TypeIdGenerator.decodeSuffix(suffix);
        if (uuid == null) {
            return Optional.empty();
        }
        return Optional.of(new IdInfo(IdType.TYPEID, raw, TypeIdGenerator.timestamp(uuid), 7,
                null, null, prefix, Map.of()));
    }
}
