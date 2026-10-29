package org.unitseek.knitid.core.worker;

import java.util.Objects;

import org.unitseek.knitid.core.spi.WorkerId;
import org.unitseek.knitid.core.spi.WorkerIdAssigner;

/**
 * Fixed WorkerId from configuration.
 */
public final class FixedWorkerIdAssigner implements WorkerIdAssigner {

    private final WorkerId workerId;

    public FixedWorkerIdAssigner(int datacenterId, int workerId) {
        this(new WorkerId(datacenterId, workerId));
    }

    public FixedWorkerIdAssigner(WorkerId workerId) {
        this.workerId = Objects.requireNonNull(workerId, "workerId must not be null");
    }

    @Override
    public WorkerId assign() {
        return workerId;
    }
}
