package org.unitseek.knitid.autoconfigure.parser;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.unitseek.knitid.core.api.IdType;
import org.unitseek.knitid.core.parser.IdInfo;
import org.unitseek.knitid.core.parser.IdTypeParser;

/**
 * Parser for 26-character Crockford Base32 ULIDs.
 */
final class UlidIdTypeParser implements IdTypeParser {

    private static final String ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";

    @Override
    public IdType type() {
        return IdType.ULID;
    }

    @Override
    public Optional<IdInfo> tryParse(String raw) {
        if (raw == null || raw.length() != 26) {
            return Optional.empty();
        }
        if (!raw.chars().allMatch(ch -> ch < 128)) {
            return Optional.empty();
        }
        long value = 0;
        for (int i = 0; i < 10; i++) {
            int digit = ALPHABET.indexOf(Character.toUpperCase(raw.charAt(i)));
            if (digit < 0) {
                return Optional.empty();
            }
            value = (value << 5) | digit;
        }
        if ((value & ~0xFFFF_FFFF_FFFFL) != 0) {
            return Optional.empty();
        }
        for (int i = 10; i < raw.length(); i++) {
            if (ALPHABET.indexOf(Character.toUpperCase(raw.charAt(i))) < 0) {
                return Optional.empty();
            }
        }
        return Optional.of(new IdInfo(IdType.ULID, raw, Instant.ofEpochMilli(value), null,
                null, null, null, Map.of()));
    }
}
