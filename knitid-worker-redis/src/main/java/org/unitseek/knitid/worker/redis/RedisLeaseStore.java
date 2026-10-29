package org.unitseek.knitid.worker.redis;

import java.time.Duration;

/**
 * Minimal Redis lease operations (isolated for testability).
 */
interface RedisLeaseStore {

    boolean tryAcquire(String key, String token, Duration ttl);

    boolean renew(String key, Duration ttl);

    boolean release(String key);
}
