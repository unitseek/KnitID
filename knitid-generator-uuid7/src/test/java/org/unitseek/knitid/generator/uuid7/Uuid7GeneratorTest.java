package org.unitseek.knitid.generator.uuid7;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import org.unitseek.knitid.core.api.IdContext;
import org.unitseek.knitid.core.exception.IdGenerationException;
import org.unitseek.knitid.test.MutableClock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Uuid7GeneratorTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void generatesValidUuid7Values() {
        MutableClock clock = new MutableClock(NOW);
        Uuid7Generator generator = new Uuid7Generator(clock, allZeroRandom());
        for (int i = 0; i < 100; i++) {
            UUID uuid = UUID.fromString(generator.generate());
            assertThat(uuid.version()).isEqualTo(7);
            assertThat(uuid.variant()).isEqualTo(2);
        }
    }

    @Test
    void monotonicWithinSameMillisecond() {
        MutableClock clock = new MutableClock(NOW);
        Uuid7Generator generator = new Uuid7Generator(clock, allZeroRandom());
        String previous = generator.generate();
        for (int i = 0; i < 100; i++) {
            String current = generator.generate();
            assertThat(current).isGreaterThan(previous);
            previous = current;
        }
    }

    @Test
    void batchBoundariesAreChecked() {
        Uuid7Generator generator = new Uuid7Generator(new MutableClock(NOW), allZeroRandom());
        assertThatThrownBy(() -> generator.generateBatch(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> generator.generateBatch(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> generator.generateBatch(10_001)).isInstanceOf(IllegalArgumentException.class);
        List<String> batch = generator.generateBatch(10);
        assertThat(batch).hasSize(10).doesNotHaveDuplicates();
    }

    @Test
    void monotonicOverflowThrowsWhenConfigured() {
        MutableClock clock = new MutableClock(NOW);
        Uuid7Generator generator = new Uuid7Generator(
                clock, allOnesRandom(), true, MonotonicOverflow.THROW);
        generator.generate();
        assertThatThrownBy(generator::generate).isInstanceOf(IdGenerationException.class);
    }

    @Test
    void monotonicOverflowWaitsForNextMillisecond() {
        TickingClock clock = new TickingClock(NOW.toEpochMilli());
        Uuid7Generator generator = new Uuid7Generator(
                clock, allOnesRandom(), true, MonotonicOverflow.WAIT);
        String first = generator.generate();
        String second = generator.generate();
        assertThat(second).isGreaterThan(first);
        assertThat(timestampOf(second)).isGreaterThanOrEqualTo(timestampOf(first));
    }

    @Test
    void clockGoingBackwardsKeepsMonotonicValues() {
        MutableClock clock = new MutableClock(NOW);
        Uuid7Generator generator = new Uuid7Generator(clock, allZeroRandom());
        String first = generator.generate();
        clock.setInstant(NOW.minus(Duration.ofSeconds(1)));
        String second = generator.generate();
        assertThat(second).isGreaterThan(first);
    }

    private static long timestampOf(String id) {
        String compact = id.replace("-", "");
        return Long.parseUnsignedLong(compact.substring(0, 12), 16);
    }

    private static org.unitseek.knitid.core.spi.RandomProvider allZeroRandom() {
        return new FixedRandomProvider((byte) 0x00);
    }

    private static org.unitseek.knitid.core.spi.RandomProvider allOnesRandom() {
        return new FixedRandomProvider((byte) 0xFF);
    }

    private static final class FixedRandomProvider implements org.unitseek.knitid.core.spi.RandomProvider {

        private final byte value;

        private FixedRandomProvider(byte value) {
            this.value = value;
        }

        @Override
        public void nextBytes(byte[] bytes) {
            java.util.Arrays.fill(bytes, value);
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
