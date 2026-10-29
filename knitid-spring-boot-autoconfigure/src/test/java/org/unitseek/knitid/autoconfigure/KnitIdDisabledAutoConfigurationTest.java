package org.unitseek.knitid.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import org.unitseek.knitid.autoconfigure.parser.KnitIdParserAutoConfiguration;
import org.unitseek.knitid.core.api.IdGeneratorRegistry;
import org.unitseek.knitid.core.parser.IdParser;

import static org.assertj.core.api.Assertions.assertThat;

class KnitIdDisabledAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    KnitIdAutoConfiguration.class,
                    KnitIdHealthAutoConfiguration.class,
                    KnitIdEndpointAutoConfiguration.class,
                    KnitIdParserAutoConfiguration.class))
            .withPropertyValues("knitid.enabled=false");

    @Test
    void disablesRegistryHealthAndEndpointWithoutFailing() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(IdGeneratorRegistry.class);
            assertThat(context).doesNotHaveBean(KnitIdHealthIndicator.class);
            assertThat(context).doesNotHaveBean("knitIdEndpoint");
            assertThat(context).hasSingleBean(IdParser.class);
        });
    }
}
