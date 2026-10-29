package org.unitseek.knitid.generator.ulid.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import org.unitseek.knitid.generator.ulid.UlidGenerator;
import org.unitseek.knitid.generator.ulid.UlidIdGeneratorFactory;

/**
 * Registers the ULID generator factory.
 */
@AutoConfiguration
@ConditionalOnClass(UlidGenerator.class)
public class UlidAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public UlidIdGeneratorFactory ulidIdGeneratorFactory() {
        return new UlidIdGeneratorFactory();
    }
}
