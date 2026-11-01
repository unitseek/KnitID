package org.unitseek.knitid.autoconfigure;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.ObjectProvider;

import org.unitseek.knitid.core.api.IdGenerator;
import org.unitseek.knitid.core.api.IdType;
import org.unitseek.knitid.core.exception.IdConfigurationException;
import org.unitseek.knitid.core.exception.IdErrorCodes;
import org.unitseek.knitid.core.spi.GeneratorConfig;
import org.unitseek.knitid.core.spi.IdGeneratorDecorator;
import org.unitseek.knitid.core.spi.IdGeneratorFactory;

/**
 * Builds the registry from configuration and generator factories.
 */
final class IdGeneratorRegistryBuilder {

    private static final Set<String> COMMON_KEYS = Set.of("enabled", "type");

    private final KnitIdProperties properties;

    private final ObjectProvider<IdGeneratorFactory> factories;

    private final List<IdGeneratorDecorator> decorators;

    IdGeneratorRegistryBuilder(KnitIdProperties properties, ObjectProvider<IdGeneratorFactory> factories,
            List<IdGeneratorDecorator> decorators) {
        this.properties = properties;
        this.factories = factories;
        this.decorators = decorators == null ? List.of() : List.copyOf(decorators);
    }

    SimpleIdGeneratorRegistry build() {
        Map<IdType, IdGeneratorFactory> factoriesByType = new LinkedHashMap<>();
        factories.orderedStream().forEach(factory -> {
            IdGeneratorFactory existing = factoriesByType.putIfAbsent(factory.type(), factory);
            if (existing != null) {
                throw new IdConfigurationException(
                        IdErrorCodes.CONFIG_CONFLICT,
                        "Duplicate generator factory for type " + factory.type());
            }
        });

        Map<String, IdGenerator> generators = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Object>> entry : properties.getGenerators().entrySet()) {
            String name = entry.getKey();
            Map<String, Object> raw = entry.getValue() == null ? Map.of() : entry.getValue();
            if (isDisabled(raw)) {
                continue;
            }
            IdType type = requireType(name, raw);
            validateKeys(name, type, raw);
            IdGeneratorFactory factory = requireFactory(factoriesByType, type);
            GeneratorConfig config = new MapGeneratorConfig(name, type, flatten(raw));
            generators.put(name, decorate(factory.create(config), name));
        }

