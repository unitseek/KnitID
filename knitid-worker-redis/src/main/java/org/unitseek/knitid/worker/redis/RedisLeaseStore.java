package org.unitseek.knitid.worker.redis;

import java.time.Duration;

/**
 * Minimal Redis lease operations (isolated for testability).
 *
 * <p>Every renew/release operation is scoped to the token that acquired the
 * lease so a stale owner can never extend or delete another owner's lease.</p>
 */
interface RedisLeaseStore {

    boolean tryAcquire(String key, String token, Duration ttl);

    boolean renew(String key, String token, Duration ttl);

    boolean release(String key, String token);
}
