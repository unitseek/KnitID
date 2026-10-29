package org.unitseek.knitid.autoconfigure;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.beans.factory.ObjectProvider;

import org.unitseek.knitid.core.api.IdGenerator;
import org.unitseek.knitid.core.api.IdGeneratorRegistry;
import org.unitseek.knitid.core.spi.IdGeneratorDecorator;
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
            ObjectProvider<IdGeneratorFactory> factories,
            ObjectProvider<IdGeneratorDecorator> decorators) {
        org.unitseek.knitid.core.api.IdGeneratorConstants.configureMaxBatchSize(
                properties.getLimits().getMaxBatchSize());
        return new IdGeneratorRegistryBuilder(properties, factories, decorators.orderedStream().toList()).build();
    }

    @Bean
    @Primary
    @ConditionalOnMissingBean(IdGenerator.class)
    public IdGenerator knitidDefaultIdGenerator(IdGeneratorRegistry registry) {
        return new DelegatingIdGenerator(registry);
    }

    @Bean
    public DisposableBean knitIdBatchSizeRestorer() {
        return org.unitseek.knitid.core.api.IdGeneratorConstants::resetMaxBatchSize;
    }

    @Bean
    public DisposableBean knitIdGeneratorCloser(IdGeneratorRegistry registry) {
        return () -> {
            // Only close generators created by this auto-configuration, not a user-supplied registry.
            if (!(registry instanceof SimpleIdGeneratorRegistry)) {
                return;
            }
            for (IdGenerator generator : java.util.List.copyOf(registry.asMap().values())) {
                if (generator instanceof AutoCloseable closeable) {
                    try {
                        closeable.close();
                    }
                    catch (Exception ex) {
                        // Best-effort shutdown; a failed close must not prevent other generators from closing.
                    }
                }
            }
        };
    }
}
