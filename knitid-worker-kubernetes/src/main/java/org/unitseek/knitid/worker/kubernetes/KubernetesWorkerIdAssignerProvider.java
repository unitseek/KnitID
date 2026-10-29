package org.unitseek.knitid.worker.kubernetes;

import java.util.Locale;
import java.util.function.Function;

import org.unitseek.knitid.core.exception.IdConfigurationException;
import org.unitseek.knitid.core.exception.IdErrorCodes;
import org.unitseek.knitid.core.exception.WorkerIdUnavailableException;
import org.unitseek.knitid.core.spi.WorkerIdAssigner;
import org.unitseek.knitid.core.spi.WorkerIdAssignerProvider;

/**
 * Exposes the Kubernetes StatefulSet ordinal assigner under the name
 * {@code kubernetes}.
 */
public final class KubernetesWorkerIdAssignerProvider implements WorkerIdAssignerProvider {

    public static final String NAME = "kubernetes";

    private static final int MAX_SUPPORTED_WORKER_ID_BITS = 16;

    private final KubernetesWorkerIdProperties properties;

    private final Function<String, String> environment;

    public KubernetesWorkerIdAssignerProvider(KubernetesWorkerIdProperties properties) {
        this(properties, KubernetesWorkerIdAssignerProvider::lookupSystem);
    }

    public KubernetesWorkerIdAssignerProvider(KubernetesWorkerIdProperties properties,
            Function<String, String> environment) {
        this.properties = properties;
        this.environment = environment;
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
                    "Kubernetes WorkerId assigner supports worker-id-bits between 1 and "
                            + MAX_SUPPORTED_WORKER_ID_BITS + ": " + workerIdBits);
        }
        String podName = resolvePodName();
        int ordinal = parseOrdinal(podName);
        int maxWorkerId = (1 << workerIdBits) - 1;
        if (ordinal > maxWorkerId) {
            throw new WorkerIdUnavailableException(
                    IdErrorCodes.WORKER_UNAVAILABLE,
                    "Kubernetes pod ordinal " + ordinal + " does not fit in " + workerIdBits + " bits");
        }
        return new KubernetesWorkerIdAssigner(datacenterId, ordinal, podName);
    }

    private String resolvePodName() {
        String configured = properties.getPodName();
        if (configured != null && !configured.isBlank()) {
            return configured.trim();
        }
        String envName = properties.getMode() == KubernetesWorkerIdProperties.Mode.DOWNWARD_API
                ? "POD_NAME"
                : "HOSTNAME";
        String value = environment.apply(envName);
        if (value == null || value.isBlank()) {
            throw new IdConfigurationException(
                    IdErrorCodes.CONFIG_INVALID,
                    "knitid.worker.kubernetes.pod-name is required, or provide environment variable " + envName);
        }
        return value.trim();
    }

    private static int parseOrdinal(String podName) {
        int separator = podName.lastIndexOf('-');
        if (separator < 0 || separator == podName.length() - 1) {
            throw new WorkerIdUnavailableException(
                    IdErrorCodes.WORKER_UNAVAILABLE,
                    "Cannot parse StatefulSet ordinal from pod name: " + podName);
        }
        try {
            return Integer.parseInt(podName.substring(separator + 1));
        }
        catch (NumberFormatException ex) {
            throw new WorkerIdUnavailableException(
                    IdErrorCodes.WORKER_UNAVAILABLE,
                    "Cannot parse StatefulSet ordinal from pod name: " + podName,
                    ex);
        }
    }

    private static String lookupSystem(String name) {
        String value = System.getProperty(name);
        return value != null ? value : System.getenv(name);
    }
}
