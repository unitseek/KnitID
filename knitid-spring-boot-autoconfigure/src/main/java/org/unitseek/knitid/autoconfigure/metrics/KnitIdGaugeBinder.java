package org.unitseek.knitid.autoconfigure.metrics;

import java.time.Duration;
import java.util.Map;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.beans.factory.SmartInitializingSingleton;

import org.unitseek.knitid.core.api.IdGenerator;
import org.unitseek.knitid.core.api.IdGeneratorHealth;
import org.unitseek.knitid.core.api.IdType;
import org.unitseek.knitid.core.api.IdGeneratorHealthSnapshot;
import org.unitseek.knitid.core.api.IdGeneratorRegistry;

/**
 * Registers WorkerId and clock-backwards gauges for generators that expose
 * health details.
 */
public final class KnitIdGaugeBinder implements SmartInitializingSingleton {

    private final IdGeneratorRegistry registry;

    private final MeterRegistry meterRegistry;

    private final Duration window;

    public KnitIdGaugeBinder(IdGeneratorRegistry registry, MeterRegistry meterRegistry, Duration window) {
        this.registry = registry;
        this.meterRegistry = meterRegistry;
        this.window = window;
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (meterRegistry == null) {
            return;
        }
        for (Map.Entry<String, IdGenerator> entry : registry.asMap().entrySet()) {
            IdGenerator generator = entry.getValue();
            if (!(generator instanceof IdGeneratorHealth health)) {
                continue;
            }
            boolean register = generator.type().equals(IdType.KSID);
            if (!register) {
                try {
                    IdGeneratorHealthSnapshot snapshot = health.health(window);
                    register = snapshot != null && snapshot.details().containsKey("workerId");
                }
                catch (RuntimeException ex) {
                    // A faulty health implementation must not break application startup.
                }
            }
            if (register) {
                Gauge.builder("knitid.id.worker.id", () -> detail(health, "workerId"))
                        .tag("name", entry.getKey())
                        .tag("id.type", generator.type().value().toLowerCase(java.util.Locale.ROOT))
                        .tag("id.name", entry.getKey())
                        .description("Current KSID WorkerId")
                        .register(meterRegistry);
            }
            Gauge.builder("knitid.id.clock.backwards", () -> detail(health, "clockBackwardsCount"))
                    .tag("name", entry.getKey())
                    .tag("id.type", generator.type().value().toLowerCase(java.util.Locale.ROOT))
                    .tag("id.name", entry.getKey())
                    .description("Clock-backwards events observed in the health window")
                    .register(meterRegistry);
        }
    }

    private double detail(IdGeneratorHealth health, String key) {
        try {
            IdGeneratorHealthSnapshot snapshot = health.health(window);
            if (snapshot == null) {
                return Double.NaN;
            }
            Object value = snapshot.details().get(key);
            return value instanceof Number number ? number.doubleValue() : Double.NaN;
        }
        catch (RuntimeException ex) {
            return Double.NaN;
        }
    }
}
