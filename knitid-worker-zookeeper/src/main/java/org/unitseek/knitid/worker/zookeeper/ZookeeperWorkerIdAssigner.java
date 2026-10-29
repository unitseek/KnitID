package org.unitseek.knitid.worker.zookeeper;

import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;

import org.unitseek.knitid.core.exception.IdErrorCodes;
import org.unitseek.knitid.core.exception.WorkerIdUnavailableException;
import org.unitseek.knitid.core.spi.WorkerId;
import org.unitseek.knitid.core.spi.WorkerIdAssigner;

/**
 * ZooKeeper ephemeral-node based WorkerId assigner.
 *
 * <p>Each slot is an ephemeral node {@code /workers/<datacenter>/<worker>}.
 * ZooKeeper removes the node when the session expires, which is the lease
 * mechanism. The KSID generator renews by checking the connection and node
 * existence.</p>
 */
final class ZookeeperWorkerIdAssigner implements WorkerIdAssigner, AutoCloseable {

    private static final String BASE_PATH = "/workers";

    private final CuratorFramework client;

    private final int datacenterId;

    private final int maxWorkerId;

    private volatile WorkerId workerId;

    ZookeeperWorkerIdAssigner(CuratorFramework client, int datacenterId, int maxWorkerId) {
        this.client = client;
        this.datacenterId = datacenterId;
        this.maxWorkerId = maxWorkerId;
    }

    @Override
    public synchronized WorkerId assign() {
        WorkerId current = workerId;
        if (current != null) {
            return current;
        }
        for (int candidate = 0; candidate <= maxWorkerId; candidate++) {
            String path = path(candidate);
            try {
                client.create()
                        .creatingParentsIfNeeded()
                        .withMode(CreateMode.EPHEMERAL)
                        .forPath(path);
                WorkerId assigned = new WorkerId(datacenterId, candidate);
                workerId = assigned;
                return assigned;
            }
            catch (KeeperException.NodeExistsException ex) {
                // Slot occupied; try the next one.
            }
            catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new WorkerIdUnavailableException(
                        IdErrorCodes.WORKER_UNAVAILABLE,
                        "Interrupted while acquiring ZooKeeper WorkerId", ex);
            }
            catch (Exception ex) {
                throw new WorkerIdUnavailableException(
                        IdErrorCodes.WORKER_UNAVAILABLE,
                        "Failed to create ZooKeeper WorkerId node " + path, ex);
            }
        }
        throw new WorkerIdUnavailableException(
                IdErrorCodes.WORKER_UNAVAILABLE,
                "No free ZooKeeper WorkerId slot in datacenter " + datacenterId
                        + " (max worker id " + maxWorkerId + ")");
    }

    @Override
    public boolean renewable() {
        return true;
    }

    @Override
    public void renew() {
        WorkerId current = workerId;
        if (current == null) {
            throw new WorkerIdUnavailableException(
                    IdErrorCodes.WORKER_UNAVAILABLE, "ZooKeeper WorkerId has not been assigned yet");
        }
        if (!client.getZookeeperClient().isConnected()) {
            throw new WorkerIdUnavailableException(
                    IdErrorCodes.WORKER_LEASE_LOST,
                    "ZooKeeper session is not connected; WorkerId lease may be lost: " + current);
        }
        try {
            Stat stat = client.checkExists().forPath(path(current.workerId()));
            if (stat == null) {
                throw new WorkerIdUnavailableException(
                        IdErrorCodes.WORKER_LEASE_LOST,
                        "ZooKeeper ephemeral node disappeared; WorkerId lease lost: " + current);
            }
        }
        catch (WorkerIdUnavailableException ex) {
            throw ex;
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new WorkerIdUnavailableException(
                    IdErrorCodes.WORKER_LEASE_LOST,
                    "Interrupted while renewing ZooKeeper WorkerId lease", ex);
        }
        catch (Exception ex) {
            throw new WorkerIdUnavailableException(
                    IdErrorCodes.WORKER_LEASE_LOST,
                    "Failed to renew ZooKeeper WorkerId lease for " + current, ex);
        }
    }

    @Override
    public synchronized void release(WorkerId released) {
        if (released == null) {
            return;
        }
        try {
            client.delete().guaranteed().forPath(path(released.workerId()));
        }
        catch (KeeperException.NoNodeException ex) {
            // Already gone; release is idempotent.
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        catch (Exception ex) {
            // Best-effort release; the ephemeral node will disappear with the session.
        }
        finally {
            if (released.equals(workerId)) {
                workerId = null;
            }
        }
    }

    @Override
    public void close() {
        client.close();
    }

    private String path(int worker) {
        return BASE_PATH + "/" + datacenterId + "/" + worker;
    }
}
