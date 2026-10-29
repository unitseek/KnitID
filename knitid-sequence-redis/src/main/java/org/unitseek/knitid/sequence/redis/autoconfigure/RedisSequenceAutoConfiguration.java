package org.unitseek.knitid.sequence.redis.autoconfigure;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

import org.unitseek.knitid.core.spi.SequenceProvider;
import org.unitseek.knitid.sequence.redis.RedisSequenceProperties;
import org.unitseek.knitid.sequence.redis.RedisSequenceProvider;

/**
 * Auto-configures the Redis sequence provider. The provider is available as
 * both {@code redis} and {@code redisSequenceProvider} bean names for use with
 * {@code knitid.generators.*.biz.sequence-provider}.
 */
@AutoConfiguration
@ConditionalOnClass({ StringRedisTemplate.class, SequenceProvider.class })
@ConditionalOnProperty(prefix = "knitid.sequence.redis", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(RedisSequenceProperties.class)
public class RedisSequenceAutoConfiguration {

    @Bean(name = { "redisSequenceProvider", "redis" })
    @ConditionalOnMissingBean(name = "redisSequenceProvider")
    public SequenceProvider redisSequenceProvider(ObjectProvider<StringRedisTemplate> templates,
            RedisSequenceProperties properties) {
        return new RedisSequenceProvider(templates.getIfAvailable(), properties.getKeyPrefix(), properties.getTtl());
    }
}
