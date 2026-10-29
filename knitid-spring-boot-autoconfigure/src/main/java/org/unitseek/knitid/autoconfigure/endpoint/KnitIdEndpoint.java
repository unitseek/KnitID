package org.unitseek.knitid.autoconfigure.endpoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import org.unitseek.knitid.core.api.IdGenerator;
import org.unitseek.knitid.core.api.IdGeneratorRegistry;
import org.unitseek.knitid.core.exception.IdGeneratorNotFoundException;

/**
 * Read-only actuator endpoint listing registered generators.
 */
@Endpoint(id = "knitid")
public class KnitIdEndpoint {

    private final IdGeneratorRegistry registry;

    public KnitIdEndpoint(IdGeneratorRegistry registry) {
        this.registry = registry;
    }

    @ReadOperation
    public KnitIdEndpointInfo info() {
        IdGenerator defaultGenerator;
        try {
            defaultGenerator = registry.getDefault();
        }
        catch (IdGeneratorNotFoundException ex) {
            defaultGenerator = null;
        }
        String defaultName = defaultGenerator == null ? null : nameOf(defaultGenerator);
        List<GeneratorInfo> generators = new ArrayList<>();
        for (Map.Entry<String, IdGenerator> entry : registry.asMap().entrySet()) {
            IdGenerator generator = entry.getValue();
            generators.add(new GeneratorInfo(entry.getKey(), generator.type().value(),
                    generator == defaultGenerator, generator.blocking()));
        }
        return new KnitIdEndpointInfo(defaultName,
                defaultGenerator == null ? null : defaultGenerator.type().value(),
                generators);
    }

    private String nameOf(IdGenerator generator) {
        for (Map.Entry<String, IdGenerator> entry : registry.asMap().entrySet()) {
            if (entry.getValue() == generator) {
                return entry.getKey();
            }
        }
        return null;
    }

    public record KnitIdEndpointInfo(String defaultGenerator, String defaultType, List<GeneratorInfo> generators) {
    }

    public record GeneratorInfo(String name, String type, boolean defaultGenerator, boolean blocking) {
    }
}
