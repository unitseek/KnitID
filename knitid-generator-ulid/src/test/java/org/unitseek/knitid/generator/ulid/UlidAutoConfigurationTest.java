package org.unitseek.knitid.generator.ulid;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import org.unitseek.knitid.generator.ulid.autoconfigure.UlidAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class UlidAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(UlidAutoConfiguration.class));

    @Test
    void registersUlidFactory() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(UlidIdGeneratorFactory.class);
            assertThat(context.getBean(UlidIdGeneratorFactory.class).type().value()).isEqualTo("ULID");
        });
    }
}
