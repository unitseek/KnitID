package org.unitseek.knitid.autoconfigure;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.unitseek.knitid.core.api.IdType;
import org.unitseek.knitid.core.spi.GeneratorConfig;

/**
 * {@link GeneratorConfig} backed by a flattened map.
 */
final class MapGeneratorConfig implements GeneratorConfig {

    private final String name;

    private final IdType type;

    private final Map<String, Object> parameters;

    MapGeneratorConfig(String name, IdType type, Map<String, Object> parameters) {
        this.name = name;
        this.type = type;
        this.parameters = new LinkedHashMap<>(parameters);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public IdType type() {
        return type;
    }

    @Override
    public Optional<Object> get(String key) {
        return Optional.ofNullable(parameters.get(key));
    }

    Map<String, Object> parameters() {
        return parameters;
    }
}
