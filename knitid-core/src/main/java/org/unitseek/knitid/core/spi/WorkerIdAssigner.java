package org.unitseek.knitid.core.spi;

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

    default void renew() {
    }

    default void release(WorkerId workerId) {
    }
}
