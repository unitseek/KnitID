package org.unitseek.knitid.worker.redis;

import java.time.Duration;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

/**
 * {@link RedisLeaseStore} backed by {@link StringRedisTemplate}.
 *
 * <p>Renew and release use atomic compare-and-set/delete Lua scripts so a
 * stale worker cannot mutate a lease now owned by someone else.</p>
 */
final class StringRedisTemplateLeaseStore implements RedisLeaseStore {

    private static final String RENEW_SCRIPT = """
            if redis.call('GET', KEYS[1]) == ARGV[1] then
              return redis.call('PEXPIRE', KEYS[1], ARGV[2])
            end
            return 0
            """;

    private static final String RELEASE_SCRIPT = """
            if redis.call('GET', KEYS[1]) == ARGV[1] then
              return redis.call('DEL', KEYS[1])
            end
            return 0
            """;

    private static final DefaultRedisScript<Long> RENEW = new DefaultRedisScript<>(RENEW_SCRIPT, Long.class);

    private static final DefaultRedisScript<Long> RELEASE = new DefaultRedisScript<>(RELEASE_SCRIPT, Long.class);

    private final StringRedisTemplate template;

    StringRedisTemplateLeaseStore(StringRedisTemplate template) {
        this.template = template;
    }

    @Override
    public boolean tryAcquire(String key, String token, Duration ttl) {
        return Boolean.TRUE.equals(template.opsForValue().setIfAbsent(key, token, ttl));
    }

    @Override
    public boolean renew(String key, String token, Duration ttl) {
        Long result = template.execute(RENEW, List.of(key), token, String.valueOf(ttl.toMillis()));
        return result != null && result > 0;
    }

    @Override
    public boolean release(String key, String token) {
        Long result = template.execute(RELEASE, List.of(key), token);
        return result != null && result > 0;
    }
}
