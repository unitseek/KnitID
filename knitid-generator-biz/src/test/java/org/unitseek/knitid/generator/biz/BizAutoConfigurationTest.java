package org.unitseek.knitid.generator.biz;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import org.unitseek.knitid.generator.biz.autoconfigure.BizAutoConfiguration;

import java.util.Optional;

import org.unitseek.knitid.core.api.IdContext;
import org.unitseek.knitid.core.api.IdType;
import org.unitseek.knitid.core.spi.GeneratorConfig;

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

    @Test
    void memorySequenceIsSharedAcrossGeneratorsFromSameFactory() {
        runner.run(context -> {
            BizIdGeneratorFactory factory = context.getBean(BizIdGeneratorFactory.class);
            GeneratorConfig config = new GeneratorConfig() {
                @Override public String name() { return "order"; }
                @Override public IdType type() { return IdType.BIZ; }
                @Override public Optional<Object> get(String key) {
                    return switch (key) {
                        case "biz.template" -> Optional.of("{seq:4}");
                        case "biz.zone-id" -> Optional.of("UTC");
                        case "biz.sequence-provider" -> Optional.of("memory");
                        case "biz.reset" -> Optional.of("NEVER");
                        case "biz.check-algorithm" -> Optional.of("NONE");
                        default -> Optional.empty();
                    };
                }
            };
            assertThat(factory.create(config).generate()).isEqualTo("0000");
            assertThat(factory.create(config).generate()).isEqualTo("0001");
        });
    }
}
