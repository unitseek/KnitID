package org.unitseek.knitid.generator.uuid7;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import org.unitseek.knitid.core.random.SecureRandomProvider;
import org.unitseek.knitid.test.MutableClock;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Randomized property checks for UUIDv7 invariants.
 */
class Uuid7PropertyTest {

    private static final int ITERATIONS = 20_000;

    @Test
    void allGeneratedValuesAreWellFormedAndUnique() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        Uuid7Generator generator = new Uuid7Generator(
                clock, new SecureRandomProvider(), true, MonotonicOverflow.THROW);
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < ITERATIONS; i++) {
            if (i % 500 == 0) {
                clock.advance(java.time.Duration.ofMillis(1));
            }
            String id = generator.generate();
            UUID uuid = UUID.fromString(id);
            assertThat(id).hasSize(36);
            assertThat(uuid.version()).isEqualTo(7);
            assertThat(uuid.variant()).isEqualTo(2);
            assertThat(seen.add(id)).as("duplicate id: %s", id).isTrue();
        }
    }

    @Test
    void valuesAreStrictlyIncreasingWhenTheClockIsFixed() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        Uuid7Generator generator = new Uuid7Generator(
                clock, new SecureRandomProvider(), true, MonotonicOverflow.THROW);
        String previous = generator.generate();
        for (int i = 0; i < 50_000; i++) {
            String current = generator.generate();
            assertThat(current).isGreaterThan(previous);
            previous = current;
        }
    }

    private static long timestampOf(String id) {
        String compact = id.replace("-", "");
        return Long.parseUnsignedLong(compact.substring(0, 12), 16);
    }

    @Test
    void timestampStaysWithinGenerationWindow() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        Uuid7Generator generator = new Uuid7Generator(
                clock, new SecureRandomProvider(), true, MonotonicOverflow.THROW);
        long start = clock.millis();
        for (int i = 0; i < 5_000; i++) {
            if (i % 100 == 0) {
                clock.advance(java.time.Duration.ofMillis(1));
            }
            String id = generator.generate();
            UUID uuid = UUID.fromString(id);
            assertThat(timestampOf(id)).isBetween(start, clock.millis());
        }
    }
}
