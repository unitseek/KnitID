package org.unitseek.knitid.generator.ksuid.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import org.unitseek.knitid.generator.ksuid.KsuidGenerator;
import org.unitseek.knitid.generator.ksuid.KsuidIdGeneratorFactory;
import org.unitseek.knitid.generator.ksuid.KsuidIdTypeParser;

/**
 * Registers KSUID generation and parsing when the KSUID module is present.
 *
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnClass(KsuidGenerator.class)
public class KsuidAutoConfiguration {

    /**
     * Registers the KSUID generator factory.
     *
     * @return KSUID factory
     */
    @Bean
    @ConditionalOnMissingBean
    public KsuidIdGeneratorFactory ksuidIdGeneratorFactory() {
        return new KsuidIdGeneratorFactory();
    }

    /**
     * Registers the KSUID parser.
     *
     * @return KSUID parser
     */
    @Bean
    @ConditionalOnMissingBean
    public KsuidIdTypeParser ksuidIdTypeParser() {
        return new KsuidIdTypeParser();
    }
}
