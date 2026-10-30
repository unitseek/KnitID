package org.unitseek.knitid.generator.ksuid.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import org.unitseek.knitid.generator.ksuid.KsuidGenerator;
import org.unitseek.knitid.generator.ksuid.KsuidIdGeneratorFactory;
import org.unitseek.knitid.generator.ksuid.KsuidIdTypeParser;

/**
 * Registers KSUID generation and parsing.
 */
@AutoConfiguration
@ConditionalOnClass(KsuidGenerator.class)
public class KsuidAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public KsuidIdGeneratorFactory ksuidIdGeneratorFactory() {
        return new KsuidIdGeneratorFactory();
    }

    @Bean
    @ConditionalOnMissingBean
    public KsuidIdTypeParser ksuidIdTypeParser() {
        return new KsuidIdTypeParser();
    }
}
