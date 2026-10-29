package org.unitseek.knitid.generator.biz;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import org.unitseek.knitid.generator.biz.autoconfigure.BizAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class BizAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BizAutoConfiguration.class));

    @Test
    void registersBizFactory() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(BizIdGeneratorFactory.class);
            assertThat(context.getBean(BizIdGeneratorFactory.class).type().value()).isEqualTo("BIZ");
        });
    }
}
