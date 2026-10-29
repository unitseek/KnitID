package org.unitseek.knitid.autoconfigure;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.unitseek.knitid.core.api.IdGenerator;
import org.unitseek.knitid.core.api.IdGeneratorRegistry;
import org.unitseek.knitid.core.api.IdType;
import org.unitseek.knitid.core.exception.IdErrorCodes;
import org.unitseek.knitid.core.exception.IdGeneratorNotFoundException;

/**
 * Immutable registry used by the Spring Boot auto-configuration.
 */
final class SimpleIdGeneratorRegistry implements IdGeneratorRegistry {

    private final Map<String, IdGenerator> generators;

    private final IdGenerator defaultGenerator;

    SimpleIdGeneratorRegistry(Map<String, IdGenerator> generators, IdGenerator defaultGenerator) {
        this.generators = Collections.unmodifiableMap(new LinkedHashMap<>(generators));
        this.defaultGenerator = defaultGenerator;
    }

    @Override
    public IdGenerator getDefault() {
        if (defaultGenerator == null) {
            throw new IdGeneratorNotFoundException(
                    IdErrorCodes.GENERATOR_NOT_FOUND,
                    "No default generator configured; add knitid.default-type or knitid.default-generator");
        }
        return defaultGenerator;
    }

    @Override
    public IdGenerator get(String name) {
        return find(name).orElseThrow(() -> new IdGeneratorNotFoundException(
                IdErrorCodes.GENERATOR_NOT_FOUND,
                "Generator not found: " + name + "; registered=" + generators.keySet()));
    }

    @Override
    public Optional<IdGenerator> find(String name) {
        return name == null ? Optional.empty() : Optional.ofNullable(generators.get(name));
    }

    @Override
    public Optional<IdGenerator> find(IdType type) {
        if (type == null) {
            return Optional.empty();
        }
        return generators.values().stream().filter(generator -> type.equals(generator.type())).findFirst();
    }

    @Override
    public Set<String> names() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(generators.keySet()));
    }

    @Override
    public Map<String, IdGenerator> asMap() {
        return generators;
    }
}
