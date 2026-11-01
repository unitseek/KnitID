package org.unitseek.knitid.autoconfigure.endpoint;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import org.unitseek.knitid.autoconfigure.KnitIdProperties;
import org.unitseek.knitid.core.api.IdGenerator;
import org.unitseek.knitid.core.api.IdGeneratorRegistry;
import org.unitseek.knitid.core.api.IdType;

/**
 * Read-only, sanitized configuration diagnostics.
 *
 * <p>Only generator names, types, enabled flags and effective runtime limits
 * are exposed. Arbitrary generator configuration values are deliberately not
 * returned because they may contain URLs, credentials or business data.</p>
 *
 * @since 1.0.0
 */
@Endpoint(id = "knitid-config")
public class KnitIdConfigEndpoint {

    private final KnitIdProperties properties;

    private final IdGeneratorRegistry registry;

    public KnitIdConfigEndpoint(KnitIdProperties properties, IdGeneratorRegistry registry) {
        this.properties = properties;
        this.registry = registry;
    }

    /**
     * Returns sanitized effective configuration.
     *
     * @return configuration diagnostics
     */
    @ReadOperation
    public KnitIdConfigInfo diagnostics() {
        List<GeneratorConfigInfo> configured = new ArrayList<>();
        for (Map.Entry<String, Map<String, Object>> entry : properties.getGenerators().entrySet()) {
            Map<String, Object> raw = entry.getValue() == null ? Map.of() : entry.getValue();
            String type = raw.get("type") == null ? null : String.valueOf(raw.get("type"));
            boolean enabled = raw.get("enabled") == null
                    || Boolean.parseBoolean(String.valueOf(raw.get("enabled")));
            configured.add(new GeneratorConfigInfo(entry.getKey(), normalizeType(type), enabled));
        }

        List<RuntimeGeneratorInfo> runtime = new ArrayList<>();
        for (Map.Entry<String, IdGenerator> entry : registry.asMap().entrySet()) {
            IdGenerator generator = entry.getValue();
            runtime.add(new RuntimeGeneratorInfo(entry.getKey(), generator.type().value(),
                    generator == defaultGenerator(), generator.blocking()));
        }

        KnitIdProperties.Health health = properties.getHealth();
        return new KnitIdConfigInfo(properties.isEnabled(), properties.getDefaultType(),
                properties.getDefaultGenerator(), properties.getLimits().getMaxBatchSize(),
                new HealthInfo(health.getCacheTtl(), health.getBackwardsDownCount(),
                        health.getBackwardsDownWindow()),
                configured, runtime);
    }

    private IdGenerator defaultGenerator() {
        try {
            return registry.getDefault();
        }
        catch (RuntimeException ex) {
            return null;
        }
    }

    private static String normalizeType(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return IdType.of(raw).value();
        }
        catch (IllegalArgumentException ex) {
            return "INVALID";
        }
    }

    public record KnitIdConfigInfo(boolean enabled, String defaultType, String defaultGenerator,
            int maxBatchSize, HealthInfo health, List<GeneratorConfigInfo> configuredGenerators,
            List<RuntimeGeneratorInfo> runtimeGenerators) {
    }

    public record HealthInfo(Duration cacheTtl, int backwardsDownCount, Duration backwardsDownWindow) {
    }

    public record GeneratorConfigInfo(String name, String type, boolean enabled) {
    }

    public record RuntimeGeneratorInfo(String name, String type, boolean defaultGenerator, boolean blocking) {
    }
}
