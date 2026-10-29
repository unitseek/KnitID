package org.unitseek.knitid.worker.kubernetes;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import org.unitseek.knitid.core.spi.WorkerIdAssignerProvider;
import org.unitseek.knitid.worker.kubernetes.autoconfigure.KubernetesWorkerIdAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class KubernetesWorkerIdAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(KubernetesWorkerIdAutoConfiguration.class));

    @Test
    void registersKubernetesProvider() {
        runner.withPropertyValues("knitid.worker.kubernetes.pod-name=orders-2").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(WorkerIdAssignerProvider.class);
            WorkerIdAssignerProvider provider = context.getBean(WorkerIdAssignerProvider.class);
            assertThat(provider.name()).isEqualTo("kubernetes");
            assertThat(provider.create(1, 5).assign().workerId()).isEqualTo(2);
        });
    }

    @Test
    void canBeDisabled() {
        runner.withPropertyValues("knitid.worker.kubernetes.enabled=false").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(WorkerIdAssignerProvider.class);
        });
    }
}
