package org.unitseek.knitid.generator.uuid8;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import org.unitseek.knitid.core.random.SecureRandomProvider;
import org.unitseek.knitid.core.spi.RandomProvider;
import org.unitseek.knitid.test.MutableClock;

import static org.assertj.core.api.Assertions.assertThat;

class Uuid8GeneratorTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void timeRandomPresetEncodesTimestampCounterAndRandom() {
        MutableClock clock = new MutableClock(NOW);
        Uuid8Generator generator = new Uuid8Generator(clock,
                new Uuid8Config(Uuid8Preset.TIME_RANDOM, 0, 0), new FixedRandomProvider((byte) 0));

        String first = generator.generate();
        UUID uuid = UUID.fromString(first);
        assertThat(uuid.version()).isEqualTo(8);
        assertThat(uuid.variant()).isEqualTo(2);
        assertThat(customA(first)).isEqualTo(NOW.toEpochMilli());
        assertThat(customB(first)).isZero();
        assertThat(customC(first)).isZero();

        String second = generator.generate();
        assertThat(customA(second)).isEqualTo(NOW.toEpochMilli());
        assertThat(customB(second)).isEqualTo(1);
        assertThat(second).isGreaterThan(first);
    }

    @Test
    void timeRandomWaitsForNextMillisecondWhenCounterExhausted() {
        OverflowClock clock = new OverflowClock(NOW.toEpochMilli());
        Uuid8Generator generator = new Uuid8Generator(clock,
                new Uuid8Config(Uuid8Preset.TIME_RANDOM, 0, 0), new FixedRandomProvider((byte) 0));
        String previous = null;
        for (int i = 0; i < 4096; i++) {
            previous = generator.generate();
            assertThat(customB(previous)).isEqualTo(i);
        }
        String next = generator.generate();
        assertThat(customA(next)).isGreaterThan(customA(previous));
        assertThat(customB(next)).isZero();
    }

    @Test
    void timeNodeRandomEncodesDatacenterAndWorker() {
        MutableClock clock = new MutableClock(NOW);
        Uuid8Generator generator = new Uuid8Generator(clock,
                new Uuid8Config(Uuid8Preset.TIME_NODE_RANDOM, 3, 42), new FixedRandomProvider((byte) 0));
        String id = generator.generate();
        assertThat(customA(id)).isEqualTo(NOW.toEpochMilli());
        assertThat(customB(id)).isEqualTo((3 << 8) | 42);
        assertThat(customC(id)).isZero();
    }

    @Test
    void randomPresetProducesUniqueVersion8Values() {
        Uuid8Generator generator = new Uuid8Generator(Clock.fixed(NOW, ZoneOffset.UTC),
                new Uuid8Config(Uuid8Preset.RANDOM, 0, 0), new SecureRandomProvider());
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            String id = generator.generate();
            UUID uuid = UUID.fromString(id);
            assertThat(uuid.version()).isEqualTo(8);
            assertThat(uuid.variant()).isEqualTo(2);
            assertThat(seen.add(id)).isTrue();
        }
    }

    @Test
    void clockBackwardsKeepsMonotonicTimeRandomValues() {
        MutableClock clock = new MutableClock(NOW);
        Uuid8Generator generator = new Uuid8Generator(clock,
                new Uuid8Config(Uuid8Preset.TIME_RANDOM, 0, 0), new FixedRandomProvider((byte) 0));
        String first = generator.generate();
        clock.setInstant(NOW.minusSeconds(1));
        String second = generator.generate();
        assertThat(customA(second)).isEqualTo(customA(first));
        assertThat(customB(second)).isEqualTo(customB(first) + 1);
        assertThat(second).isGreaterThan(first);
    }

    private static long customA(String id) {
        return Long.parseUnsignedLong(id.replace("-", "").substring(0, 12), 16);
    }

    private static int customB(String id) {
        return (int) (Long.parseUnsignedLong(id.replace("-", "").substring(12, 16), 16) & 0x0FFFL);
    }

    private static long customC(String id) {
        return Long.parseUnsignedLong(id.replace("-", "").substring(16), 16) & 0x3FFF_FFFF_FFFF_FFFFL;
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

    private static final class OverflowClock extends Clock {

        private long currentMillis;

        private int calls;

        private OverflowClock(long currentMillis) {
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
            if (calls >= 4096) {
                currentMillis++;
            }
            calls++;
            return Instant.ofEpochMilli(value);
        }
    }
}
