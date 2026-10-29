package org.unitseek.knitid.autoconfigure;

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

    public Map<String, Map<String, Object>> getGenerators() {
        return generators;
    }

    public void setGenerators(Map<String, Map<String, Object>> generators) {
        this.generators = generators == null ? new LinkedHashMap<>() : generators;
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
