package org.unitseek.knitid.worker.zookeeper;

import java.time.Duration;
import java.util.UUID;

import org.apache.curator.test.TestingServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.unitseek.knitid.core.exception.IdConfigurationException;
import org.unitseek.knitid.core.spi.WorkerId;
import org.unitseek.knitid.core.spi.WorkerIdAssigner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ZookeeperWorkerIdAssignerProviderTest {

    private static TestingServer server;

    @BeforeAll
    static void startServer() throws Exception {
        server = new TestingServer();
    }

    @AfterAll
    static void stopServer() throws Exception {
        if (server != null) {
            server.close();
        }
    }

    @Test
    void assignsUniqueEphemeralNodes() throws Exception {
        ZookeeperWorkerIdAssignerProvider provider = provider();
        WorkerIdAssigner first = provider.create(1, 5);
        WorkerIdAssigner second = provider.create(1, 5);
        try {
            assertThat(first.assign()).isEqualTo(new WorkerId(1, 0));
            assertThat(second.assign()).isEqualTo(new WorkerId(1, 1));
            first.renew();
        }
        finally {
            close(first);
            close(second);
        }
    }

    @Test
    void releaseDeletesNodeAndAllowsReuse() throws Exception {
        ZookeeperWorkerIdAssignerProvider provider = provider();
        WorkerIdAssigner first = provider.create(0, 5);
        WorkerId worker = first.assign();
        first.release(worker);
        close(first);

        WorkerIdAssigner second = provider.create(0, 5);
        try {
            assertThat(second.assign()).isEqualTo(new WorkerId(0, 0));
        }
        finally {
            close(second);
        }
    }

    @Test
    void missingConnectStringIsRejected() {
        ZookeeperWorkerIdProperties properties = new ZookeeperWorkerIdProperties();
        ZookeeperWorkerIdAssignerProvider provider = new ZookeeperWorkerIdAssignerProvider(properties);
        assertThatThrownBy(() -> provider.create(0, 5))
                .isInstanceOf(IdConfigurationException.class)
                .hasMessageContaining("connect-string");
    }

    @Test
    void unsupportedWorkerIdBitsAreRejected() {
        ZookeeperWorkerIdAssignerProvider provider = provider();
        assertThatThrownBy(() -> provider.create(0, 0))
                .isInstanceOf(IdConfigurationException.class)
                .hasMessageContaining("worker-id-bits");
    }

    private static ZookeeperWorkerIdAssignerProvider provider() {
        ZookeeperWorkerIdProperties properties = new ZookeeperWorkerIdProperties();
        properties.setConnectString(server.getConnectString());
        properties.setNamespace("knitid-test-" + UUID.randomUUID());
        properties.setSessionTimeout(Duration.ofSeconds(3));
        properties.setConnectionTimeout(Duration.ofSeconds(3));
        return new ZookeeperWorkerIdAssignerProvider(properties);
    }

    private static void close(WorkerIdAssigner assigner) throws Exception {
        if (assigner instanceof AutoCloseable closeable) {
            closeable.close();
        }
    }
}
