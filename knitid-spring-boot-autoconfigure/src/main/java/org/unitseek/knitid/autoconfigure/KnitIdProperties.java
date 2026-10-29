package org.unitseek.knitid.autoconfigure;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import org.unitseek.knitid.core.api.IdGeneratorConstants;

/**
 * KnitID configuration properties ({@code knitid.*}).
 */
@ConfigurationProperties(prefix = "knitid", ignoreUnknownFields = false)
public class KnitIdProperties {

    private boolean enabled = true;

    private String defaultType;

    private String defaultGenerator;

    private Limits limits = new Limits();

    private Health health = new Health();

    private Map<String, Map<String, Object>> generators = new LinkedHashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDefaultType() {
        return defaultType;
    }

    public void setDefaultType(String defaultType) {
        this.defaultType = defaultType;
    }

    public String getDefaultGenerator() {
        return defaultGenerator;
    }

    public void setDefaultGenerator(String defaultGenerator) {
        this.defaultGenerator = defaultGenerator;
    }

    public Limits getLimits() {
        return limits;
    }

    public void setLimits(Limits limits) {
        this.limits = limits == null ? new Limits() : limits;
    }

    public Health getHealth() {
        return health;
    }

    public void setHealth(Health health) {
        this.health = health == null ? new Health() : health;
    }

    public Map<String, Map<String, Object>> getGenerators() {
        return generators;
    }

    public void setGenerators(Map<String, Map<String, Object>> generators) {
        this.generators = generators == null ? new LinkedHashMap<>() : generators;
    }

    public static class Health {

        private Duration cacheTtl = Duration.ofSeconds(5);

        private int backwardsDownCount = 5;

        private Duration backwardsDownWindow = Duration.ofMinutes(1);

        public Duration getCacheTtl() {
            return cacheTtl;
        }

        public void setCacheTtl(Duration cacheTtl) {
            if (cacheTtl == null || cacheTtl.isNegative() || cacheTtl.isZero()) {
                throw new IllegalArgumentException("knitid.health.cache-ttl must be positive");
            }
            this.cacheTtl = cacheTtl;
        }

        public int getBackwardsDownCount() {
            return backwardsDownCount;
        }

        public void setBackwardsDownCount(int backwardsDownCount) {
            if (backwardsDownCount < 0) {
                throw new IllegalArgumentException("knitid.health.backwards-down-count must be >= 0");
            }
            this.backwardsDownCount = backwardsDownCount;
        }

        public Duration getBackwardsDownWindow() {
            return backwardsDownWindow;
        }

        public void setBackwardsDownWindow(Duration backwardsDownWindow) {
            if (backwardsDownWindow != null && (backwardsDownWindow.isNegative() || backwardsDownWindow.isZero())) {
                throw new IllegalArgumentException("knitid.health.backwards-down-window must be positive");
            }
            this.backwardsDownWindow = backwardsDownWindow;
        }
    }

    public static class Limits {

        private int maxBatchSize = IdGeneratorConstants.DEFAULT_MAX_BATCH_SIZE;

        public int getMaxBatchSize() {
            return maxBatchSize;
        }

        public void setMaxBatchSize(int maxBatchSize) {
            if (maxBatchSize <= 0) {
                throw new IllegalArgumentException("knitid.limits.max-batch-size must be > 0");
            }
            this.maxBatchSize = maxBatchSize;
        }
    }
}
