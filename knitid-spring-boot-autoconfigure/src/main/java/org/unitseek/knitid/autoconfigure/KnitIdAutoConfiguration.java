package org.unitseek.knitid.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.beans.factory.ObjectProvider;

import org.unitseek.knitid.core.api.IdGenerator;
import org.unitseek.knitid.core.api.IdGeneratorRegistry;
import org.unitseek.knitid.core.spi.IdGeneratorFactory;

/**
 * Central KnitID auto-configuration.
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "knitid", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(KnitIdProperties.class)
public class KnitIdAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public IdGeneratorRegistry idGeneratorRegistry(KnitIdProperties properties,
            ObjectProvider<IdGeneratorFactory> factories) {
        return new IdGeneratorRegistryBuilder(properties, factories).build();
    }

    @Bean
    @Primary
    @ConditionalOnMissingBean(IdGenerator.class)
    public IdGenerator knitidDefaultIdGenerator(IdGeneratorRegistry registry) {
        return new DelegatingIdGenerator(registry);
    }
}
