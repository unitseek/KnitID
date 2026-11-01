package org.unitseek.knitid.worker.redis.autoconfigure;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

import org.unitseek.knitid.core.spi.WorkerIdAssignerProvider;
import org.unitseek.knitid.worker.redis.RedisWorkerIdAssignerProvider;
import org.unitseek.knitid.worker.redis.RedisWorkerIdProperties;

/**
 * Auto-configures the Redis WorkerId assigner provider.
 */
@AutoConfiguration(afterName = "org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration")
@ConditionalOnClass({ StringRedisTemplate.class, WorkerIdAssignerProvider.class })
@ConditionalOnProperty(prefix = "knitid.worker.redis", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(RedisWorkerIdProperties.class)
public class RedisWorkerIdAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "redisWorkerIdAssignerProvider")
    @ConditionalOnSingleCandidate(StringRedisTemplate.class)
    public WorkerIdAssignerProvider redisWorkerIdAssignerProvider(
            ObjectProvider<StringRedisTemplate> templates, RedisWorkerIdProperties properties) {
        return new RedisWorkerIdAssignerProvider(templates.getIfAvailable(), properties);
    }
}
