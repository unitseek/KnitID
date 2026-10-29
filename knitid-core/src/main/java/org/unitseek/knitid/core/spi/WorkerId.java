package org.unitseek.knitid.core.spi;

/**
 * Worker identity used by KSID.
 */
public record WorkerId(int datacenterId, int workerId) {

    public WorkerId {
        if (datacenterId < 0) {
            throw new IllegalArgumentException("datacenterId must be >= 0: " + datacenterId);
        }
        if (workerId < 0) {
            throw new IllegalArgumentException("workerId must be >= 0: " + workerId);
        }
    }
}
