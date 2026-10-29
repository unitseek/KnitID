package org.unitseek.knitid.autoconfigure.parser;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.unitseek.knitid.core.parser.IdInfo;
import org.unitseek.knitid.core.parser.IdParser;
import org.unitseek.knitid.core.parser.IdTypeParser;

/**
 * Composite parser that tries user-provided parsers first, then built-in
 * format parsers.
 */
public final class SimpleIdParser implements IdParser {

    private static final System.Logger LOGGER = System.getLogger(SimpleIdParser.class.getName());

    private final List<IdTypeParser> parsers;

    public SimpleIdParser(List<IdTypeParser> parsers) {
        this.parsers = List.copyOf(Objects.requireNonNull(parsers, "parsers must not be null"));
    }

    @Override
    public Optional<IdInfo> tryParse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        for (IdTypeParser parser : parsers) {
            try {
                Optional<IdInfo> info = parser.tryParse(raw);
                if (info != null && info.isPresent()) {
                    return info;
                }
            }
            catch (RuntimeException ex) {
                // A user parser must not break the whole parsing chain, but keep the failure observable.
                LOGGER.log(System.Logger.Level.DEBUG,
                        "IdTypeParser " + parser.type() + " failed; falling through", ex);
            }
        }
        return Optional.empty();
    }
}
