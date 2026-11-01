package org.unitseek.knitid.autoconfigure;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import org.unitseek.knitid.autoconfigure.endpoint.KnitIdConfigEndpoint;
import org.unitseek.knitid.core.api.IdGeneratorRegistry;

/**
 * Registers the sanitized KnitID configuration diagnostics endpoint.
 *
 * @since 1.0.0
 */
@AutoConfiguration(after = KnitIdAutoConfiguration.class)
@ConditionalOnClass(Endpoint.class)
@ConditionalOnBean(IdGeneratorRegistry.class)
@ConditionalOnProperty(prefix = "management.endpoint.knitid-config", name = "enabled", matchIfMissing = true)
public class KnitIdConfigEndpointAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public KnitIdConfigEndpoint knitIdConfigEndpoint(KnitIdProperties properties, IdGeneratorRegistry registry) {
        return new KnitIdConfigEndpoint(properties, registry);
    }
}
