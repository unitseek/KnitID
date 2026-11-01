package org.unitseek.knitid.autoconfigure;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import org.unitseek.knitid.core.api.HealthState;
import org.unitseek.knitid.core.api.IdContext;
import org.unitseek.knitid.core.api.IdGenerator;
import org.unitseek.knitid.core.api.IdGeneratorHealth;
import org.unitseek.knitid.core.api.IdGeneratorHealthSnapshot;
import org.unitseek.knitid.core.api.IdGeneratorRegistry;
import org.unitseek.knitid.core.api.IdType;
import org.unitseek.knitid.core.exception.IdErrorCodes;
import org.unitseek.knitid.core.exception.IdGeneratorNotFoundException;
import org.unitseek.knitid.generator.uuid7.autoconfigure.Uuid7AutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class KnitIdHealthIndicatorTest {

    private final KnitIdProperties properties = new KnitIdProperties();

    @Test
    void healthAutoConfigurationRegistersContributor() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        Uuid7AutoConfiguration.class, KnitIdAutoConfiguration.class,
                        KnitIdHealthAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(KnitIdHealthIndicator.class);
                    assertThat(context.getBean(KnitIdHealthIndicator.class).health().getStatus())
                            .isEqualTo(Status.UP);
                });
    }

    @Test
    void unknownWhenNoDefaultGenerator() {
        KnitIdHealthIndicator indicator = new KnitIdHealthIndicator(new FakeRegistry(null), properties);
        assertThat(indicator.health().getStatus()).isEqualTo(Status.UNKNOWN);
    }

    @Test
    void upForHealthyGenerator() {
        HealthGenerator generator = new HealthGenerator(HealthState.UP, 0);
        KnitIdHealthIndicator indicator = new KnitIdHealthIndicator(new FakeRegistry(generator), properties);
        assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void downWhenGeneratorReportsDown() {
        HealthGenerator generator = new HealthGenerator(HealthState.DOWN, 0);
        KnitIdHealthIndicator indicator = new KnitIdHealthIndicator(new FakeRegistry(generator), properties);
        assertThat(indicator.health().getStatus()).isEqualTo(Status.DOWN);
    }

    @Test
    void downWhenClockBackwardsCountExceedsThreshold() {
        properties.getHealth().setBackwardsDownCount(3);
        HealthGenerator generator = new HealthGenerator(HealthState.UP, 3);
        KnitIdHealthIndicator indicator = new KnitIdHealthIndicator(new FakeRegistry(generator), properties);
        assertThat(indicator.health().getStatus()).isEqualTo(Status.DOWN);
    }

    @Test
    void upWhenClockBackwardsCountIsBelowThreshold() {
        properties.getHealth().setBackwardsDownCount(3);
        HealthGenerator generator = new HealthGenerator(HealthState.UP, 2);
        KnitIdHealthIndicator indicator = new KnitIdHealthIndicator(new FakeRegistry(generator), properties);
        assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void downWithoutLeakingHealthExceptionDetails() {
        KnitIdHealthIndicator indicator = new KnitIdHealthIndicator(
                new FakeRegistry(new FailingHealthGenerator()), properties);
        var health = indicator.health();
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).doesNotContainKey("error");
        assertThat(health.getDetails().toString()).doesNotContain("leak-me");
    }

    @Test
    void upForGeneratorWithoutHealthSupport() {
        KnitIdHealthIndicator indicator = new KnitIdHealthIndicator(
                new FakeRegistry(new PlainGenerator()), properties);
        assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
    }

    private static final class HealthGenerator implements IdGenerator, IdGeneratorHealth {

        private final HealthState state;

        private final int backwards;

        private HealthGenerator(HealthState state, int backwards) {
            this.state = state;
            this.backwards = backwards;
        }

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
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("clockBackwardsCount", backwards);
            return new IdGeneratorHealthSnapshot(state, "test", details);
        }
    }

    private static final class FailingHealthGenerator implements IdGenerator, IdGeneratorHealth {

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
            throw new IllegalStateException("redis://user:leak-me@host");
        }
    }

    private static final class PlainGenerator implements IdGenerator {

        @Override
        public IdType type() {
            return IdType.UUID7;
        }

        @Override
        public String generate() {
            return "1";
        }

        @Override
        public String generate(IdContext context) {
            return generate();
        }
    }

    private static final class FakeRegistry implements IdGeneratorRegistry {

        private final IdGenerator generator;

        private FakeRegistry(IdGenerator generator) {
            this.generator = generator;
        }

        @Override
        public IdGenerator getDefault() {
            if (generator == null) {
                throw new IdGeneratorNotFoundException(IdErrorCodes.GENERATOR_NOT_FOUND, "none");
            }
            return generator;
        }

        @Override
        public IdGenerator get(String name) {
            return getDefault();
        }

        @Override
        public Optional<IdGenerator> find(String name) {
            return Optional.ofNullable(generator);
        }

        @Override
        public Optional<IdGenerator> find(IdType type) {
            return Optional.ofNullable(generator);
        }

        @Override
        public Set<String> names() {
            return Set.of("default");
        }

        @Override
        public Map<String, IdGenerator> asMap() {
            return generator == null ? Map.of() : Map.of("default", generator);
        }
    }
}
