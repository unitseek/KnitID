package org.unitseek.knitid.generator.typeid.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import org.unitseek.knitid.generator.typeid.TypeIdGenerator;
import org.unitseek.knitid.generator.typeid.TypeIdIdGeneratorFactory;
import org.unitseek.knitid.generator.typeid.TypeIdIdTypeParser;

/**
 * Registers TypeID generation and parsing when the TypeID module is present.
 *
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnClass(TypeIdGenerator.class)
public class TypeIdAutoConfiguration {

    /**
     * Registers the TypeID generator factory.
     *
     * @return TypeID factory
     */
    @Bean
    @ConditionalOnMissingBean
    public TypeIdIdGeneratorFactory typeIdIdGeneratorFactory() {
        return new TypeIdIdGeneratorFactory();
    }

    /**
     * Registers the TypeID parser.
     *
     * @return TypeID parser
     */
    @Bean
    @ConditionalOnMissingBean
    public TypeIdIdTypeParser typeIdIdTypeParser() {
        return new TypeIdIdTypeParser();
    }
}
