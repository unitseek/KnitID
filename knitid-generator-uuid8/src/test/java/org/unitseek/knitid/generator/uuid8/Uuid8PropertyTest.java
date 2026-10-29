package org.unitseek.knitid.generator.uuid8;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import org.unitseek.knitid.core.random.SecureRandomProvider;
import org.unitseek.knitid.test.MutableClock;

import static org.assertj.core.api.Assertions.assertThat;

class Uuid8PropertyTest {

    private static final int ITERATIONS = 20_000;

    @Test
    void timeRandomValuesAreUniqueAndWellFormed() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        Uuid8Generator generator = new Uuid8Generator(clock,
                Uuid8Config.defaults(), new SecureRandomProvider());
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < ITERATIONS; i++) {
            if (i % 500 == 0) {
                clock.advance(java.time.Duration.ofMillis(1));
            }
            String id = generator.generate();
            UUID uuid = UUID.fromString(id);
            assertThat(uuid.version()).isEqualTo(8);
            assertThat(uuid.variant()).isEqualTo(2);
            assertThat(seen.add(id)).as("duplicate UUIDv8: %s", id).isTrue();
        }
    }
}
