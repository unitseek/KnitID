package org.unitseek.knitid.worker.zookeeper;

import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;

import org.unitseek.knitid.core.exception.IdConfigurationException;
import org.unitseek.knitid.core.exception.IdErrorCodes;
import org.unitseek.knitid.core.exception.WorkerIdUnavailableException;
import org.unitseek.knitid.core.spi.WorkerIdAssigner;
import org.unitseek.knitid.core.spi.WorkerIdAssignerProvider;

/**
 * Exposes the ZooKeeper WorkerId assigner under the name {@code zookeeper}.
 */
public final class ZookeeperWorkerIdAssignerProvider implements WorkerIdAssignerProvider {

    public static final String NAME = "zookeeper";

    private static final int MAX_SUPPORTED_WORKER_ID_BITS = 16;

    private final ZookeeperWorkerIdProperties properties;

    public ZookeeperWorkerIdAssignerProvider(ZookeeperWorkerIdProperties properties) {
        this.properties = properties;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public WorkerIdAssigner create(int datacenterId, int workerIdBits) {
        if (datacenterId < 0) {
            throw new IdConfigurationException(
                    IdErrorCodes.CONFIG_INVALID, "ksid.datacenter-id must be >= 0");
        }
        if (workerIdBits < 1 || workerIdBits > MAX_SUPPORTED_WORKER_ID_BITS) {
            throw new IdConfigurationException(
                    IdErrorCodes.CONFIG_INVALID,
                    "ZooKeeper WorkerId assigner supports worker-id-bits between 1 and "
                            + MAX_SUPPORTED_WORKER_ID_BITS + ": " + workerIdBits);
        }
        validateProperties();
        CuratorFramework client = buildClient();
        try {
            client.start();
            int timeout = (int) Math.min(Integer.MAX_VALUE, properties.getConnectionTimeout().toMillis());
            if (!client.blockUntilConnected(timeout, TimeUnit.MILLISECONDS)) {
                throw new WorkerIdUnavailableException(
                        IdErrorCodes.WORKER_UNAVAILABLE,
                        "Could not connect to ZooKeeper within " + properties.getConnectionTimeout()
                                + ": " + properties.getConnectString());
            }
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            client.close();
            throw new WorkerIdUnavailableException(
                    IdErrorCodes.WORKER_UNAVAILABLE, "Interrupted while connecting to ZooKeeper", ex);
        }
        catch (RuntimeException ex) {
            client.close();
            throw ex;
        }
        int maxWorkerId = (1 << workerIdBits) - 1;
        return new ZookeeperWorkerIdAssigner(client, datacenterId, maxWorkerId);
    }

    private void validateProperties() {
        if (properties.getConnectString() == null || properties.getConnectString().isBlank()) {
            throw new IdConfigurationException(
                    IdErrorCodes.CONFIG_INVALID, "knitid.worker.zookeeper.connect-string is required");
        }
        if (properties.getNamespace() == null || properties.getNamespace().isBlank()) {
            throw new IdConfigurationException(
                    IdErrorCodes.CONFIG_INVALID, "knitid.worker.zookeeper.namespace must not be blank");
        }
        if (properties.getSessionTimeout() == null || properties.getSessionTimeout().isNegative()
                || properties.getSessionTimeout().isZero()) {
            throw new IdConfigurationException(
                    IdErrorCodes.CONFIG_INVALID, "knitid.worker.zookeeper.session-timeout must be positive");
        }
        if (properties.getConnectionTimeout() == null || properties.getConnectionTimeout().isNegative()
                || properties.getConnectionTimeout().isZero()) {
            throw new IdConfigurationException(
                    IdErrorCodes.CONFIG_INVALID,
                    "knitid.worker.zookeeper.connection-timeout must be positive");
        }
        if (properties.getSessionTimeout().toMillis() < 1
                || properties.getSessionTimeout().toMillis() > Integer.MAX_VALUE) {
            throw new IdConfigurationException(
                    IdErrorCodes.CONFIG_INVALID,
                    "knitid.worker.zookeeper.session-timeout must be between 1ms and Integer.MAX_VALUE ms");
        }
        if (properties.getConnectionTimeout().toMillis() < 1
                || properties.getConnectionTimeout().toMillis() > Integer.MAX_VALUE) {
            throw new IdConfigurationException(
                    IdErrorCodes.CONFIG_INVALID,
                    "knitid.worker.zookeeper.connection-timeout must be between 1ms and Integer.MAX_VALUE ms");
        }
    }

    private static int millis(java.time.Duration duration) {
        long millis = duration.toMillis();
        return (int) Math.max(1L, Math.min(Integer.MAX_VALUE, millis));
    }

    private CuratorFramework buildClient() {
        return CuratorFrameworkFactory.builder()
                .connectString(properties.getConnectString())
                .namespace(properties.getNamespace())
                .sessionTimeoutMs(millis(properties.getSessionTimeout()))
                .connectionTimeoutMs(millis(properties.getConnectionTimeout()))
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .build();
    }
}
