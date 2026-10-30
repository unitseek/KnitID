package org.unitseek.knitid.autoconfigure;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.unitseek.knitid.core.api.IdContext;
import org.unitseek.knitid.core.api.IdGenerator;
import org.unitseek.knitid.core.api.IdType;
import org.unitseek.knitid.core.spi.GeneratorConfig;
import org.unitseek.knitid.core.spi.IdGeneratorFactory;

import static org.assertj.core.api.Assertions.assertThat;

class NumericMapFlattenTest {

    private static final IdType CUSTOM = IdType.of("CUSTOM");

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(KnitIdAutoConfiguration.class))
            .withUserConfiguration(FakeConfiguration.class)
            .withPropertyValues(
                    "knitid.generators.custom.type=CUSTOM",
                    "knitid.generators.custom.custom.weights.0=10",
                    "knitid.generators.custom.custom.weights.1=20");

    @Test
    void preservesLegacyDottedKeysForIndexedMaps() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            GeneratorConfig config = FakeFactory.captured.get();
            assertThat(config).isNotNull();
            assertThat(config.get("custom.weights.0")).contains("10");
            assertThat(config.get("custom.weights.1")).contains("20");
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class FakeConfiguration {
        @Bean
        IdGeneratorFactory fakeFactory() {
            return new FakeFactory();
        }
    }

    private static final class FakeFactory implements IdGeneratorFactory {
        private static final AtomicReference<GeneratorConfig> captured = new AtomicReference<>();

        @Override
        public IdType type() {
            return CUSTOM;
        }

        @Override
        public IdGenerator create(GeneratorConfig config) {
            captured.set(config);
            return new IdGenerator() {
                @Override public IdType type() { return CUSTOM; }
                @Override public String generate() { return "custom"; }
                @Override public String generate(IdContext context) { return generate(); }
            };
        }
    }
}
