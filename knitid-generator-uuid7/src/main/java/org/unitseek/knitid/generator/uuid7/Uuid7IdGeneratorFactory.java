package org.unitseek.knitid.generator.uuid7;

import java.time.Clock;

import org.unitseek.knitid.core.api.IdGenerator;
import org.unitseek.knitid.core.api.IdType;
import org.unitseek.knitid.core.random.SecureRandomProvider;
import org.unitseek.knitid.core.spi.GeneratorConfig;
import org.unitseek.knitid.core.spi.IdGeneratorFactory;

/**
 * Factory for {@link Uuid7Generator}.
 *
 * @since 1.0.0
 */
public final class Uuid7IdGeneratorFactory implements IdGeneratorFactory {

    /**
     * {@inheritDoc}
     */
    @Override
    public IdType type() {
        return IdType.UUID7;
    }

    /**
     * Creates a UUIDv7 generator from the {@code uuid7.*} configuration block.
     *
     * @param config generator configuration
     * @return new UUIDv7 generator
     * @throws org.unitseek.knitid.core.exception.IdConfigurationException if
     *         {@code uuid7.monotonic-overflow} is invalid
     */
    @Override
    public IdGenerator create(GeneratorConfig config) {
        boolean monotonic = config.getBoolean("uuid7.monotonic", true);
        MonotonicOverflow overflow = Uuid7Generator.parseOverflow(
                config.getString("uuid7.monotonic-overflow", MonotonicOverflow.WAIT.name()));
        return new Uuid7Generator(Clock.systemUTC(), new SecureRandomProvider(), monotonic, overflow);
    }
}
