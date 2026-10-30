package org.unitseek.knitid.generator.ksuid;

import java.util.Map;
import java.util.Optional;

import org.unitseek.knitid.core.api.IdType;
import org.unitseek.knitid.core.parser.IdInfo;
import org.unitseek.knitid.core.parser.IdTypeParser;

/**
 * Parser for 27-character KSUID strings.
 */
public final class KsuidIdTypeParser implements IdTypeParser {

    @Override
    public IdType type() {
        return IdType.KSUID;
    }

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
