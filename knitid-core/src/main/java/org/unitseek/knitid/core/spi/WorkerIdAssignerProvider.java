package org.unitseek.knitid.core.spi;

/**
 * Provides named WorkerId assigners for integration modules (Redis, ZooKeeper,
 * Kubernetes, ...).
 *
 * <p>Algorithm modules resolve providers by the configured
 * {@code worker-id-assigner} name.</p>
 *
 * @since 1.0.0
 */
public interface WorkerIdAssignerProvider {

    /**
     * Returns the provider name used in configuration.
     *
     * @return provider name
     */
    String name();

    /**
     * Creates a WorkerId assigner.
     *
     * @param datacenterId configured datacenter id
     * @param workerIdBits configured worker id width
     * @return assigner
     */
    WorkerIdAssigner create(int datacenterId, int workerIdBits);
}
