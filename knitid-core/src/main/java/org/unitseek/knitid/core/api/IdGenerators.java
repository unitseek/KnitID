package org.unitseek.knitid.core.api;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.unitseek.knitid.core.exception.IdErrorCodes;
import org.unitseek.knitid.core.exception.IdGeneratorNotFoundException;

/**
 * Static facade for non-Spring usage and tests.
 *
 * <p>This facade has no Spring dependency and does not share state with the
 * Spring Boot registry.</p>
 */
public final class IdGenerators {

    private static final Map<String, IdGenerator> GENERATORS = new ConcurrentHashMap<>();

    private static volatile String defaultName;

    private IdGenerators() {
    }

    public static void register(String name, IdGenerator generator) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (generator == null) {
            throw new IllegalArgumentException("generator must not be null");
        }
        GENERATORS.put(name, generator);
        if (defaultName == null) {
            defaultName = name;
        }
    }

    public static void setDefault(String name) {
        if (name == null || !GENERATORS.containsKey(name)) {
            throw new IdGeneratorNotFoundException(
                    IdErrorCodes.GENERATOR_NOT_FOUND, "Unknown default generator: " + name);
        }
        defaultName = name;
    }

    public static void unregister(String name) {
        GENERATORS.remove(name);
        if (name != null && name.equals(defaultName)) {
            defaultName = GENERATORS.keySet().stream().sorted().findFirst().orElse(null);
        }
    }

    public static Optional<IdGenerator> find(String name) {
        return name == null ? Optional.empty() : Optional.ofNullable(GENERATORS.get(name));
    }

    public static IdGenerator get(String name) {
        return find(name).orElseThrow(() -> new IdGeneratorNotFoundException(
                IdErrorCodes.GENERATOR_NOT_FOUND,
                "Generator not found: " + name + "; registered=" + GENERATORS.keySet()));
    }

    public static IdGenerator getDefault() {
        String name = defaultName;
        if (name == null) {
            throw new IdGeneratorNotFoundException(
                    IdErrorCodes.GENERATOR_NOT_FOUND, "No default generator registered");
        }
        return get(name);
    }

    public static Set<String> names() {
        return Set.copyOf(GENERATORS.keySet());
    }

    public static void reset() {
        GENERATORS.clear();
        defaultName = null;
    }
}
