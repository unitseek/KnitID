package org.unitseek.knitid.generator.biz.autoconfigure;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    /**
     * Registers the business-ID generator factory and resolves custom
     * {@link SequenceProvider} beans by name.
     *
     * @param beanFactory bean factory used to look up sequence providers
     * @return business-ID factory
     */
    @Bean
    @ConditionalOnMissingBean
    public BizIdGeneratorFactory bizIdGeneratorFactory(ListableBeanFactory beanFactory) {
        Map<String, SequenceProvider> providers = new ConcurrentHashMap<>();
        return new BizIdGeneratorFactory(Clock.systemUTC(), name -> {
            if (name == null || name.isBlank()) {
                throw new IdConfigurationException(
                        IdErrorCodes.CONFIG_INVALID, "biz.sequence-provider must not be blank");
            }
            return providers.computeIfAbsent(name, key -> {
                if (beanFactory.containsBean(key)) {
                    return beanFactory.getBean(key, SequenceProvider.class);
                }
                if ("memory".equalsIgnoreCase(key)) {
                    return new MemorySequenceProvider();
                }
                throw new IdConfigurationException(
                        IdErrorCodes.CONFIG_INVALID, "SequenceProvider bean not found: " + key);
            });
        });
    }
}
