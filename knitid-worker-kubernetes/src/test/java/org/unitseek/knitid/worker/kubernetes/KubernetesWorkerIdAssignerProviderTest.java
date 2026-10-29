package org.unitseek.knitid.worker.kubernetes;

import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import org.unitseek.knitid.core.exception.IdConfigurationException;
import org.unitseek.knitid.core.exception.WorkerIdUnavailableException;
import org.unitseek.knitid.core.spi.WorkerId;
import org.unitseek.knitid.core.spi.WorkerIdAssigner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KubernetesWorkerIdAssignerProviderTest {

    @Test
    void usesStatefulSetOrdinalFromHostname() {
        WorkerIdAssigner assigner = provider(Map.of("HOSTNAME", "orders-3")).create(1, 5);
        assertThat(assigner.assign()).isEqualTo(new WorkerId(1, 3));
        assertThat(assigner.assign()).isEqualTo(new WorkerId(1, 3));
    }

    @Test
    void usesDownwardApiPodName() {
        KubernetesWorkerIdProperties properties = new KubernetesWorkerIdProperties();
        properties.setMode(KubernetesWorkerIdProperties.Mode.DOWNWARD_API);
        WorkerIdAssigner assigner = new KubernetesWorkerIdAssignerProvider(properties,
                Map.of("POD_NAME", "orders-2")::get).create(0, 5);
        assertThat(assigner.assign()).isEqualTo(new WorkerId(0, 2));
    }

    @Test
    void explicitPodNameWins() {
        KubernetesWorkerIdProperties properties = new KubernetesWorkerIdProperties();
        properties.setPodName("orders-4");
        WorkerIdAssigner assigner = new KubernetesWorkerIdAssignerProvider(properties,
                name -> "orders-1").create(0, 5);
        assertThat(assigner.assign()).isEqualTo(new WorkerId(0, 4));
    }

    @Test
    void missingPodNameFails() {
        assertThatThrownBy(() -> provider(Map.of()).create(0, 5))
                .isInstanceOf(IdConfigurationException.class)
                .hasMessageContaining("HOSTNAME");
    }

    @Test
    void invalidPodNameFails() {
        assertThatThrownBy(() -> provider(Map.of("HOSTNAME", "orders")).create(0, 5))
                .isInstanceOf(WorkerIdUnavailableException.class)
                .hasMessageContaining("KNITID-2003");
    }

    @Test
    void ordinalMustFitConfiguredBits() {
        assertThatThrownBy(() -> provider(Map.of("HOSTNAME", "orders-4")).create(0, 2))
                .isInstanceOf(WorkerIdUnavailableException.class)
                .hasMessageContaining("does not fit");
    }

    @Test
    void workerIdBitsAreValidated() {
        assertThatThrownBy(() -> provider(Map.of("HOSTNAME", "orders-1")).create(0, 0))
                .isInstanceOf(IdConfigurationException.class)
                .hasMessageContaining("worker-id-bits");
    }

    private static KubernetesWorkerIdAssignerProvider provider(Map<String, String> environment) {
        KubernetesWorkerIdProperties properties = new KubernetesWorkerIdProperties();
        Function<String, String> lookup = environment::get;
        return new KubernetesWorkerIdAssignerProvider(properties, lookup);
    }
}
