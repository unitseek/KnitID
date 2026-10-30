package org.unitseek.knitid.generator.ulid;

import java.time.Clock;

import org.unitseek.knitid.core.api.IdGenerator;
import org.unitseek.knitid.core.api.IdType;
import org.unitseek.knitid.core.random.PerformanceRandomProvider;
import org.unitseek.knitid.core.random.SecureRandomProvider;
import org.unitseek.knitid.core.spi.GeneratorConfig;
import org.unitseek.knitid.core.spi.IdGeneratorFactory;
import org.unitseek.knitid.core.spi.RandomProvider;

/**
 * Factory for {@link UlidGenerator}.
 *
 * @since 1.0.0
 */
public final class UlidIdGeneratorFactory implements IdGeneratorFactory {

    /**
     * {@inheritDoc}
     */
    @Override
    public IdType type() {
        return IdType.ULID;
    }

    /**
     * Creates a ULID generator from the {@code ulid.*} configuration block.
     *
     * @param config generator configuration
     * @return new ULID generator
     * @throws org.unitseek.knitid.core.exception.IdConfigurationException if
     *         an enum value is invalid
     */
    @Override
    public IdGenerator create(GeneratorConfig config) {
        UlidConfig ulidConfig = UlidConfig.from(config);
        RandomProvider random = ulidConfig.randomSource() == UlidRandomSource.SECURE
                ? new SecureRandomProvider()
                : new PerformanceRandomProvider();
        return new UlidGenerator(Clock.systemUTC(), random, ulidConfig.monotonic(),
                ulidConfig.monotonicOverflow());
    }
}
