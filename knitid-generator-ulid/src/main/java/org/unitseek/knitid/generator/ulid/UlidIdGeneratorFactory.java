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
 */
public final class UlidIdGeneratorFactory implements IdGeneratorFactory {

    @Override
    public IdType type() {
        return IdType.ULID;
    }

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
