package org.unitseek.knitid.core.spi;

import org.unitseek.knitid.core.api.IdGenerator;

/**
 * Customizes a generator before it is registered.
 *
 * <p><strong>Reserved extension point:</strong> this SPI is not wired into the
 * registry in 1.0.0. Use {@link IdGeneratorDecorator} instead.</p>
 *
 * @since 1.0.0
 */
public interface IdGeneratorCustomizer {

    /**
     * Customizes a generator.
     *
     * @param generator original generator
     * @param name registered generator name
     * @return customized generator
     */
    IdGenerator customize(IdGenerator generator, String name);
}
