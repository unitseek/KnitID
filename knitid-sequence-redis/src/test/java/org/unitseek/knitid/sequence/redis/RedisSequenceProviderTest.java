package org.unitseek.knitid.sequence.redis;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.unitseek.knitid.core.exception.SequenceUnavailableException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RedisSequenceProviderTest {

    private static final Duration TTL = Duration.ofDays(2);

    @Test
    void sequencesArePerScopeAndStartAtZero() {
        InMemoryStore store = new InMemoryStore();
        RedisSequenceProvider provider = new RedisSequenceProvider(store, "knitid:seq", TTL);

        assertThat(provider.next("ORD:20260101")).isZero();
        assertThat(provider.next("ORD:20260101")).isEqualTo(1);
        assertThat(provider.next("ORD:20260102")).isZero();
        assertThat(provider.next("USR:20260101")).isZero();
        assertThat(store.keys).containsKeys("knitid:seq:ORD:20260101", "knitid:seq:ORD:20260102");
    }

    @Test
    void batchReturnsContiguousRange() {
        InMemoryStore store = new InMemoryStore();
        RedisSequenceProvider provider = new RedisSequenceProvider(store, "knitid:seq", TTL);

        assertThat(provider.next("ORD", 3)).containsExactly(0, 1, 2);
        assertThat(provider.next("ORD", 2)).containsExactly(3, 4);
    }

    @Test
    void passesConfiguredTtl() {
        InMemoryStore store = new InMemoryStore();
        RedisSequenceProvider provider = new RedisSequenceProvider(store, "knitid:seq", TTL);
        provider.next("ORD");
        assertThat(store.ttl).isEqualTo(TTL);
    }

    @Test
    void redisFailureMapsToSequenceUnavailable() {
        RedisSequenceProvider provider = new RedisSequenceProvider(
                (key, delta, ttl) -> {
                    throw new IllegalStateException("redis down");
                },
                "knitid:seq", TTL);
        assertThatThrownBy(() -> provider.next("ORD"))
                .isInstanceOf(SequenceUnavailableException.class)
                .hasMessageContaining("KNITID-2004");
    }

    @Test
    void rejectsInvalidArguments() {
        RedisSequenceProvider provider = new RedisSequenceProvider(new InMemoryStore(), "knitid:seq", TTL);
        assertThatThrownBy(() -> provider.next(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> provider.next("ORD", 0)).isInstanceOf(IllegalArgumentException.class);
    }

    private static final class InMemoryStore implements RedisSequenceStore {

        private final Map<String, Long> keys = new HashMap<>();

        private Duration ttl;

        @Override
        public long increment(String key, long delta, Duration ttl) {
            this.ttl = ttl;
            long next = keys.getOrDefault(key, 0L) + delta;
            keys.put(key, next);
            return next;
        }
    }
}
