package org.unitseek.knitid.autoconfigure.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import org.unitseek.knitid.autoconfigure.KnitIdAutoConfiguration;
import org.unitseek.knitid.autoconfigure.KnitIdMetricsAutoConfiguration;
import org.unitseek.knitid.core.api.IdGenerator;
import org.unitseek.knitid.core.api.IdGeneratorRegistry;
import org.unitseek.knitid.generator.uuid7.autoconfigure.Uuid7AutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class KnitIdMetricsAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    Uuid7AutoConfiguration.class, KnitIdAutoConfiguration.class, KnitIdMetricsAutoConfiguration.class))
            .withBean(SimpleMeterRegistry.class, SimpleMeterRegistry::new);

    @Test
    void generatesTimersForRegisteredGenerators() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            IdGeneratorRegistry registry = context.getBean(IdGeneratorRegistry.class);
            IdGenerator generator = registry.getDefault();
            generator.generate();
            assertThat(context.getBean(SimpleMeterRegistry.class)
                    .find("knitid.id.generation.duration").timer().count()).isEqualTo(1);
        });
    }
}
