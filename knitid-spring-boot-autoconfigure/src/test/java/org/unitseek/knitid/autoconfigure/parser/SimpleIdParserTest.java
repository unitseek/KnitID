package org.unitseek.knitid.autoconfigure.parser;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.unitseek.knitid.core.api.IdType;
import org.unitseek.knitid.core.exception.IdParseException;
import org.unitseek.knitid.core.parser.IdInfo;
import org.unitseek.knitid.core.parser.IdParser;
import org.unitseek.knitid.core.parser.IdTypeParser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SimpleIdParserTest {

    private final IdParser parser = new SimpleIdParser(List.of(
            new Uuid7IdTypeParser(),
            new Uuid8IdTypeParser(),
            new UlidIdTypeParser(),
            new KsidNumericIdTypeParser()));

    @Test
    void parsesUuid7WithTimestamp() {
        IdInfo info = parser.parse("017f22e2-79b0-7cc3-98c4-dc0c0c07398f");
        assertThat(info.type()).isEqualTo(IdType.UUID7);
        assertThat(info.version()).isEqualTo(7);
        assertThat(info.timestamp()).isEqualTo(Instant.ofEpochMilli(1645557742000L));
    }

    @Test
    void parsesUuid8VersionOnly() {
        IdInfo info = parser.parse("00000000-0000-8000-8000-000000000000");
        assertThat(info.type()).isEqualTo(IdType.UUID8);
        assertThat(info.version()).isEqualTo(8);
        assertThat(info.timestamp()).isNull();
    }

    @Test
    void parsesUlidTimestamp() {
        IdInfo info = parser.parse("01ARZ3NDEKTSV4RRFFQ69G5FAV");
        assertThat(info.type()).isEqualTo(IdType.ULID);
        assertThat(info.timestamp()).isEqualTo(Instant.ofEpochMilli(1469922850259L));
    }

    @Test
    void parsesNumericKsidAsFallback() {
        IdInfo info = parser.parse("1234567890123456789");
        assertThat(info.type()).isEqualTo(IdType.KSID);
        assertThat(info.timestamp()).isNull();
    }

    @Test
    void rejectsUnknownAndNonUuidVersions() {
        assertThat(parser.tryParse("not-an-id")).isEmpty();
        assertThat(parser.tryParse("919108f7-52d1-4320-9bac-f847db4148a8")).isEmpty();
        assertThat(parser.tryParse("01ARZ3NDEKTSV4RRFFQ69G5FAI")).isEmpty();
        assertThatThrownBy(() -> parser.parse("not-an-id")).isInstanceOf(IdParseException.class);
    }

    @Test
    void userParserTakesPrecedenceOverBuiltIns() {
        IdTypeParser custom = new IdTypeParser() {
            @Override
            public IdType type() {
                return IdType.BIZ;
            }

            @Override
            public Optional<IdInfo> tryParse(String raw) {
                return raw.startsWith("ORD-")
                        ? Optional.of(new IdInfo(IdType.BIZ, raw, null, null, null, null, "ORDER", Map.of()))
                        : Optional.empty();
            }
        };
        IdParser composite = new SimpleIdParser(List.of(custom, new KsidNumericIdTypeParser()));
        assertThat(composite.parse("ORD-123").type()).isEqualTo(IdType.BIZ);
        assertThat(composite.parse("ORD-123").businessType()).isEqualTo("ORDER");
    }
}
