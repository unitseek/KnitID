package org.unitseek.knitid.generator.ksid;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import org.unitseek.knitid.core.parser.IdInfo;
import org.unitseek.knitid.core.spi.WorkerId;

import static org.assertj.core.api.Assertions.assertThat;

class KsidLayoutIdTypeParserTest {

    @Test
    void decodesTimestampNodeAndSequence() {
        KsidConfig config = KsidConfig.defaults();
        KsidGenerator generator = new KsidGenerator(
                Clock.fixed(Instant.parse("2026-05-01T00:00:00Z"), ZoneOffset.UTC),
                new WorkerId(3, 7), config);
        String raw = generator.generate();
        IdInfo info = new KsidLayoutIdTypeParser(config).tryParse(raw).orElseThrow();
        assertThat(info.type().value()).isEqualTo("KSID");
        assertThat(info.timestamp()).isNotNull();
        assertThat(info.nodeId()).isNotNull();
        assertThat(info.sequence()).isZero();
        assertThat(info.attributes()).containsEntry("datacenterId", 3L).containsEntry("workerId", 7L);
    }
}
