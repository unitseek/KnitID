package org.unitseek.knitid.samples;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.unitseek.knitid.core.api.IdGenerator;
import org.unitseek.knitid.core.api.IdGeneratorRegistry;

/**
 * Prints one sample for each built-in generator when the sample application starts.
 */
@Configuration(proxyBeanMethods = false)
public class SampleConsoleConfiguration {

    @Bean
    CommandLineRunner knitIdConsoleSample(IdGeneratorRegistry registry) {
        return args -> {
            System.out.println();
            System.out.println("KnitID sample IDs (one per generator)");
            System.out.println("-------------------------------------");
            for (String name : List.of("uuid7", "uuid8", "ulid", "ksid", "biz", "ksuid", "typeid")) {
                IdGenerator generator = registry.get(name);
                System.out.printf("%-6s %-4s %s%n", name, generator.type(), generator.generate());
            }
            System.out.println("-------------------------------------");
            System.out.println();
        };
    }
}
