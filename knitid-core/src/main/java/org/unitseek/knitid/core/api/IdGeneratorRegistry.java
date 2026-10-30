package org.unitseek.knitid.core.api;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Read-only registry of generators.
 *
 * @since 1.0.0
 */
public interface IdGeneratorRegistry {

    /**
     * Returns the configured default generator.
     *
     * @return default generator
     * @throws org.unitseek.knitid.core.exception.IdGeneratorNotFoundException if
     *         no default is configured
     */
    IdGenerator getDefault();

    /**
     * Returns a named generator.
     *
     * @param name generator name
     * @return generator
     * @throws org.unitseek.knitid.core.exception.IdGeneratorNotFoundException if
     *         no generator has this name
     */
    IdGenerator get(String name);

    /**
     * Finds a generator by name without throwing.
     *
     * @param name generator name, possibly {@code null}
     * @return matching generator
     */
    Optional<IdGenerator> find(String name);

    /**
     * Finds the first generator of a type without throwing.
     *
     * @param type identifier type, possibly {@code null}
     * @return matching generator
     */
    Optional<IdGenerator> find(IdType type);

    /**
     * Returns the configured generator names.
     *
     * @return immutable set of names
     */
    Set<String> names();

    /**
     * Returns an immutable name-to-generator view.
     *
     * @return immutable map of generators
     */
    Map<String, IdGenerator> asMap();
}
