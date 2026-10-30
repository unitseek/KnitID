package org.unitseek.knitid.generator.typeid.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import org.unitseek.knitid.core.parser.IdTypeParser;
import org.unitseek.knitid.core.spi.IdGeneratorFactory;
import org.unitseek.knitid.generator.typeid.TypeIdIdGeneratorFactory;
import org.unitseek.knitid.generator.typeid.TypeIdIdTypeParser;

import static org.assertj.core.api.Assertions.assertThat;

class TypeIdAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TypeIdAutoConfiguration.class));

    @Test
    void registersFactoryAndParser() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(IdGeneratorFactory.class);
            assertThat(context).hasSingleBean(IdTypeParser.class);
            assertThat(context.getBean(IdGeneratorFactory.class)).isInstanceOf(TypeIdIdGeneratorFactory.class);
            assertThat(context.getBean(IdTypeParser.class)).isInstanceOf(TypeIdIdTypeParser.class);
        });
    }
}
