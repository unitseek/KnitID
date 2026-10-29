package org.unitseek.knitid.worker.redis;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * {@link RedisLeaseStore} backed by {@link StringRedisTemplate}.
 */
final class StringRedisTemplateLeaseStore implements RedisLeaseStore {

    private final StringRedisTemplate template;

    StringRedisTemplateLeaseStore(StringRedisTemplate template) {
        this.template = template;
    }

    @Override
    public boolean tryAcquire(String key, String token, Duration ttl) {
        return Boolean.TRUE.equals(template.opsForValue().setIfAbsent(key, token, ttl));
    }

    @Override
    public boolean renew(String key, Duration ttl) {
        return Boolean.TRUE.equals(template.expire(key, ttl));
    }

    @Override
    public boolean release(String key) {
        return Boolean.TRUE.equals(template.delete(key));
    }
}
