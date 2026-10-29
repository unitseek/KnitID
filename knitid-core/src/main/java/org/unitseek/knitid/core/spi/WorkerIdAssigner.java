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

    default void renew() {
    }

    default void release(WorkerId workerId) {
    }
}
