package org.unitseek.knitid.core.spi;

import org.unitseek.knitid.core.api.IdGenerator;
import org.unitseek.knitid.core.api.IdType;

/**
 * Factory registered by an algorithm module. The central auto-configuration
 * uses it to instantiate generators from {@code knitid.generators.*}.
 *
 * @since 1.0.0
 */
public interface IdGeneratorFactory {

    /**
     * Returns the type created by this factory.
     *
     * @return identifier type
     */
    IdType type();

    /**
     * Creates a generator from configuration.
     *
     * @param config generator configuration
     * @return generator
     * @throws org.unitseek.knitid.core.exception.IdConfigurationException if
     *         configuration is invalid
     */
    IdGenerator create(GeneratorConfig config);
}
