package org.unitseek.knitid.generator.ulid;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.unitseek.knitid.core.random.PerformanceRandomProvider;
import org.unitseek.knitid.test.MutableClock;

import static org.assertj.core.api.Assertions.assertThat;

class UlidPropertyTest {

    private static final int ITERATIONS = 20_000;

    @Test
    void allGeneratedValuesAreCanonicalAndUnique() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        UlidGenerator generator = new UlidGenerator(
                clock, new PerformanceRandomProvider(), true, UlidMonotonicOverflow.THROW);
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < ITERATIONS; i++) {
            if (i % 500 == 0) {
                clock.advance(java.time.Duration.ofMillis(1));
            }
            String ulid = generator.generate();
            assertThat(ulid).hasSize(26);
            assertThat(ulid).matches("[0-9A-HJKMNP-TV-Z]{26}");
            assertThat(seen.add(ulid)).as("duplicate ULID: %s", ulid).isTrue();
        }
    }

    @Test
    void valuesAreStrictlyIncreasingForFixedClock() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        UlidGenerator generator = new UlidGenerator(
                clock, new PerformanceRandomProvider(), true, UlidMonotonicOverflow.THROW);
        String previous = generator.generate();
        for (int i = 0; i < 50_000; i++) {
            String current = generator.generate();
            assertThat(current).isGreaterThan(previous);
            previous = current;
        }
    }
}
