package org.unitseek.knitid.core.parser;

import java.util.Optional;

import org.unitseek.knitid.core.api.IdType;

/**
 * Parser for one identifier type.
 *
 * @since 1.0.0
 */
public interface IdTypeParser {

    /**
     * Returns the type this parser recognizes.
     *
     * @return identifier type
     */
    IdType type();

    /**
     * Attempts to parse one identifier.
     *
     * @param raw candidate identifier
     * @return parsed information, or empty if the value does not match
     */
    Optional<IdInfo> tryParse(String raw);
}
