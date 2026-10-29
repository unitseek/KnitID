package org.unitseek.knitid.worker.redis;

import java.time.Duration;

import org.unitseek.knitid.core.exception.IdErrorCodes;
import org.unitseek.knitid.core.exception.WorkerIdUnavailableException;
import org.unitseek.knitid.core.spi.WorkerId;
import org.unitseek.knitid.core.spi.WorkerIdAssigner;

/**
 * Redis lease based WorkerId assigner.
 *
 * <p>Slots are keys of the form {@code <prefix>:<datacenter>:<worker>} and are
 * acquired atomically with SET NX + TTL. The lease must be renewed before the
 * TTL expires; the KSID generator performs the periodic renewal.</p>
 */
final class RedisWorkerIdAssigner implements WorkerIdAssigner {

    private final RedisLeaseStore store;

    private final String keyPrefix;

    private final Duration leaseTtl;

    private final Duration renewInterval;

    private final int datacenterId;

    private final int maxWorkerId;

    private final String token;

    private volatile WorkerId workerId;

    RedisWorkerIdAssigner(RedisLeaseStore store, String keyPrefix, Duration leaseTtl, Duration renewInterval,
            int datacenterId, int maxWorkerId, String token) {
        this.store = store;
        this.keyPrefix = keyPrefix;
        this.leaseTtl = leaseTtl;
        this.renewInterval = renewInterval;
        this.datacenterId = datacenterId;
        this.maxWorkerId = maxWorkerId;
        this.token = token;
    }

    @Override
    public synchronized WorkerId assign() {
        WorkerId current = workerId;
        if (current != null) {
            return current;
        }
        try {
            for (int candidate = 0; candidate <= maxWorkerId; candidate++) {
                String key = key(datacenterId, candidate);
                if (store.tryAcquire(key, token, leaseTtl)) {
                    WorkerId assigned = new WorkerId(datacenterId, candidate);
                    workerId = assigned;
                    return assigned;
                }
            }
        }
        catch (RuntimeException ex) {
            throw new WorkerIdUnavailableException(
                    IdErrorCodes.WORKER_UNAVAILABLE,
                    "Failed to acquire a Redis WorkerId lease in datacenter " + datacenterId,
                    ex);
        }
        throw new WorkerIdUnavailableException(
                IdErrorCodes.WORKER_UNAVAILABLE,
                "No free Redis WorkerId slot in datacenter " + datacenterId
                        + " (max worker id " + maxWorkerId + ", key prefix " + keyPrefix + ")");
    }

    @Override
    public boolean renewable() {
        return true;
    }

    @Override
    public Duration renewalInterval() {
        return renewInterval;
    }

    @Override
    public synchronized void renew() {
        WorkerId current = workerId;
        if (current == null) {
            throw new WorkerIdUnavailableException(
                    IdErrorCodes.WORKER_UNAVAILABLE, "Redis WorkerId has not been assigned yet");
        }
        String key = key(current.datacenterId(), current.workerId());
        try {
            if (!store.renew(key, leaseTtl)) {
                throw new WorkerIdUnavailableException(
                        IdErrorCodes.WORKER_LEASE_LOST,
                        "Redis WorkerId lease expired or was lost: " + key);
            }
        }
        catch (WorkerIdUnavailableException ex) {
            throw ex;
        }
        catch (RuntimeException ex) {
            throw new WorkerIdUnavailableException(
                    IdErrorCodes.WORKER_LEASE_LOST, "Failed to renew Redis WorkerId lease: " + key, ex);
        }
    }

    @Override
    public synchronized void release(WorkerId released) {
        if (released == null) {
            return;
        }
        store.release(key(released.datacenterId(), released.workerId()));
        WorkerId current = workerId;
        if (released.equals(current)) {
            workerId = null;
        }
    }

    private String key(int datacenter, int worker) {
        return keyPrefix + ":" + datacenter + ":" + worker;
    }
}
