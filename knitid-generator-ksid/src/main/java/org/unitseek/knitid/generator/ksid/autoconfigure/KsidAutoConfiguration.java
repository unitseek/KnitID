package org.unitseek.knitid.generator.ksid.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;

import org.unitseek.knitid.core.spi.WorkerIdAssignerProvider;
import org.unitseek.knitid.generator.ksid.KsidGenerator;
import org.unitseek.knitid.generator.ksid.KsidIdGeneratorFactory;

/**
 * Registers the KSID generator factory.
 */
@AutoConfiguration
@ConditionalOnClass(KsidGenerator.class)
public class KsidAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public KsidIdGeneratorFactory ksidIdGeneratorFactory(ObjectProvider<WorkerIdAssignerProvider> assignerProviders) {
        return new KsidIdGeneratorFactory(java.time.Clock.systemUTC(), assignerProviders.orderedStream().toList());
    }
}
