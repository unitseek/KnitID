package org.unitseek.knitid.core.api;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Read-only registry of generators.
 */
public interface IdGeneratorRegistry {

    IdGenerator getDefault();

    IdGenerator get(String name);

    Optional<IdGenerator> find(String name);

    Optional<IdGenerator> find(IdType type);

    Set<String> names();

    Map<String, IdGenerator> asMap();
}
