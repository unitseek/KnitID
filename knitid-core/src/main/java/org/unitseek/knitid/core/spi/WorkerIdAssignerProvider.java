package org.unitseek.knitid.core.spi;

/**
 * Provides named WorkerId assigners for integration modules (Redis, ZooKeeper,
 * Kubernetes, ...).
 *
 * <p>Algorithm modules resolve providers by the configured
 * {@code worker-id-assigner} name.</p>
 */
public interface WorkerIdAssignerProvider {

    String name();

    WorkerIdAssigner create(int datacenterId, int workerIdBits);
}
