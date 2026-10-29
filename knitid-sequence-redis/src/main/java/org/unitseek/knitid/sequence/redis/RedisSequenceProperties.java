package org.unitseek.knitid.sequence.redis;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Redis sequence configuration ({@code knitid.sequence.redis.*}).
 */
@ConfigurationProperties(prefix = "knitid.sequence.redis")
public class RedisSequenceProperties {

    private boolean enabled = true;

    private String keyPrefix = "knitid:seq";

    private Duration ttl = Duration.ofDays(2);

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

    public Duration getTtl() {
        return ttl;
    }

    public void setTtl(Duration ttl) {
        this.ttl = ttl;
    }
}
