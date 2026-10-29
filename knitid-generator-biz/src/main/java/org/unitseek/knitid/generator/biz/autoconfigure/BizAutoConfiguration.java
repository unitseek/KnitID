package org.unitseek.knitid.generator.biz.autoconfigure;

import java.time.Clock;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import org.unitseek.knitid.core.exception.IdConfigurationException;
import org.unitseek.knitid.core.exception.IdErrorCodes;
import org.unitseek.knitid.core.spi.SequenceProvider;
import org.unitseek.knitid.generator.biz.BizIdGenerator;
import org.unitseek.knitid.generator.biz.BizIdGeneratorFactory;
import org.unitseek.knitid.generator.biz.MemorySequenceProvider;

/**
 * Registers the business-ID generator factory.
 */
@AutoConfiguration
@ConditionalOnClass(BizIdGenerator.class)
public class BizAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public BizIdGeneratorFactory bizIdGeneratorFactory(ListableBeanFactory beanFactory) {
        return new BizIdGeneratorFactory(Clock.systemUTC(), name -> {
            if ("memory".equalsIgnoreCase(name)) {
                return new MemorySequenceProvider();
            }
            try {
                return beanFactory.getBean(name, SequenceProvider.class);
            }
            catch (RuntimeException ex) {
                throw new IdConfigurationException(
                        IdErrorCodes.CONFIG_INVALID,
                        "SequenceProvider bean not found: " + name + " (" + ex.getMessage() + ")");
            }
        });
    }
}
