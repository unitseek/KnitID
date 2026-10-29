package org.unitseek.knitid.worker.redis;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Redis WorkerId lease configuration ({@code knitid.worker.redis.*}).
 */
@ConfigurationProperties(prefix = "knitid.worker.redis")
public class RedisWorkerIdProperties {

    private boolean enabled = true;

    private String keyPrefix = "knitid:worker";

    private Duration leaseTtl = Duration.ofSeconds(60);

    private Duration renewInterval = Duration.ofSeconds(20);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public Duration getLeaseTtl() {
        return leaseTtl;
    }

    public void setLeaseTtl(Duration leaseTtl) {
        this.leaseTtl = leaseTtl;
    }

    public Duration getRenewInterval() {
        return renewInterval;
    }

    public void setRenewInterval(Duration renewInterval) {
        this.renewInterval = renewInterval;
    }
}
