package org.unitseek.knitid.generator.uuid8;

import java.time.Clock;

import org.unitseek.knitid.core.api.IdGenerator;
import org.unitseek.knitid.core.api.IdType;
import org.unitseek.knitid.core.random.SecureRandomProvider;
import org.unitseek.knitid.core.spi.GeneratorConfig;
import org.unitseek.knitid.core.spi.IdGeneratorFactory;

/**
 * Factory for {@link Uuid8Generator}.
 *
 * @since 1.0.0
 */
public final class Uuid8IdGeneratorFactory implements IdGeneratorFactory {

    /**
     * {@inheritDoc}
     */
    @Override
    public IdType type() {
        return IdType.UUID8;
    }

    /**
     * Creates a UUIDv8 generator from the {@code uuid8.*} configuration block.
     *
     * @param config generator configuration
     * @return new UUIDv8 generator
     * @throws org.unitseek.knitid.core.exception.IdConfigurationException if
     *         the preset or node ids are invalid
     */
    @Override
    public IdGenerator create(GeneratorConfig config) {
        return new Uuid8Generator(Clock.systemUTC(), Uuid8Config.from(config), new SecureRandomProvider());
    }
}
