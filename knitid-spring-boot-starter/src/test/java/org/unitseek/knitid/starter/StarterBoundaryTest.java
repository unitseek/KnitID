package org.unitseek.knitid.starter;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import org.unitseek.knitid.autoconfigure.ConfiguredBizParserAutoConfiguration;
import org.unitseek.knitid.autoconfigure.ConfiguredKsidParserAutoConfiguration;
import org.unitseek.knitid.autoconfigure.KnitIdAutoConfiguration;
import org.unitseek.knitid.autoconfigure.parser.KnitIdParserAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StarterBoundaryTest {

    @Test
    void plainStarterCanStartWithoutGeneratorModules() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        KnitIdAutoConfiguration.class,
                        KnitIdParserAutoConfiguration.class,
                        ConfiguredKsidParserAutoConfiguration.class,
                        ConfiguredBizParserAutoConfiguration.class))
                .run(context -> org.assertj.core.api.Assertions.assertThat(context).hasNotFailed());
    }

    @Test
    void plainStarterDoesNotContainGeneratorModules() {
        assertThatThrownBy(() -> Class.forName("org.unitseek.knitid.generator.uuid7.Uuid7Generator"))
                .isInstanceOf(ClassNotFoundException.class);
        assertThatThrownBy(() -> Class.forName("org.unitseek.knitid.generator.ksuid.KsuidGenerator"))
                .isInstanceOf(ClassNotFoundException.class);
        assertThatThrownBy(() -> Class.forName("org.unitseek.knitid.generator.typeid.TypeIdGenerator"))
                .isInstanceOf(ClassNotFoundException.class);
    }
}
