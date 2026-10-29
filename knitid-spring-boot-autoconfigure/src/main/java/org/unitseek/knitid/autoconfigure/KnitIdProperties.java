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
        this.limits = limits;
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
            this.cacheTtl = cacheTtl;
        }

        public int getBackwardsDownCount() {
            return backwardsDownCount;
        }

        public void setBackwardsDownCount(int backwardsDownCount) {
            this.backwardsDownCount = backwardsDownCount;
        }

        public Duration getBackwardsDownWindow() {
            return backwardsDownWindow;
        }

        public void setBackwardsDownWindow(Duration backwardsDownWindow) {
            this.backwardsDownWindow = backwardsDownWindow;
        }
    }

    public static class Limits {

        private int maxBatchSize = IdGeneratorConstants.DEFAULT_MAX_BATCH_SIZE;

        public int getMaxBatchSize() {
            return maxBatchSize;
        }

        public void setMaxBatchSize(int maxBatchSize) {
            this.maxBatchSize = maxBatchSize;
        }
    }
}
