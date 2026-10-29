package org.unitseek.knitid.autoconfigure.endpoint;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import org.unitseek.knitid.autoconfigure.KnitIdAutoConfiguration;
import org.unitseek.knitid.autoconfigure.KnitIdEndpointAutoConfiguration;
import org.unitseek.knitid.generator.uuid7.autoconfigure.Uuid7AutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class KnitIdEndpointAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    Uuid7AutoConfiguration.class, KnitIdAutoConfiguration.class, KnitIdEndpointAutoConfiguration.class));

    @Test
    void registersEndpointWhenActuatorIsAvailable() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(KnitIdEndpoint.class);
            KnitIdEndpoint.KnitIdEndpointInfo info = context.getBean(KnitIdEndpoint.class).info();
            assertThat(info.defaultType()).isEqualTo("UUID7");
            assertThat(info.generators()).hasSize(1);
        });
    }
}
