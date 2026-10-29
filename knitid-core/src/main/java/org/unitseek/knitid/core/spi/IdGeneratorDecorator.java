package org.unitseek.knitid.core.spi;

import org.unitseek.knitid.core.api.IdGenerator;

/**
 * Decorates a generator before it is registered (for example with metrics).
 */
@FunctionalInterface
public interface IdGeneratorDecorator {

    IdGenerator decorate(IdGenerator generator, String name);
}
