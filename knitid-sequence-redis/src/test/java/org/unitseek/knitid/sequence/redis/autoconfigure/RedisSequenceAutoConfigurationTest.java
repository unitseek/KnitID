package org.unitseek.knitid.sequence.redis.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.StringRedisTemplate;

import org.unitseek.knitid.core.spi.SequenceProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RedisSequenceAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RedisSequenceAutoConfiguration.class))
            .withBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class));

    @Test
    void registersRedisSequenceProviderUnderBothNames() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(SequenceProvider.class);
            assertThat(context).hasBean("redisSequenceProvider");
            assertThat(context).hasBean("redis");
        });
    }

    @Test
    void canBeDisabled() {
        runner.withPropertyValues("knitid.sequence.redis.enabled=false").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(SequenceProvider.class);
        });
    }
}
