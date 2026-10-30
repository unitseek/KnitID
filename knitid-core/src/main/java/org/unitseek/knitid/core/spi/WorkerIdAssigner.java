package org.unitseek.knitid.core.spi;

import java.time.Duration;

/**
 * WorkerId allocation SPI.
 *
 * @since 1.0.0
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

    /**
     * Renews a renewable lease. Default is a no-op.
     *
     * @throws org.unitseek.knitid.core.exception.WorkerIdUnavailableException if
     *         the lease cannot be renewed
     */
    default void renew() {
    }

    /**
     * Releases a lease/worker id. Default is a no-op.
     *
     * @param workerId worker id to release
     */
    default void release(WorkerId workerId) {
    }
}
