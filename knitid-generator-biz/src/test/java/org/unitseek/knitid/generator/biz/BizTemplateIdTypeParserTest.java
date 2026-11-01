package org.unitseek.knitid.generator.biz;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import org.unitseek.knitid.core.parser.IdInfo;
import org.unitseek.knitid.core.spi.SequenceProvider;

import static org.assertj.core.api.Assertions.assertThat;

class BizTemplateIdTypeParserTest {

    @Test
    void parsesTemplateMetadataAndCheckDigit() {
        BizConfig config = new BizConfig(
                "{prefix}-{yyyyMMdd}-{seq:6}-{check}",
                "SAMPLE",
                ZoneId.of("Asia/Shanghai"),
                null,
                "memory",
                SequenceReset.DAILY,
                CheckAlgorithm.MOD37_36);
        BizIdGenerator generator = new BizIdGenerator(
                Clock.fixed(Instant.parse("2026-05-01T00:00:00Z"), ZoneOffset.UTC),
                config, BizTemplate.compile(config), new FixedSequence(), new ZeroRandom());
        String id = generator.generate();
        IdInfo info = new BizTemplateIdTypeParser(config).tryParse(id).orElseThrow();
        assertThat(info.type().value()).isEqualTo("BIZ");
        assertThat(info.businessType()).isEqualTo("SAMPLE");
        assertThat(info.sequence()).isZero();
        assertThat(info.timestamp()).isEqualTo(Instant.parse("2026-04-30T16:00:00Z"));
    }

    @Test
    void rejectsBrokenCheckDigit() {
        BizConfig config = new BizConfig(
                "{prefix}-{yyyyMMdd}-{seq:6}-{check}", "SAMPLE", ZoneId.of("UTC"), null,
                "memory", SequenceReset.DAILY, CheckAlgorithm.MOD37_36);
        assertThat(new BizTemplateIdTypeParser(config).tryParse("SAMPLE-20260501-000000-0")).isEmpty();
    }

    private static final class FixedSequence implements SequenceProvider {
        @Override public long next(String scope) { return 0; }
    }

    private static final class ZeroRandom implements org.unitseek.knitid.core.spi.RandomProvider {
        @Override public void nextBytes(byte[] bytes) { java.util.Arrays.fill(bytes, (byte) 0); }
        @Override public int nextInt(int bound) { return 0; }
        @Override public boolean secure() { return false; }
    }
}
