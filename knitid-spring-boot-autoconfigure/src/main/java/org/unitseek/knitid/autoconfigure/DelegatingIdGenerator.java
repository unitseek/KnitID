package org.unitseek.knitid.autoconfigure;

import org.unitseek.knitid.core.api.IdContext;
import org.unitseek.knitid.core.api.IdGenerator;
import org.unitseek.knitid.core.api.IdGeneratorRegistry;
import org.unitseek.knitid.core.api.IdType;

/**
 * {@code @Primary} {@link IdGenerator} that delegates to the registry default.
 */
final class DelegatingIdGenerator implements IdGenerator {

    private final IdGeneratorRegistry registry;

    DelegatingIdGenerator(IdGeneratorRegistry registry) {
        this.registry = registry;
    }

    @Override
    public IdType type() {
        return registry.getDefault().type();
    }

    @Override
    public String generate() {
        return registry.getDefault().generate();
    }

    @Override
    public String generate(IdContext context) {
        return registry.getDefault().generate(context);
    }
}
