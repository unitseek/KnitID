package org.unitseek.knitid.core.spi;

import org.unitseek.knitid.core.api.IdGenerator;

/**
 * Customizes a generator before it is registered.
 */
public interface IdGeneratorCustomizer {

    IdGenerator customize(IdGenerator generator, String name);
}
