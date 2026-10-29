package org.unitseek.knitid.generator.biz;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.unitseek.knitid.core.api.IdContext;
import org.unitseek.knitid.core.exception.IdConfigurationException;
import org.unitseek.knitid.core.exception.IdGenerationException;
import org.unitseek.knitid.core.random.SecureRandomProvider;
import org.unitseek.knitid.test.MutableClock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BizIdGeneratorTest {

    private static final Instant NOW = Instant.parse("2026-01-01T12:34:56Z");

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    @Test
    void generatesTemplateWithSequenceAndCheckDigit() {
        MutableClock clock = new MutableClock(NOW);
        BizConfig config = config("{prefix}-{yyyyMMdd}-{seq:6}-{check}", "ORD", ZONE, null,
                SequenceReset.DAILY, CheckAlgorithm.MOD37_36);
        BizIdGenerator generator = generator(clock, config, new MemorySequenceProvider());

        String first = generator.generate();
        String second = generator.generate();

        assertThat(first).matches("ORD-\\d{8}-000000-.{1}");
        assertThat(second).matches("ORD-\\d{8}-000001-.{1}");
        String body = first.substring(0, first.length() - 1);
        assertThat(first.charAt(first.length() - 1)).isEqualTo(CheckAlgorithm.MOD37_36.checkDigit(body));
    }

    @Test
    void sequenceResetsDailyInConfiguredZone() {
        MutableClock clock = new MutableClock(NOW);
        BizConfig config = config("{seq:4}", null, ZONE, null, SequenceReset.DAILY, CheckAlgorithm.NONE);
        BizIdGenerator generator = generator(clock, config, new MemorySequenceProvider());

        assertThat(generator.generate()).isEqualTo("0000");
        assertThat(generator.generate()).isEqualTo("0001");
        clock.advance(Duration.ofDays(1));
        assertThat(generator.generate()).isEqualTo("0000");
    }

    @Test
    void sequenceNeverResetsWhenConfigured() {
        MutableClock clock = new MutableClock(NOW);
        BizConfig config = config("{seq:4}", null, ZONE, null, SequenceReset.NEVER, CheckAlgorithm.NONE);
        BizIdGenerator generator = generator(clock, config, new MemorySequenceProvider());

        assertThat(generator.generate()).isEqualTo("0000");
        clock.advance(Duration.ofDays(1));
        assertThat(generator.generate()).isEqualTo("0001");
    }

    @Test
    void contextPrefixOverridesConfiguredPrefix() {
        MutableClock clock = new MutableClock(NOW);
        BizConfig config = config("{prefix}-{seq:2}", "ORD", ZONE, null,
                SequenceReset.NEVER, CheckAlgorithm.NONE);
        BizIdGenerator generator = generator(clock, config, new MemorySequenceProvider());

        IdContext context = IdContext.builder().prefix("USR").build();
        assertThat(generator.generate(context)).startsWith("USR-");
    }

    @Test
    void businessDateOverridesDatePlaceholders() {
        MutableClock clock = new MutableClock(NOW);
        BizConfig config = config("{yyyyMMdd}-{seq:2}", null, ZONE, null,
                SequenceReset.DAILY, CheckAlgorithm.NONE);
        BizIdGenerator generator = generator(clock, config, new MemorySequenceProvider());

        IdContext context = IdContext.builder().businessDate(java.time.LocalDate.of(2025, 12, 31)).build();
        assertThat(generator.generate(context)).isEqualTo("20251231-00");
    }

    @Test
    void workerPlaceholderUsesNodeId() {
        MutableClock clock = new MutableClock(NOW);
        BizConfig config = config("{worker}-{seq:2}", null, ZONE, 7L,
                SequenceReset.NEVER, CheckAlgorithm.NONE);
        BizIdGenerator generator = generator(clock, config, new MemorySequenceProvider());
        assertThat(generator.generate()).isEqualTo("7-00");
    }

    @Test
    void sequenceOverflowFails() {
        MutableClock clock = new MutableClock(NOW);
        BizConfig config = config("{seq:2}", null, ZONE, null,
                SequenceReset.NEVER, CheckAlgorithm.NONE);
        BizIdGenerator generator = generator(clock, config, new MemorySequenceProvider());
        for (int i = 0; i < 100; i++) {
            assertThat(generator.generate()).hasSize(2);
        }
        assertThatThrownBy(generator::generate)
                .isInstanceOf(IdGenerationException.class)
                .hasMessageContaining("KNITID-2005");
    }

    @Test
    void generatedIdsAreUniqueUnderConcurrency() throws Exception {
        MutableClock clock = new MutableClock(NOW);
        BizConfig config = config("{seq:6}", null, ZONE, null, SequenceReset.NEVER, CheckAlgorithm.NONE);
        BizIdGenerator generator = generator(clock, config, new MemorySequenceProvider());
        Set<String> ids = java.util.Collections.synchronizedSet(new HashSet<>());
        Thread[] threads = new Thread[8];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 500; j++) {
                    ids.add(generator.generate());
                }
            });
            threads[i].start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        assertThat(ids).hasSize(4000);
    }

    @Test
    void templateValidationRejectsBadTemplates() {
        assertThatThrownBy(() -> BizTemplate.compile(config("{unknown}", null, ZONE, null, SequenceReset.DAILY, CheckAlgorithm.NONE)))
                .isInstanceOf(IdConfigurationException.class);
        assertThatThrownBy(() -> BizTemplate.compile(config("{prefix}", null, ZONE, null, SequenceReset.DAILY, CheckAlgorithm.NONE)))
                .isInstanceOf(IdConfigurationException.class)
                .hasMessageContaining("biz.prefix");
        assertThatThrownBy(() -> BizTemplate.compile(config("{worker}", null, ZONE, null, SequenceReset.DAILY, CheckAlgorithm.NONE)))
                .isInstanceOf(IdConfigurationException.class)
                .hasMessageContaining("biz.node-id");
        assertThatThrownBy(() -> BizTemplate.compile(config("{check}-{seq:2}", null, ZONE, null, SequenceReset.DAILY, CheckAlgorithm.MOD37_36)))
                .isInstanceOf(IdConfigurationException.class)
                .hasMessageContaining("last");
        assertThatThrownBy(() -> BizTemplate.compile(config("{seq:0}", null, ZONE, null, SequenceReset.DAILY, CheckAlgorithm.NONE)))
                .isInstanceOf(IdConfigurationException.class);
        assertThatThrownBy(() -> BizTemplate.compile(config("{check}", null, ZONE, null, SequenceReset.DAILY, CheckAlgorithm.NONE)))
                .isInstanceOf(IdConfigurationException.class)
                .hasMessageContaining("NONE");
    }

    private static BizConfig config(String template, String prefix, ZoneId zone, Long nodeId,
            SequenceReset reset, CheckAlgorithm algorithm) {
        return new BizConfig(template, prefix, zone, nodeId, "memory", reset, algorithm);
    }

    private static BizIdGenerator generator(MutableClock clock, BizConfig config, MemorySequenceProvider provider) {
        return new BizIdGenerator(clock, config, BizTemplate.compile(config), provider, new SecureRandomProvider());
    }
}
