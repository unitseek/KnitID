package org.unitseek.knitid.worker.zookeeper;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import org.unitseek.knitid.core.exception.IdErrorCodes;
import org.unitseek.knitid.core.exception.WorkerIdUnavailableException;
import org.unitseek.knitid.core.spi.WorkerIdAssigner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ZooKeeper network-failure integration test: stop the container and verify the
 * lease is reported as lost and new connections fail.
 */
@Testcontainers(disabledWithoutDocker = true)
class ZookeeperWorkerIdAssignerDisconnectIntegrationTest {

    private static final String ZOOKEEPER_IMAGE = System.getenv().getOrDefault(
            "KNITID_TEST_ZOOKEEPER_IMAGE", "zookeeper:3.9.6");

    @Test
    void renewFailsWhenZooKeeperStops() throws Exception {
        GenericContainer<?> zookeeper = new GenericContainer<>(DockerImageName.parse(ZOOKEEPER_IMAGE))
                .withExposedPorts(2181)
                .waitingFor(Wait.forListeningPort());
        zookeeper.start();
        ZookeeperWorkerIdAssignerProvider provider = null;
        WorkerIdAssigner assigner = null;
        try {
            ZookeeperWorkerIdProperties properties = new ZookeeperWorkerIdProperties();
            properties.setConnectString(zookeeper.getHost() + ":" + zookeeper.getMappedPort(2181));
            properties.setNamespace("knitid-test-" + UUID.randomUUID());
            properties.setSessionTimeout(Duration.ofSeconds(4));
            properties.setConnectionTimeout(Duration.ofSeconds(3));
            provider = new ZookeeperWorkerIdAssignerProvider(properties);
            assigner = provider.create(0, 5);
            assigner.assign();

            zookeeper.stop();

            WorkerIdUnavailableException failure = null;
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
            while (System.nanoTime() < deadline) {
                try {
                    assigner.renew();
                    Thread.sleep(200);
                }
                catch (WorkerIdUnavailableException ex) {
                    failure = ex;
                    break;
                }
            }
            assertThat(failure).isNotNull();
            assertThat(failure.code()).isEqualTo(IdErrorCodes.WORKER_LEASE_LOST);

            ZookeeperWorkerIdAssignerProvider offlineProvider = provider;
            assertThatThrownBy(() -> offlineProvider.create(0, 5))
                    .isInstanceOf(WorkerIdUnavailableException.class)
                    .hasMessageContaining(IdErrorCodes.WORKER_UNAVAILABLE);
        }
        finally {
            if (assigner instanceof AutoCloseable closeable) {
                closeable.close();
            }
            if (zookeeper.isRunning()) {
                zookeeper.stop();
            }
        }
    }
}
