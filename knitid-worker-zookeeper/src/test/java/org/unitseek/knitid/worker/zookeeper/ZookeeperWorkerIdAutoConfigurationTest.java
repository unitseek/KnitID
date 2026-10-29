package org.unitseek.knitid.worker.zookeeper;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import org.unitseek.knitid.core.spi.WorkerIdAssignerProvider;
import org.unitseek.knitid.worker.zookeeper.autoconfigure.ZookeeperWorkerIdAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class ZookeeperWorkerIdAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ZookeeperWorkerIdAutoConfiguration.class));

    @Test
    void registersZookeeperProvider() {
        runner.withPropertyValues("knitid.worker.zookeeper.connect-string=127.0.0.1:2181").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(WorkerIdAssignerProvider.class);
            assertThat(context.getBean(WorkerIdAssignerProvider.class).name()).isEqualTo("zookeeper");
        });
    }

    @Test
    void canBeDisabled() {
        runner.withPropertyValues("knitid.worker.zookeeper.enabled=false").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(WorkerIdAssignerProvider.class);
        });
    }
}
