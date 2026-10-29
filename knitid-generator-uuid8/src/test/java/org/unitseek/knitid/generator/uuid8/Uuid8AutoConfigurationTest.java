package org.unitseek.knitid.generator.uuid8;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import org.unitseek.knitid.generator.uuid8.autoconfigure.Uuid8AutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class Uuid8AutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(Uuid8AutoConfiguration.class));

    @Test
    void registersUuid8Factory() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(Uuid8IdGeneratorFactory.class);
            assertThat(context.getBean(Uuid8IdGeneratorFactory.class).type().value()).isEqualTo("UUID8");
        });
    }
}
