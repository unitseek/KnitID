package org.unitseek.knitid.autoconfigure;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import org.unitseek.knitid.autoconfigure.endpoint.KnitIdEndpoint;
import org.unitseek.knitid.core.api.IdGeneratorRegistry;

/**
 * Registers the KnitID actuator endpoint.
 */
@AutoConfiguration(after = KnitIdAutoConfiguration.class)
@ConditionalOnClass(Endpoint.class)
@ConditionalOnProperty(prefix = "management.endpoint.knitid", name = "enabled", matchIfMissing = true)
public class KnitIdEndpointAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public KnitIdEndpoint knitIdEndpoint(IdGeneratorRegistry registry) {
        return new KnitIdEndpoint(registry);
    }
}
