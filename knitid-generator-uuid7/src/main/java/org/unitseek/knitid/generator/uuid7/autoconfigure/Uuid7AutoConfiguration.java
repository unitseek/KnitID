package org.unitseek.knitid.generator.uuid7.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import org.unitseek.knitid.generator.uuid7.Uuid7Generator;
import org.unitseek.knitid.generator.uuid7.Uuid7IdGeneratorFactory;

/**
 * Registers the UUIDv7 generator factory.
 */
@AutoConfiguration
@ConditionalOnClass(Uuid7Generator.class)
public class Uuid7AutoConfiguration {

    /**
     * Registers the UUIDv7 generator factory.
     *
     * @return UUIDv7 factory
     */
    @Bean
    @ConditionalOnMissingBean
    public Uuid7IdGeneratorFactory uuid7IdGeneratorFactory() {
        return new Uuid7IdGeneratorFactory();
    }
}
