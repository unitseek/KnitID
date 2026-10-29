package org.unitseek.knitid.core.parser;

import java.util.Optional;

import org.unitseek.knitid.core.exception.IdErrorCodes;
import org.unitseek.knitid.core.exception.IdParseException;

/**
 * Parser facade that delegates to all {@link IdTypeParser} implementations.
 */
public interface IdParser {

    Optional<IdInfo> tryParse(String raw);

    default IdInfo parse(String raw) {
        return tryParse(raw).orElseThrow(() -> new IdParseException(
                IdErrorCodes.PARSE_FAILED, "Unrecognized id: " + mask(raw)));
    }

    private static String mask(String raw) {
        if (raw == null) {
            return "null";
        }
        return "***";
    }
}
