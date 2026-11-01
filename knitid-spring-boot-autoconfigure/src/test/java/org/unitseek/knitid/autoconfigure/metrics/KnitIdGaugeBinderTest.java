package org.unitseek.knitid.autoconfigure.metrics;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import org.unitseek.knitid.core.api.IdContext;
import org.unitseek.knitid.core.api.IdGenerator;
import org.unitseek.knitid.core.api.IdGeneratorHealth;
import org.unitseek.knitid.core.api.IdGeneratorHealthSnapshot;
import org.unitseek.knitid.core.api.IdGeneratorRegistry;
import org.unitseek.knitid.core.api.IdType;
import org.unitseek.knitid.core.api.HealthState;

import static org.assertj.core.api.Assertions.assertThat;

class KnitIdGaugeBinderTest {

    @Test
    void exposesClockBackwardsGauge() {
        SimpleMeterRegistry meters = new SimpleMeterRegistry();
        IdGeneratorRegistry registry = registry(new HealthGenerator());
        new KnitIdGaugeBinder(registry, meters, Duration.ofMinutes(1)).afterSingletonsInstantiated();

        assertThat(meters.get("knitid.id.clock.backwards").tag("name", "ksid").gauge().value())
                .isEqualTo(3.0);
        assertThat(meters.get("knitid.id.worker.id").tag("name", "ksid").gauge().value())
                .isEqualTo(7.0);
    }

    private static IdGeneratorRegistry registry(IdGenerator generator) {
        Map<String, IdGenerator> generators = new LinkedHashMap<>();
        generators.put("ksid", generator);
        return new IdGeneratorRegistry() {
            @Override public IdGenerator getDefault() { return generator; }
            @Override public IdGenerator get(String name) { return generators.get(name); }
            @Override public Optional<IdGenerator> find(String name) { return Optional.ofNullable(generators.get(name)); }
            @Override public Optional<IdGenerator> find(IdType type) { return generators.values().stream().filter(g -> g.type().equals(type)).findFirst(); }
            @Override public Set<String> names() { return generators.keySet(); }
            @Override public Map<String, IdGenerator> asMap() { return generators; }
        };
    }

    private static final class HealthGenerator implements IdGenerator, IdGeneratorHealth {
        @Override public IdType type() { return IdType.KSID; }
        @Override public String generate() { return "1"; }
        @Override public String generate(IdContext context) { return "1"; }
        @Override public IdGeneratorHealthSnapshot health(Duration window) {
            return new IdGeneratorHealthSnapshot(HealthState.UP, "ready",
                    Map.of("workerId", 7, "clockBackwardsCount", 3));
        }
    }
}
