package org.unitseek.knitid.core.spi;

import org.unitseek.knitid.core.api.IdGenerator;
import org.unitseek.knitid.core.api.IdType;

/**
 * Factory registered by an algorithm module. The central auto-configuration
 * uses it to instantiate generators from {@code knitid.generators.*}.
 */
public interface IdGeneratorFactory {

    IdType type();

    IdGenerator create(GeneratorConfig config);
}
