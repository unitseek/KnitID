package org.unitseek.knitid.generator.ulid;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.unitseek.knitid.core.api.IdContext;
import org.unitseek.knitid.core.exception.IdGenerationException;
import org.unitseek.knitid.core.spi.RandomProvider;
import org.unitseek.knitid.test.MutableClock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UlidGeneratorTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private static final String ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";

    @Test
    void generatesWellFormedAndUniqueValues() {
        MutableClock clock = new MutableClock(NOW);
        UlidGenerator generator = new UlidGenerator(clock, new FixedRandomProvider((byte) 0x42));
        for (int i = 0; i < 100; i++) {
            String ulid = generator.generate();
            assertThat(ulid).hasSize(26);
            assertThat(ulid).matches("[0-9A-HJKMNP-TV-Z]{26}");
            assertThat(timestampOf(ulid)).isEqualTo(NOW.toEpochMilli());
        }
    }

    @Test
    void monotonicWithinSameMillisecond() {
        MutableClock clock = new MutableClock(NOW);
        UlidGenerator generator = new UlidGenerator(clock, new FixedRandomProvider((byte) 0));
        String previous = generator.generate();
        for (int i = 0; i < 1000; i++) {
            String current = generator.generate();
            assertThat(current).isGreaterThan(previous);
            previous = current;
        }
    }

    @Test
    void overflowThrowsWhenConfigured() {
        MutableClock clock = new MutableClock(NOW);
        UlidGenerator generator = new UlidGenerator(clock, new FixedRandomProvider((byte) 0xFF),
                true, UlidMonotonicOverflow.THROW);
        generator.generate();
        assertThatThrownBy(generator::generate).isInstanceOf(IdGenerationException.class);
    }

    @Test
    void overflowWaitsForNextMillisecond() {
        TickingClock clock = new TickingClock(NOW.toEpochMilli());
        UlidGenerator generator = new UlidGenerator(clock, new FixedRandomProvider((byte) 0xFF),
                true, UlidMonotonicOverflow.WAIT);
        String first = generator.generate();
        String second = generator.generate();
        assertThat(second).isGreaterThan(first);
        assertThat(timestampOf(second)).isGreaterThanOrEqualTo(timestampOf(first));
    }

    @Test
    void clockGoingBackwardsKeepsTimestamp() {
        MutableClock clock = new MutableClock(NOW);
        UlidGenerator generator = new UlidGenerator(clock, new FixedRandomProvider((byte) 0));
        String first = generator.generate();
        clock.setInstant(NOW.minus(Duration.ofSeconds(1)));
        String second = generator.generate();
        assertThat(timestampOf(second)).isEqualTo(timestampOf(first));
        assertThat(second).isGreaterThan(first);
    }

    @Test
    void batchBoundariesAreChecked() {
        UlidGenerator generator = new UlidGenerator(new MutableClock(NOW), new FixedRandomProvider((byte) 0));
        assertThatThrownBy(() -> generator.generateBatch(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> generator.generateBatch(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> generator.generateBatch(10_001)).isInstanceOf(IllegalArgumentException.class);
        List<String> batch = generator.generateBatch(10);
        assertThat(batch).hasSize(10).doesNotHaveDuplicates();
    }

    @Test
    void timestampIsExtractedFromTheFirstTenCharacters() {
        MutableClock clock = new MutableClock(NOW);
        UlidGenerator generator = new UlidGenerator(clock, new FixedRandomProvider((byte) 0x01));
        assertThat(timestampOf(generator.generate())).isEqualTo(NOW.toEpochMilli());
    }

    private static long timestampOf(String ulid) {
        long value = 0;
        for (int i = 0; i < 10; i++) {
            int index = ALPHABET.indexOf(ulid.charAt(i));
            assertThat(index).isGreaterThanOrEqualTo(0);
            value = (value << 5) | index;
        }
        return value & 0xFFFF_FFFF_FFFFL;
    }

    private static final class FixedRandomProvider implements RandomProvider {

        private final byte value;

        private FixedRandomProvider(byte value) {
            this.value = value;
        }

        @Override
        public void nextBytes(byte[] bytes) {
            Arrays.fill(bytes, value);
        }

        @Override
        public int nextInt(int bound) {
            return 0;
        }

        @Override
        public boolean secure() {
            return false;
        }
    }

    private static final class TickingClock extends Clock {

        private long currentMillis;

        private int calls;

        private TickingClock(long currentMillis) {
            this.currentMillis = currentMillis;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            long value = currentMillis;
            if (calls >= 2) {
                currentMillis++;
            }
            calls++;
            return Instant.ofEpochMilli(value);
        }
    }
}
