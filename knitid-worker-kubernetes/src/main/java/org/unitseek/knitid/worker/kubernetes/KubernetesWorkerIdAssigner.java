package org.unitseek.knitid.worker.kubernetes;

import org.unitseek.knitid.core.exception.IdErrorCodes;
import org.unitseek.knitid.core.exception.WorkerIdUnavailableException;
import org.unitseek.knitid.core.spi.WorkerId;
import org.unitseek.knitid.core.spi.WorkerIdAssigner;

/**
 * WorkerId derived from a Kubernetes StatefulSet pod ordinal.
 *
 * <p>This assigner does not own a distributed lease: uniqueness is provided by
 * the StatefulSet identity (one ordinal per pod at any time).</p>
 */
final class KubernetesWorkerIdAssigner implements WorkerIdAssigner {

    private final int datacenterId;

    private final int workerId;

    private final String podName;

    KubernetesWorkerIdAssigner(int datacenterId, int workerId, String podName) {
        this.datacenterId = datacenterId;
        this.workerId = workerId;
        this.podName = podName;
    }

    @Override
    public WorkerId assign() {
        if (workerId < 0) {
            throw new WorkerIdUnavailableException(
                    IdErrorCodes.WORKER_UNAVAILABLE,
                    "Invalid Kubernetes WorkerId for pod " + podName + ": " + workerId);
        }
        return new WorkerId(datacenterId, workerId);
    }

    String podName() {
        return podName;
    }
}
