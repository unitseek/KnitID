package org.unitseek.knitid.generator.ksuid.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import org.unitseek.knitid.core.parser.IdTypeParser;
import org.unitseek.knitid.core.spi.IdGeneratorFactory;
import org.unitseek.knitid.generator.ksuid.KsuidIdGeneratorFactory;
import org.unitseek.knitid.generator.ksuid.KsuidIdTypeParser;

import static org.assertj.core.api.Assertions.assertThat;

class KsuidAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(KsuidAutoConfiguration.class));

    @Test
    void registersFactoryAndParser() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(IdGeneratorFactory.class);
            assertThat(context).hasSingleBean(IdTypeParser.class);
            assertThat(context.getBean(IdGeneratorFactory.class)).isInstanceOf(KsuidIdGeneratorFactory.class);
            assertThat(context.getBean(IdTypeParser.class)).isInstanceOf(KsuidIdTypeParser.class);
        });
    }
}
