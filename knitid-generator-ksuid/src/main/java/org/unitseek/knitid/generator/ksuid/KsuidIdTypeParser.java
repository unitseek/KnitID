package org.unitseek.knitid.generator.ksuid;

import java.util.Map;
import java.util.Optional;

import org.unitseek.knitid.core.api.IdType;
import org.unitseek.knitid.core.parser.IdInfo;
import org.unitseek.knitid.core.parser.IdTypeParser;

/**
 * Parser for 27-character KSUID strings.
 *
 * <p>KSUID has no header or check digit, so any syntactically valid 27-character
 * Base62 value is accepted. Callers must not treat a successful parse as proof
 * that the value originated from a trusted KSUID generator.</p>
 *
 * @since 1.0.0
 */
public final class KsuidIdTypeParser implements IdTypeParser {

    /**
     * {@inheritDoc}
     */
    @Override
    public IdType type() {
        return IdType.KSUID;
    }

    /**
     * Parses a KSUID and extracts its timestamp.
     *
     * @param raw candidate identifier
     * @return parsed information, or empty if the value is not a valid KSUID
     */
    @Override
    public Optional<IdInfo> tryParse(String raw) {
        byte[] bytes = KsuidGenerator.decode(raw);
        if (bytes == null) {
            return Optional.empty();
        }
        return Optional.of(new IdInfo(IdType.KSUID, raw, KsuidGenerator.timestamp(bytes),
                null, null, null, null, Map.of()));
    }
}
