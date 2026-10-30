package org.unitseek.knitid.core.worker;

import java.util.Objects;

import org.unitseek.knitid.core.spi.WorkerId;
import org.unitseek.knitid.core.spi.WorkerIdAssigner;

/**
 * Fixed WorkerId from configuration.
 *
 * @since 1.0.0
 */
public final class FixedWorkerIdAssigner implements WorkerIdAssigner {

    private final WorkerId workerId;

    /**
     * Creates an assigner from raw ids.
     *
     * @param datacenterId datacenter id
     * @param workerId worker id
     */
    public FixedWorkerIdAssigner(int datacenterId, int workerId) {
        this(new WorkerId(datacenterId, workerId));
    }

    /**
     * Creates an assigner from a WorkerId.
     *
     * @param workerId fixed worker id
     */
    public FixedWorkerIdAssigner(WorkerId workerId) {
        this.workerId = Objects.requireNonNull(workerId, "workerId must not be null");
    }

    /**
     * Returns the configured worker id.
     *
     * @return worker id
     */
    @Override
    public WorkerId assign() {
        return workerId;
    }
}
