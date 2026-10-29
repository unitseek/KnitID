package org.unitseek.knitid.autoconfigure;

import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import org.unitseek.knitid.autoconfigure.metrics.IdGeneratorMetrics;
import org.unitseek.knitid.autoconfigure.metrics.KnitIdGaugeBinder;
import org.unitseek.knitid.core.api.IdGeneratorRegistry;
import org.unitseek.knitid.core.spi.IdGeneratorDecorator;

/**
 * Micrometer integration: generation timers/errors/batch sizes and WorkerId gauges.
 */
@AutoConfiguration(after = KnitIdAutoConfiguration.class)
@ConditionalOnClass(MeterRegistry.class)
public class KnitIdMetricsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "knitIdMetricsDecorator")
    public IdGeneratorDecorator knitIdMetricsDecorator(ObjectProvider<MeterRegistry> registries) {
        MeterRegistry registry = registries.getIfAvailable();
        if (registry == null) {
            return (generator, name) -> generator;
        }
        return (generator, name) -> IdGeneratorMetrics.wrap(generator, name, registry);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(IdGeneratorRegistry.class)
    public KnitIdGaugeBinder knitIdGaugeBinder(IdGeneratorRegistry registry, KnitIdProperties properties,
            ObjectProvider<MeterRegistry> registries) {
        return new KnitIdGaugeBinder(registry, registries.getIfAvailable(),
                properties.getHealth().getBackwardsDownWindow());
    }
}
