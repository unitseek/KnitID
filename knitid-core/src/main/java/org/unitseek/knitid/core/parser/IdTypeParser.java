package org.unitseek.knitid.core.parser;

import java.util.Optional;

import org.unitseek.knitid.core.api.IdType;

/**
 * Parser for one identifier type.
 */
public interface IdTypeParser {

    IdType type();

    Optional<IdInfo> tryParse(String raw);
}
