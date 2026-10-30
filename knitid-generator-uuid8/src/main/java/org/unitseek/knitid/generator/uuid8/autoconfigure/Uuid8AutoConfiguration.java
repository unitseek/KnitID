package org.unitseek.knitid.generator.uuid8.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import org.unitseek.knitid.generator.uuid8.Uuid8Generator;
import org.unitseek.knitid.generator.uuid8.Uuid8IdGeneratorFactory;

/**
 * Registers the UUIDv8 generator factory.
 */
@AutoConfiguration
@ConditionalOnClass(Uuid8Generator.class)
public class Uuid8AutoConfiguration {

    /**
     * Registers the UUIDv8 generator factory.
     *
     * @return UUIDv8 factory
     */
    @Bean
    @ConditionalOnMissingBean
    public Uuid8IdGeneratorFactory uuid8IdGeneratorFactory() {
        return new Uuid8IdGeneratorFactory();
    }
}