        IdGenerator defaultGenerator = resolveDefault(factoriesByType, generators);
        return new SimpleIdGeneratorRegistry(generators, defaultGenerator);
    }

    private IdGenerator resolveDefault(Map<IdType, IdGeneratorFactory> factoriesByType,
            Map<String, IdGenerator> generators) {

        String configuredName = properties.getDefaultGenerator();
        if (configuredName != null && !configuredName.isBlank()) {
            IdGenerator generator = generators.get(configuredName);
            if (generator == null) {
                throw new IdConfigurationException(
                        IdErrorCodes.CONFIG_INVALID,
                        "knitid.default-generator refers to unknown generator: " + configuredName);
            }
            return generator;
        }

        String configuredType = properties.getDefaultType();
        if (configuredType != null && !configuredType.isBlank()) {
            IdType type = IdType.of(configuredType);
            IdGenerator generator = generators.values().stream()
                    .filter(candidate -> type.equals(candidate.type()))
                    .findFirst()
                    .orElse(null);
            if (generator != null) {
                return generator;
            }
            IdGeneratorFactory factory = requireFactory(factoriesByType, type);
            IdGenerator implicit = decorate(factory.create(new MapGeneratorConfig("default", type, Map.of())), "default");
            return registerImplicitDefault(generators, implicit);
        }

        if (generators.size() == 1) {
            return generators.values().iterator().next();
        }
        if (generators.isEmpty()) {
            if (factoriesByType.isEmpty()) {
                return null;
            }
            if (factoriesByType.size() == 1) {
                Map.Entry<IdType, IdGeneratorFactory> entry = factoriesByType.entrySet().iterator().next();
                IdGenerator implicit = decorate(entry.getValue()
                        .create(new MapGeneratorConfig("default", entry.getKey(), Map.of())), "default");
                return registerImplicitDefault(generators, implicit);
            }
        }
        throw new IdConfigurationException(
                IdErrorCodes.CONFIG_CONFLICT,
                "Multiple generators available; configure knitid.default-type or knitid.default-generator. "
                        + "Available: " + describe(generators, factoriesByType));
    }

    private static IdType requireType(String name, Map<String, Object> raw) {
        Object value = raw.get("type");
        if (value == null || String.valueOf(value).isBlank()) {
            throw new IdConfigurationException(
                    IdErrorCodes.CONFIG_INVALID,
                    "Missing knitid.generators." + name + ".type");
        }
        try {
            return IdType.of(String.valueOf(value));
        }
        catch (IllegalArgumentException ex) {
            throw new IdConfigurationException(
                    IdErrorCodes.CONFIG_INVALID,
                    "Invalid knitid.generators." + name + ".type: " + value,
                    ex);
        }
    }

    private static void validateKeys(String name, IdType type, Map<String, Object> raw) {
        String block = type.value().toLowerCase(Locale.ROOT);
        for (String key : raw.keySet()) {
            if (!COMMON_KEYS.contains(key) && !block.equals(key)) {
                throw new IdConfigurationException(
                        IdErrorCodes.CONFIG_INVALID,
                        "Unknown property knitid.generators." + name + "." + key
                                + "; allowed: enabled, type, " + block);
            }
        }
    }

    private static boolean isDisabled(Map<String, Object> raw) {
        Object enabled = raw.get("enabled");
        return enabled != null && !Boolean.parseBoolean(String.valueOf(enabled));
    }

    private IdGenerator registerImplicitDefault(Map<String, IdGenerator> generators, IdGenerator implicit) {
        IdGenerator existing = generators.putIfAbsent("default", implicit);
        if (existing != null) {
            throw new IdConfigurationException(
                    IdErrorCodes.CONFIG_CONFLICT,
                    "Implicit default generator name 'default' conflicts with an explicitly configured generator");
        }
        return implicit;
    }

    private static boolean isIndexedMap(Map<?, ?> map) {
        return !map.isEmpty() && map.keySet().stream().allMatch(key -> {
            String text = String.valueOf(key);
            return !text.isEmpty() && text.chars().allMatch(Character::isDigit);
        });
    }

    private IdGenerator decorate(IdGenerator generator, String name) {
        IdGenerator decorated = generator;
        for (IdGeneratorDecorator decorator : decorators) {
            decorated = decorator.decorate(decorated, name);
        }
        return decorated;
    }

    private static IdGeneratorFactory requireFactory(Map<IdType, IdGeneratorFactory> factoriesByType, IdType type) {
        IdGeneratorFactory factory = factoriesByType.get(type);
        if (factory == null) {
            throw new IdConfigurationException(
                    IdErrorCodes.MODULE_MISSING,
                    "No generator module for type " + type + "; add dependency knitid-generator-"
                            + type.value().toLowerCase(Locale.ROOT) + " or register an IdGeneratorFactory");
        }
        return factory;
    }

    private static String describe(Map<String, IdGenerator> generators,
            Map<IdType, IdGeneratorFactory> factoriesByType) {
        StringBuilder builder = new StringBuilder();
        if (!generators.isEmpty()) {
            builder.append("generators=").append(generators.keySet());
        }
        if (!factoriesByType.isEmpty()) {
            if (!builder.isEmpty()) {
                builder.append(", ");
            }
            builder.append("types=").append(factoriesByType.keySet());
        }
        return builder.toString();
    }

    static GeneratorConfig generatorConfig(String name, IdType type, Map<String, Object> raw) {
        return new MapGeneratorConfig(name, type, flatten(raw));
    }

    private static Map<String, Object> flatten(Map<String, Object> raw) {
        Map<String, Object> result = new LinkedHashMap<>();
        flatten("", raw, result);
        return result;
    }

    private static void flatten(String prefix, Map<String, Object> source, Map<String, Object> target) {
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> nested) {
                if (isIndexedMap(nested)) {
                    // YAML sequences bind to Map<String,Object> as {0: ...}; expose a List
                    // without removing the legacy dotted keys for custom factories.
                    target.put(key, nested.entrySet().stream()
                            .sorted((left, right) -> Integer.compare(
                                    Integer.parseInt(String.valueOf(left.getKey())),
                                    Integer.parseInt(String.valueOf(right.getKey()))))
                            .map(Map.Entry::getValue)
                            .toList());
                }
                Map<String, Object> typed = new LinkedHashMap<>();
                nested.forEach((nestedKey, nestedValue) -> typed.put(String.valueOf(nestedKey), nestedValue));
                flatten(key, typed, target);
            }
            else {
                target.put(key, value);
            }
        }
    }
}
