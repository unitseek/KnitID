package org.unitseek.knitid.autoconfigure.parser;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import org.unitseek.knitid.core.api.IdType;
import org.unitseek.knitid.core.parser.IdParser;

import static org.assertj.core.api.Assertions.assertThat;

class KnitIdParserAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(KnitIdParserAutoConfiguration.class));

    @Test
    void registersCompositeParser() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(IdParser.class);
            IdParser parser = context.getBean(IdParser.class);
            assertThat(parser.parse("017f22e2-79b0-7cc3-98c4-dc0c0c07398f").type())
                    .isEqualTo(IdType.UUID7);
        });
    }
}
