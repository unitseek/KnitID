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
 *
 * @since 1.0.0
 */
public final class IdGenerators {

    private static final Map<String, IdGenerator> GENERATORS = new ConcurrentHashMap<>();

    private static volatile String defaultName;

    private IdGenerators() {
    }

    /**
     * Registers a generator and makes it the default if none is set.
     *
     * @param name unique generator name
     * @param generator generator instance
     * @throws IllegalArgumentException if name is blank or generator is null
     */
    public static synchronized void register(String name, IdGenerator generator) {
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

    /**
     * Sets the default generator.
     *
     * @param name registered generator name
     * @throws org.unitseek.knitid.core.exception.IdGeneratorNotFoundException if
     *         the name is not registered
     */
    public static synchronized void setDefault(String name) {
        if (name == null || !GENERATORS.containsKey(name)) {
            throw new IdGeneratorNotFoundException(
                    IdErrorCodes.GENERATOR_NOT_FOUND, "Unknown default generator: " + name);
        }
        defaultName = name;
    }

    /**
     * Removes a generator. If it was the default, the lexicographically first
     * remaining name becomes the default.
     *
     * @param name generator name; {@code null} is ignored
     */
    public static synchronized void unregister(String name) {
        if (name == null) {
            return;
        }
        GENERATORS.remove(name);
        if (name.equals(defaultName)) {
            defaultName = GENERATORS.keySet().stream().sorted().findFirst().orElse(null);
        }
    }

    /**
     * Finds a registered generator without throwing.
     *
     * @param name generator name, possibly {@code null}
     * @return matching generator
     */
    public static Optional<IdGenerator> find(String name) {
        return name == null ? Optional.empty() : Optional.ofNullable(GENERATORS.get(name));
    }

    /**
     * Returns a registered generator.
     *
     * @param name generator name
     * @return generator
     * @throws org.unitseek.knitid.core.exception.IdGeneratorNotFoundException if
     *         not registered
     */
    public static IdGenerator get(String name) {
        return find(name).orElseThrow(() -> new IdGeneratorNotFoundException(
                IdErrorCodes.GENERATOR_NOT_FOUND,
                "Generator not found: " + name + "; registered=" + GENERATORS.keySet()));
    }

    /**
     * Returns the current default generator.
     *
     * @return default generator
     * @throws org.unitseek.knitid.core.exception.IdGeneratorNotFoundException if
     *         no default is configured
     */
    public static IdGenerator getDefault() {
        String name = defaultName;
        if (name == null) {
            throw new IdGeneratorNotFoundException(
                    IdErrorCodes.GENERATOR_NOT_FOUND, "No default generator registered");
        }
        return get(name);
    }

    /**
     * Returns a snapshot of registered names.
     *
     * @return immutable name set
     */
    public static Set<String> names() {
        return Set.copyOf(GENERATORS.keySet());
    }

    /**
     * Removes all generators and the default selection. Intended for tests.
     */
    public static synchronized void reset() {
        GENERATORS.clear();
        defaultName = null;
    }
}
