package org.unitseek.knitid.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import org.unitseek.knitid.generator.biz.BizTemplateIdTypeParser;
import org.unitseek.knitid.generator.ksid.KsidLayoutIdTypeParser;

import static org.assertj.core.api.Assertions.assertThat;

class ConfiguredParserAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ConfiguredKsidParserAutoConfiguration.class,
                    ConfiguredBizParserAutoConfiguration.class))
            .withPropertyValues(
                    "knitid.generators.ksid-id.type=KSID",
                    "knitid.generators.ksid-id.ksid.worker-id=1",
                    "knitid.generators.biz-id.type=BIZ",
                    "knitid.generators.biz-id.biz.template={prefix}-{yyyyMMdd}-{seq:6}",
                    "knitid.generators.biz-id.biz.prefix=SAMPLE",
                    "knitid.generators.biz-id.biz.zone-id=UTC",
                    "knitid.generators.biz-id.biz.sequence-provider=memory");

    @Test
    void ignoresDisabledGenerators() {
        runner.withPropertyValues("knitid.generators.ksid-id.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(KsidLayoutIdTypeParser.class);
                });
    }

    @Test
    void bindsLayoutAndTemplateParsers() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(KsidLayoutIdTypeParser.class);
            assertThat(context).hasSingleBean(BizTemplateIdTypeParser.class);
        });
    }
}
