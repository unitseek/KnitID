package org.unitseek.knitid.core.spi;

import java.time.Duration;

/**
 * WorkerId allocation SPI.
 */
public interface WorkerIdAssigner {

    /**
     * Acquire a worker identity. May block; failures must throw
     * {@link org.unitseek.knitid.core.exception.WorkerIdUnavailableException}.
     */
    WorkerId assign();

    /**
     * Whether this assigner holds a renewable lease that must be renewed
     * periodically and released on shutdown.
     */
    default boolean renewable() {
        return false;
    }

    /**
     * Renewal interval suggested by the assigner, or {@code null} to use the
     * generator configuration.
     */
    default Duration renewalInterval() {
        return null;
    }

    default void renew() {
    }

    default void release(WorkerId workerId) {
    }
}
