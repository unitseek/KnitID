package org.unitseek.knitid.core.spi;

import org.unitseek.knitid.core.api.IdGenerator;

/**
 * Decorates a generator before it is registered (for example with metrics).
 *
 * <p>Decorators are applied in bean order by the Spring auto-configuration.</p>
 *
 * @since 1.0.0
 */
@FunctionalInterface
public interface IdGeneratorDecorator {

    /**
     * Wraps a generator.
     *
     * @param generator original generator
     * @param name registered generator name
     * @return decorated generator
     */
    IdGenerator decorate(IdGenerator generator, String name);
}
