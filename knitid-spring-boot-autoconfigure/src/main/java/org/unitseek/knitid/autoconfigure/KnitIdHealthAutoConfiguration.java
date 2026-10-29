package org.unitseek.knitid.autoconfigure;

import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import org.unitseek.knitid.core.api.IdGeneratorRegistry;

/**
 * Actuator health integration for KnitID.
 */
@AutoConfiguration(after = KnitIdAutoConfiguration.class)
@ConditionalOnClass(HealthIndicator.class)
@ConditionalOnBean({ IdGeneratorRegistry.class, KnitIdProperties.class })
@ConditionalOnProperty(prefix = "management.health.knitid", name = "enabled", matchIfMissing = true)
public class KnitIdHealthAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "knitIdHealthIndicator")
    public KnitIdHealthIndicator knitIdHealthIndicator(IdGeneratorRegistry registry, KnitIdProperties properties) {
        return new KnitIdHealthIndicator(registry, properties);
    }
}
