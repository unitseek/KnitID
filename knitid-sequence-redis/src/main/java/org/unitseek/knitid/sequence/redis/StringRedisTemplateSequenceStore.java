package org.unitseek.knitid.sequence.redis;

import java.time.Duration;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

/**
 * Atomic INCRBY + TTL-on-first-increment Redis store.
 */
final class StringRedisTemplateSequenceStore implements RedisSequenceStore {

    private static final String SCRIPT_TEXT = """
            local value = redis.call('INCRBY', KEYS[1], ARGV[1])
            if tonumber(ARGV[2]) > 0 then
              redis.call('PEXPIRE', KEYS[1], ARGV[2])
            end
            return value
            """;

    private static final DefaultRedisScript<Long> SCRIPT = new DefaultRedisScript<>(SCRIPT_TEXT, Long.class);

    private final StringRedisTemplate template;

    StringRedisTemplateSequenceStore(StringRedisTemplate template) {
        this.template = template;
    }

    @Override
    public long increment(String key, long delta, Duration ttl) {
        if (template == null) {
            throw new IllegalStateException(
                    "Redis sequence provider requires a StringRedisTemplate; add spring-boot-starter-data-redis");
        }
        Long value = template.execute(SCRIPT, List.of(key), String.valueOf(delta), String.valueOf(ttl.toMillis()));
        if (value == null) {
            throw new IllegalStateException("Redis INCRBY returned no value for key " + key);
        }
        return value;
    }
}
