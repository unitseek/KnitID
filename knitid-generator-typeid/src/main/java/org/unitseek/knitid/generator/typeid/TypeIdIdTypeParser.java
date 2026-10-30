package org.unitseek.knitid.generator.typeid;

import java.util.Map;
import java.util.Optional;

import org.unitseek.knitid.core.api.IdType;
import org.unitseek.knitid.core.parser.IdInfo;
import org.unitseek.knitid.core.parser.IdTypeParser;

/**
 * Parser for prefixed TypeID strings.
 *
 * <p>Suffix-only TypeIDs are intentionally not accepted by the built-in parser
 * because they would collide with ULID. Prefixes containing underscores are
 * supported; the separator is resolved from the right because the suffix is
 * always 26 characters.</p>
 *
 * @since 1.0.0
 */
public final class TypeIdIdTypeParser implements IdTypeParser {

    /**
     * {@inheritDoc}
     */
    @Override
    public IdType type() {
        return IdType.TYPEID;
    }

    /**
     * Parses a prefixed TypeID and extracts its prefix and UUIDv7 timestamp.
     *
     * @param raw candidate identifier
     * @return parsed information, or empty if the value is not a prefixed TypeID
     */
    @Override
    public Optional<IdInfo> tryParse(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        int separator = raw.lastIndexOf('_');
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
