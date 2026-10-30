package org.unitseek.knitid.generator.ksuid;

import java.time.Clock;

import org.unitseek.knitid.core.api.IdGenerator;
import org.unitseek.knitid.core.api.IdType;
import org.unitseek.knitid.core.random.PerformanceRandomProvider;
import org.unitseek.knitid.core.random.SecureRandomProvider;
import org.unitseek.knitid.core.spi.GeneratorConfig;
import org.unitseek.knitid.core.spi.IdGeneratorFactory;
import org.unitseek.knitid.core.spi.RandomProvider;

/**
 * Factory for {@link KsuidGenerator}.
 *
 * @since 1.0.0
 */
public final class KsuidIdGeneratorFactory implements IdGeneratorFactory {

    /**
     * {@inheritDoc}
     */
    @Override
    public IdType type() {
        return IdType.KSUID;
    }

    /**
     * Creates a KSUID generator from the {@code ksuid.*} configuration block.
     *
     * @param config generator configuration
     * @return new KSUID generator
     * @throws org.unitseek.knitid.core.exception.IdConfigurationException if
     *         the random source is invalid or an unknown nested key is present
     */
    @Override
    public IdGenerator create(GeneratorConfig config) {
        KsuidConfig ksuidConfig = KsuidConfig.from(config);
        RandomProvider random = ksuidConfig.randomSource() == KsuidRandomSource.SECURE
                ? new SecureRandomProvider()
                : new PerformanceRandomProvider();
        return new KsuidGenerator(Clock.systemUTC(), random);
    }
}
