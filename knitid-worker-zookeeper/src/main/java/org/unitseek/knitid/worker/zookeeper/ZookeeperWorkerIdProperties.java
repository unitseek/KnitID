package org.unitseek.knitid.worker.zookeeper;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * ZooKeeper WorkerId configuration ({@code knitid.worker.zookeeper.*}).
 */
@ConfigurationProperties(prefix = "knitid.worker.zookeeper")
public class ZookeeperWorkerIdProperties {

    private boolean enabled = true;

    private String connectString;

    private String namespace = "knitid";

    private Duration sessionTimeout = Duration.ofSeconds(10);

    private Duration connectionTimeout = Duration.ofSeconds(10);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getConnectString() {
        return connectString;
    }

    public void setConnectString(String connectString) {
        this.connectString = connectString;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public Duration getSessionTimeout() {
        return sessionTimeout;
    }

    public void setSessionTimeout(Duration sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    public Duration getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(Duration connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }
}
