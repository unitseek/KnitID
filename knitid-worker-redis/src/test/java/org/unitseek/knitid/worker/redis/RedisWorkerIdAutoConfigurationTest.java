package org.unitseek.knitid.worker.redis;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.StringRedisTemplate;

import org.unitseek.knitid.core.spi.WorkerIdAssignerProvider;
import org.unitseek.knitid.worker.redis.autoconfigure.RedisWorkerIdAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RedisWorkerIdAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RedisWorkerIdAutoConfiguration.class))
            .withBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class));

    @Test
    void registersRedisAssignerProvider() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(WorkerIdAssignerProvider.class);
            WorkerIdAssignerProvider provider = context.getBean(WorkerIdAssignerProvider.class);
            assertThat(provider.name()).isEqualTo("redis");
            assertThat(provider.create(0, 5)).isNotNull();
        });
    }

    @Test
    void canBeDisabled() {
        runner.withPropertyValues("knitid.worker.redis.enabled=false").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(WorkerIdAssignerProvider.class);
        });
    }
}
