package org.unitseek.knitid.autoconfigure.endpoint;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import org.unitseek.knitid.autoconfigure.KnitIdAutoConfiguration;
import org.unitseek.knitid.autoconfigure.KnitIdConfigEndpointAutoConfiguration;
import org.unitseek.knitid.generator.uuid7.autoconfigure.Uuid7AutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class KnitIdConfigEndpointAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    Uuid7AutoConfiguration.class, KnitIdAutoConfiguration.class,
                    KnitIdConfigEndpointAutoConfiguration.class))
            .withPropertyValues("knitid.default-type=UUID7");

    @Test
    void registersSanitizedConfigEndpoint() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(KnitIdConfigEndpoint.class);
            assertThat(context.getBean(KnitIdConfigEndpoint.class).diagnostics().maxBatchSize())
                    .isEqualTo(10_000);
        });
    }
}
