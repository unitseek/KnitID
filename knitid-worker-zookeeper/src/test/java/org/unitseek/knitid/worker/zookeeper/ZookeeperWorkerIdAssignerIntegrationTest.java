package org.unitseek.knitid.worker.zookeeper;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import org.unitseek.knitid.core.spi.WorkerId;
import org.unitseek.knitid.core.spi.WorkerIdAssigner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZooKeeper ephemeral-node integration tests against a real ZooKeeper
 * container. The image can be overridden with {@code KNITID_TEST_ZOOKEEPER_IMAGE}.
 */
@Testcontainers(disabledWithoutDocker = true)
class ZookeeperWorkerIdAssignerIntegrationTest {

    private static final String ZOOKEEPER_IMAGE = System.getenv().getOrDefault(
            "KNITID_TEST_ZOOKEEPER_IMAGE", "zookeeper:3.9.6");

    @Container
    static final GenericContainer<?> ZOOKEEPER = new GenericContainer<>(DockerImageName.parse(ZOOKEEPER_IMAGE))
            .withExposedPorts(2181)
            .waitingFor(Wait.forListeningPort());

    @Test
    void releaseIgnoresWorkerIdNotOwnedByThisAssigner() throws Exception {
        ZookeeperWorkerIdAssignerProvider provider = provider();
        WorkerIdAssigner first = provider.create(1, 5);
        WorkerIdAssigner next = null;
        try {
            WorkerId owned = first.assign();
            first.release(new WorkerId(1, 1));
            assertThat(owned).isEqualTo(new WorkerId(1, 0));
            next = provider.create(1, 5);
            assertThat(next.assign()).isEqualTo(new WorkerId(1, 1));
        }
        finally {
            close(first);
            close(next);
        }
    }

    @Test
    void assignsUniqueEphemeralNodesAndReleasesThem() throws Exception {
        ZookeeperWorkerIdAssignerProvider provider = provider();
        WorkerIdAssigner first = provider.create(1, 5);
        WorkerIdAssigner second = provider.create(1, 5);
        WorkerIdAssigner third = null;
        try {
            WorkerId firstWorker = first.assign();
            WorkerId secondWorker = second.assign();
            assertThat(firstWorker).isEqualTo(new WorkerId(1, 0));
            assertThat(secondWorker).isEqualTo(new WorkerId(1, 1));
            first.renew();
            second.renew();

            first.release(firstWorker);
            third = provider.create(1, 5);
            assertThat(third.assign()).isEqualTo(new WorkerId(1, 0));
        }
        finally {
            close(first);
            close(second);
            close(third);
        }
    }

    private static ZookeeperWorkerIdAssignerProvider provider() {
        ZookeeperWorkerIdProperties properties = new ZookeeperWorkerIdProperties();
        properties.setConnectString(ZOOKEEPER.getHost() + ":" + ZOOKEEPER.getMappedPort(2181));
        properties.setNamespace("knitid-test-" + UUID.randomUUID());
        properties.setSessionTimeout(Duration.ofSeconds(4));
        properties.setConnectionTimeout(Duration.ofSeconds(5));
        return new ZookeeperWorkerIdAssignerProvider(properties);
    }

    private static void close(WorkerIdAssigner assigner) throws Exception {
        if (assigner instanceof AutoCloseable closeable) {
            closeable.close();
        }
    }
}
