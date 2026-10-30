package org.unitseek.knitid.core.parser;

import java.util.Optional;

import org.unitseek.knitid.core.exception.IdErrorCodes;
import org.unitseek.knitid.core.exception.IdParseException;

/**
 * Parser facade that delegates to all {@link IdTypeParser} implementations.
 *
 * @since 1.0.0
 */
public interface IdParser {

    /**
     * Attempts to parse an identifier without throwing.
     *
     * @param raw candidate identifier
     * @return parsed information, or empty if no parser recognizes it
     */
    Optional<IdInfo> tryParse(String raw);

    /**
     * Parses an identifier or throws.
     *
     * @param raw candidate identifier
     * @return parsed information
     * @throws org.unitseek.knitid.core.exception.IdParseException if no parser
     *         recognizes the value
     */
    default IdInfo parse(String raw) {
        return tryParse(raw).orElseThrow(() -> new IdParseException(
                IdErrorCodes.PARSE_FAILED, "Unrecognized id: " + mask(raw)));
    }

    // Never include raw input in an exception message.
    private static String mask(String raw) {
        if (raw == null) {
            return "null";
        }
        return "***";
    }
}
