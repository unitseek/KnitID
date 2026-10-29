package org.unitseek.knitid.sequence.redis;

import java.time.Duration;
import java.util.Objects;

import org.springframework.data.redis.core.StringRedisTemplate;

import org.unitseek.knitid.core.exception.IdErrorCodes;
import org.unitseek.knitid.core.exception.SequenceUnavailableException;
import org.unitseek.knitid.core.spi.SequenceProvider;

/**
 * Redis backed sequence provider for multi-instance business identifiers.
 *
 * <p>Each scope maps to one Redis key. INCRBY is atomic, so multiple
 * application instances share the same sequence. The first increment sets the
 * configured TTL, allowing daily scoped keys to expire automatically.</p>
 */
public final class RedisSequenceProvider implements SequenceProvider {

    private final RedisSequenceStore store;

    private final String keyPrefix;

    private final Duration ttl;

    public RedisSequenceProvider(StringRedisTemplate template, String keyPrefix, Duration ttl) {
        this(new StringRedisTemplateSequenceStore(template), keyPrefix, ttl);
    }

    RedisSequenceProvider(RedisSequenceStore store, String keyPrefix, Duration ttl) {
        if (keyPrefix == null || keyPrefix.isBlank()) {
            throw new IllegalArgumentException("keyPrefix must not be blank");
        }
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("ttl must be positive");
        }
        this.store = Objects.requireNonNull(store, "store must not be null");
        this.keyPrefix = keyPrefix;
        this.ttl = ttl;
    }

    @Override
    public long next(String scope) {
        return increment(scope, 1) - 1;
    }

    @Override
    public long[] next(String scope, int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("count must be > 0: " + count);
        }
        long end = increment(scope, count);
        long start = end - count;
        long[] values = new long[count];
        for (int i = 0; i < count; i++) {
            values[i] = start + i;
        }
        return values;
    }

    private long increment(String scope, long delta) {
        if (scope == null || scope.isBlank()) {
            throw new IllegalArgumentException("scope must not be blank");
        }
        String key = keyPrefix + ":" + scope;
        try {
            return store.increment(key, delta, ttl);
        }
        catch (RuntimeException ex) {
            throw new SequenceUnavailableException(
                    IdErrorCodes.SEQUENCE_UNAVAILABLE,
                    "Redis sequence unavailable for key " + key, ex);
        }
    }
}
