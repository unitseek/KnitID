package org.unitseek.knitid.autoconfigure;

import java.util.Map;
import java.util.Optional;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import org.unitseek.knitid.autoconfigure.parser.KnitIdParserAutoConfiguration;
import org.unitseek.knitid.core.api.IdType;
import org.unitseek.knitid.core.parser.IdTypeParser;
import org.unitseek.knitid.core.spi.GeneratorConfig;
import org.unitseek.knitid.generator.biz.BizConfig;
import org.unitseek.knitid.generator.biz.BizTemplateIdTypeParser;

/**
 * Auto-binds a BIZ template parser to the configured BIZ generator.
 *
 * @since 1.0.0
 */
@AutoConfiguration(before = KnitIdParserAutoConfiguration.class)
@ConditionalOnClass(name = "org.unitseek.knitid.generator.biz.BizTemplateIdTypeParser")
@EnableConfigurationProperties(KnitIdProperties.class)
public class ConfiguredBizParserAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "knitidBizTemplateParser")
    public IdTypeParser knitidBizTemplateParser(KnitIdProperties properties) {
        return configuredGenerator(properties, IdType.BIZ)
                .map(config -> (IdTypeParser) new BizTemplateIdTypeParser(BizConfig.from(config)))
                .orElse(null);
    }

    private static Optional<GeneratorConfig> configuredGenerator(KnitIdProperties properties, IdType wanted) {
        String defaultName = properties.getDefaultGenerator();
        if (defaultName != null) {
            Optional<GeneratorConfig> selected = config(properties, defaultName, wanted);
            if (selected.isPresent()) {
                return selected;
            }
        }
        for (String name : properties.getGenerators().keySet()) {
            Optional<GeneratorConfig> candidate = config(properties, name, wanted);
            if (candidate.isPresent()) {
                return candidate;
            }
        }
        return Optional.empty();
    }

    private static Optional<GeneratorConfig> config(KnitIdProperties properties, String name, IdType wanted) {
        Map<String, Object> raw = properties.getGenerators().get(name);
        if (raw == null || Boolean.FALSE.equals(raw.get("enabled"))) {
            return Optional.empty();
        }
        Object typeValue = raw.get("type");
        if (typeValue == null) {
            return Optional.empty();
        }
        try {
            if (!IdType.of(String.valueOf(typeValue)).equals(wanted)) {
                return Optional.empty();
            }
        }
        catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
        return Optional.of(IdGeneratorRegistryBuilder.generatorConfig(name, wanted, raw));
    }
}
