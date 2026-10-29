package org.unitseek.knitid.autoconfigure;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.unitseek.knitid.core.api.IdContext;
import org.unitseek.knitid.core.api.IdGenerator;
import org.unitseek.knitid.core.api.IdGeneratorRegistry;
import org.unitseek.knitid.core.api.IdType;
import org.unitseek.knitid.core.exception.IdConfigurationException;
import org.unitseek.knitid.core.exception.IdGeneratorNotFoundException;
import org.unitseek.knitid.core.spi.GeneratorConfig;
import org.unitseek.knitid.core.spi.IdGeneratorFactory;
import org.unitseek.knitid.generator.uuid7.autoconfigure.Uuid7AutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KnitIdAutoConfigurationTest {

    private static final IdType FAKE = IdType.of("FAKE");

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(KnitIdAutoConfiguration.class));

    private final ApplicationContextRunner withUuid7 = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(Uuid7AutoConfiguration.class, KnitIdAutoConfiguration.class));

    @Test
    void startsWithoutGeneratorsAndReportsMissingDefault() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            IdGeneratorRegistry registry = context.getBean(IdGeneratorRegistry.class);
            assertThat(registry.names()).isEmpty();
            assertThatThrownBy(registry::getDefault).isInstanceOf(IdGeneratorNotFoundException.class);
            IdGenerator primary = context.getBean(IdGenerator.class);
            assertThatThrownBy(primary::generate).isInstanceOf(IdGeneratorNotFoundException.class);
        });
    }

    @Test
    void failsWhenDefaultTypeModuleIsMissing() {
        runner.withPropertyValues("knitid.default-type=UUID7").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasStackTraceContaining("KNITID-1002");
        });
    }

    @Test
    void autoSelectsSingleFactoryAsDefault() {
        withUuid7.run(context -> {
            assertThat(context).hasNotFailed();
            IdGeneratorRegistry registry = context.getBean(IdGeneratorRegistry.class);
            assertThat(registry.getDefault().type()).isEqualTo(IdType.UUID7);
            assertThat(registry.names()).containsExactly("default");
            IdGenerator primary = context.getBean(IdGenerator.class);
            assertThat(primary.type()).isEqualTo(IdType.UUID7);
            assertThat(primary.generate()).hasSize(36);
        });
    }

    @Test
    void failsWithMultipleFactoriesAndNoDefault() {
        withUuid7.withUserConfiguration(FakeGeneratorConfiguration.class).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasStackTraceContaining("KNITID-1003");
        });
    }

    @Test
    void selectsConfiguredDefaultTypeAmongMultipleFactories() {
        withUuid7.withUserConfiguration(FakeGeneratorConfiguration.class)
                .withPropertyValues("knitid.default-type=UUID7")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    IdGeneratorRegistry registry = context.getBean(IdGeneratorRegistry.class);
                    assertThat(registry.getDefault().type()).isEqualTo(IdType.UUID7);
                });
    }

    @Test
    void registersNamedGeneratorFromConfiguration() {
        withUuid7.withPropertyValues("knitid.generators.order-id.type=UUID7").run(context -> {
            assertThat(context).hasNotFailed();
            IdGeneratorRegistry registry = context.getBean(IdGeneratorRegistry.class);
            assertThat(registry.get("order-id").type()).isEqualTo(IdType.UUID7);
            assertThat(registry.getDefault().type()).isEqualTo(IdType.UUID7);
        });
    }

    @Test
    void usesDefaultGeneratorNameWhenConfigured() {
        withUuid7.withPropertyValues(
                "knitid.generators.order-id.type=UUID7",
                "knitid.generators.other-id.type=UUID7",
                "knitid.default-generator=order-id")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    IdGeneratorRegistry registry = context.getBean(IdGeneratorRegistry.class);
                    assertThat(registry.getDefault()).isSameAs(registry.get("order-id"));
                    assertThat(registry.getDefault()).isNotSameAs(registry.get("other-id"));
                });
    }

    @Test
    void failsOnUnknownGeneratorProperty() {
        withUuid7.withPropertyValues(
                "knitid.generators.order-id.type=UUID7",
                "knitid.generators.order-id.unknown-key=1")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasStackTraceContaining("KNITID-1001");
                });
    }

    @Test
    void failsWhenTypeBlockDoesNotMatchType() {
        withUuid7.withPropertyValues(
                "knitid.generators.order-id.type=UUID7",
                "knitid.generators.order-id.ksid.epoch=2024-01-01T00:00:00Z")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasStackTraceContaining("KNITID-1001");
                });
    }

    @Test
    void failsOnUnknownTopLevelProperty() {
        runner.withPropertyValues("knitid.unknown-property=1").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasStackTraceContaining("unknown-property");
        });
    }

    @Test
    void canBeDisabled() {
        runner.withPropertyValues("knitid.enabled=false").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(IdGeneratorRegistry.class);
            assertThat(context).doesNotHaveBean(IdGenerator.class);
        });
    }

    @Test
    void failureAnalyzerProducesActionableConfigurationError() {
        runner.withPropertyValues("knitid.default-type=KSID").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasStackTraceContaining("KNITID-1002");
            assertThat(context.getStartupFailure()).hasStackTraceContaining("knitid-generator-ksid");
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class FakeGeneratorConfiguration {

        @Bean
        IdGeneratorFactory fakeIdGeneratorFactory() {
            return new FakeIdGeneratorFactory();
        }
    }

    private static final class FakeIdGeneratorFactory implements IdGeneratorFactory {

        @Override
        public IdType type() {
            return FAKE;
        }

        @Override
        public IdGenerator create(GeneratorConfig config) {
            return new FakeIdGenerator();
        }
    }

    private static final class FakeIdGenerator implements IdGenerator {

        private final AtomicLong counter = new AtomicLong();

        @Override
        public IdType type() {
            return FAKE;
        }

        @Override
        public String generate() {
            return "fake-" + counter.incrementAndGet();
        }

        @Override
        public String generate(IdContext context) {
            return generate();
        }
    }
}
