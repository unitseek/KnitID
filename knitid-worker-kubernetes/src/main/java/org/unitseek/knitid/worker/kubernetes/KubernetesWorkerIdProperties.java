package org.unitseek.knitid.worker.kubernetes;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Kubernetes WorkerId configuration ({@code knitid.worker.kubernetes.*}).
 */
@ConfigurationProperties(prefix = "knitid.worker.kubernetes")
public class KubernetesWorkerIdProperties {

    private boolean enabled = true;

    private Mode mode = Mode.STATEFULSET_ORDINAL;

    private String podName;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public String getPodName() {
        return podName;
    }

    public void setPodName(String podName) {
        this.podName = podName;
    }

    public enum Mode {

        /** Read the pod name from HOSTNAME and use the StatefulSet ordinal suffix. */
        STATEFULSET_ORDINAL,

        /** Read the pod name from the Downward API environment variable POD_NAME. */
        DOWNWARD_API
    }
}
