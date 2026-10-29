package org.unitseek.knitid.sequence.redis;

import java.time.Duration;

/**
 * Atomic Redis sequence operation (isolated for testability).
 */
interface RedisSequenceStore {

    long increment(String key, long delta, Duration ttl);
}
