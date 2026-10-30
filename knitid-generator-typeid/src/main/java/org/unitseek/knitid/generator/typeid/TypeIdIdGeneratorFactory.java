package org.unitseek.knitid.generator.typeid;

import java.time.Clock;

import org.unitseek.knitid.core.api.IdGenerator;
import org.unitseek.knitid.core.api.IdType;
import org.unitseek.knitid.core.random.SecureRandomProvider;
import org.unitseek.knitid.core.spi.GeneratorConfig;
import org.unitseek.knitid.core.spi.IdGeneratorFactory;

/**
 * Factory for {@link TypeIdGenerator}.
 *
 * @since 1.0.0
 */
public final class TypeIdIdGeneratorFactory implements IdGeneratorFactory {

    /**
     * {@inheritDoc}
     */
    @Override
    public IdType type() {
        return IdType.TYPEID;
    }

    /**
     * Creates a TypeID generator from the {@code typeid.*} configuration block.
     *
     * @param config generator configuration
     * @return new TypeID generator
     * @throws org.unitseek.knitid.core.exception.IdConfigurationException if
     *         prefixes are missing/invalid or an unknown nested key is present
     */
    @Override
    public IdGenerator create(GeneratorConfig config) {
        return new TypeIdGenerator(Clock.systemUTC(), new SecureRandomProvider(), TypeIdConfig.from(config));
    }
}
