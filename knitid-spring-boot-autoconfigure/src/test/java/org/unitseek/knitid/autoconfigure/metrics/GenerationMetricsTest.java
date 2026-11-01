package org.unitseek.knitid.autoconfigure.metrics;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import org.unitseek.knitid.core.api.IdContext;
import org.unitseek.knitid.core.api.IdGenerator;
import org.unitseek.knitid.core.api.IdGeneratorHealth;
import org.unitseek.knitid.core.api.IdGeneratorHealthSnapshot;
import org.unitseek.knitid.core.api.IdGeneratorRegistry;
import org.unitseek.knitid.core.api.IdType;
import org.unitseek.knitid.core.api.NumericIdGenerator;
import org.unitseek.knitid.core.exception.ClockBackwardsException;
import org.unitseek.knitid.core.exception.IdErrorCodes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GenerationMetricsTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

    @Test
    void recordsDurationAndBatchSize() {
        IdGenerator generator = IdGeneratorMetrics.wrap(new PlainGenerator(), "test", registry);
        generator.generate();
        generator.generateBatch(5);
        assertThat(registry.find("knitid.id.generation.duration").timer().count()).isEqualTo(6);
        assertThat(registry.find("knitid.id.generation.duration").tag("id.type", "uuid7").timer()).isNotNull();
        assertThat(registry.find("knitid.id.batch.size").summary().count()).isEqualTo(1);
        assertThat(registry.find("knitid.id.batch.size").summary().totalAmount()).isEqualTo(5);
    }

    @Test
    void recordsErrorReason() {
        IdGenerator generator = IdGeneratorMetrics.wrap(new FailingGenerator(), "test", registry);
        assertThatThrownBy(generator::generate).isInstanceOf(ClockBackwardsException.class);
        assertThat(registry.find("knitid.id.generation.errors").tag("reason", "CLOCK_BACKWARDS").counter().count())
                .isEqualTo(1);
        assertThat(registry.find("knitid.id.generation.errors").tag("error.type", "CLOCK_BACKWARDS").counter().count())
                .isEqualTo(1);
        assertThat(registry.find("knitid.id.generation.duration").timer().count()).isEqualTo(1);
    }

    @Test
    void recordsNumericGenerators() {
        NumericIdGenerator generator = (NumericIdGenerator) IdGeneratorMetrics.wrap(new NumericGenerator(), "ksid", registry);
        assertThat(generator.generateLong()).isEqualTo(1);
        assertThat(generator.generateLongBatch(3)).hasSize(3);
        assertThat(registry.find("knitid.id.generation.duration").timer().count()).isEqualTo(4);
        assertThat(registry.find("knitid.id.batch.size").summary().totalAmount()).isEqualTo(3);
    }

    @Test
    void bindsWorkerIdGauge() {
        WorkerGenerator generator = new WorkerGenerator();
        FakeRegistry idRegistry = new FakeRegistry(Map.of("order-id", generator), generator);
        new KnitIdGaugeBinder(idRegistry, registry, Duration.ofMinutes(1)).afterSingletonsInstantiated();
        assertThat(registry.find("knitid.id.worker.id").tag("name", "order-id").gauge().value()).isEqualTo(5.0);
    }

    private static final class PlainGenerator implements IdGenerator {

        @Override
        public IdType type() {
            return IdType.UUID7;
        }

        @Override
        public String generate() {
            return "id";
        }

        @Override
        public String generate(IdContext context) {
            return generate();
        }
    }

    private static final class FailingGenerator implements IdGenerator {

        @Override
        public IdType type() {
            return IdType.KSID;
        }

        @Override
        public String generate() {
            throw new ClockBackwardsException(IdErrorCodes.CLOCK_BACKWARDS, "backwards");
        }

        @Override
        public String generate(IdContext context) {
            return generate();
        }
    }

    private static final class NumericGenerator implements NumericIdGenerator {

        private final AtomicLong counter = new AtomicLong();

        @Override
        public IdType type() {
            return IdType.KSID;
        }

        @Override
        public String generate() {
            return Long.toString(generateLong());
        }

        @Override
        public String generate(IdContext context) {
            return generate();
        }

        @Override
        public long generateLong() {
            return counter.incrementAndGet();
        }

        @Override
        public long generateLong(IdContext context) {
            return generateLong();
        }
    }

    private static final class WorkerGenerator implements IdGenerator, IdGeneratorHealth {

        @Override
        public IdType type() {
            return IdType.KSID;
        }

        @Override
        public String generate() {
            return "1";
        }

        @Override
        public String generate(IdContext context) {
            return generate();
        }

        @Override
        public IdGeneratorHealthSnapshot health(Duration window) {
            return new IdGeneratorHealthSnapshot(
                    org.unitseek.knitid.core.api.HealthState.UP, "ready", Map.of("workerId", 5));
        }
    }

    private static final class FakeRegistry implements IdGeneratorRegistry {

        private final Map<String, IdGenerator> generators;

        private final IdGenerator defaultGenerator;

        private FakeRegistry(Map<String, IdGenerator> generators, IdGenerator defaultGenerator) {
            this.generators = generators;
            this.defaultGenerator = defaultGenerator;
        }

        @Override
        public IdGenerator getDefault() {
            return defaultGenerator;
        }

        @Override
        public IdGenerator get(String name) {
            return generators.get(name);
        }

        @Override
        public Optional<IdGenerator> find(String name) {
            return Optional.ofNullable(generators.get(name));
        }

        @Override
        public Optional<IdGenerator> find(IdType type) {
            return generators.values().stream().filter(g -> g.type().equals(type)).findFirst();
        }

        @Override
        public Set<String> names() {
            return generators.keySet();
        }

        @Override
        public Map<String, IdGenerator> asMap() {
            return generators;
        }
    }
}
