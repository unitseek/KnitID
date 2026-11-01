package org.unitseek.knitid.autoconfigure;

import java.time.Duration;
import java.util.Map;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Status;

import org.unitseek.knitid.core.api.HealthState;
import org.unitseek.knitid.core.api.IdGenerator;
import org.unitseek.knitid.core.api.IdGeneratorHealth;
import org.unitseek.knitid.core.api.IdGeneratorHealthSnapshot;
import org.unitseek.knitid.core.api.IdGeneratorRegistry;
import org.unitseek.knitid.core.exception.IdGeneratorNotFoundException;

/**
 * Health indicator for the default generator (health key {@code knitId}).
 */
public class KnitIdHealthIndicator implements HealthIndicator {

    private final IdGeneratorRegistry registry;

    private final KnitIdProperties properties;

    private record CachedHealth(Health health, long atNanos) {
    }

    private volatile CachedHealth cached;

    public KnitIdHealthIndicator(IdGeneratorRegistry registry, KnitIdProperties properties) {
        this.registry = registry;
        this.properties = properties;
    }

    @Override
    public Health health() {
        long now = System.nanoTime();
        CachedHealth current = cached;
        Duration ttl = properties.getHealth().getCacheTtl();
        long ttlNanos;
        try {
            ttlNanos = ttl == null ? 0L : ttl.toNanos();
        }
        catch (ArithmeticException ex) {
            ttlNanos = 0L;
        }
        if (current != null && ttlNanos > 0 && now - current.atNanos() < ttlNanos) {
            return current.health();
        }
        Health computed = compute();
        cached = new CachedHealth(computed, System.nanoTime());
        return computed;
    }

    private Health compute() {
        IdGenerator generator;
        try {
            generator = registry.getDefault();
        }
        catch (IdGeneratorNotFoundException ex) {
            return Health.status(Status.UNKNOWN)
                    .withDetail("reason", "No default generator configured")
                    .build();
        }
        catch (RuntimeException ex) {
            // Do not expose exception messages: they may contain Redis/ZK endpoints or credentials.
            return Health.down()
                    .withDetail("reason", "Generator registry lookup failed")
                    .build();
        }
        try {
            if (generator instanceof IdGeneratorHealth healthGenerator) {
                IdGeneratorHealthSnapshot snapshot = healthGenerator.health(
                        properties.getHealth().getBackwardsDownWindow());
                Map<String, Object> details = snapshot.details();
                int backwards = details.get("clockBackwardsCount") instanceof Number number
                        ? number.intValue()
                        : 0;
                if (snapshot.state() == HealthState.DOWN) {
                    return Health.down().withDetails(details)
                            .withDetail("message", snapshot.message()).build();
                }
                int threshold = properties.getHealth().getBackwardsDownCount();
                if (threshold > 0 && backwards >= threshold) {
                    return Health.down().withDetails(details)
                            .withDetail("message", "Clock moved backwards " + backwards + " times in window")
                            .build();
                }
                if (snapshot.state() == HealthState.UNKNOWN) {
                    return Health.status(Status.UNKNOWN).withDetails(details)
                            .withDetail("message", snapshot.message()).build();
                }
                return Health.up().withDetails(details)
                        .withDetail("message", snapshot.message()).build();
            }
            return Health.up().withDetail("type", generator.type().value()).build();
        }
        catch (RuntimeException ex) {
            // Do not expose exception messages: they may contain infrastructure endpoints.
            return Health.down()
                    .withDetail("reason", "Generator health check failed")
                    .build();
        }
    }
}
