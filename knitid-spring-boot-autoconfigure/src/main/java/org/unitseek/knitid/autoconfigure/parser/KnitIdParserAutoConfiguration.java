package org.unitseek.knitid.autoconfigure.parser;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import org.unitseek.knitid.core.parser.IdParser;
import org.unitseek.knitid.core.parser.IdTypeParser;

/**
 * Registers the composite ID parser. User-provided {@link IdTypeParser} beans
 * run before the built-in format parsers.
 */
@AutoConfiguration
public class KnitIdParserAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(IdParser.class)
    public IdParser idParser(ObjectProvider<IdTypeParser> parsers) {
        List<IdTypeParser> ordered = new ArrayList<>(parsers.orderedStream().toList());
        ordered.add(new Uuid7IdTypeParser());
        ordered.add(new Uuid8IdTypeParser());
        ordered.add(new UlidIdTypeParser());
        ordered.add(new KsidNumericIdTypeParser());
        return new SimpleIdParser(ordered);
    }
}
