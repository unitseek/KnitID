package org.unitseek.knitid.generator.uuid7;

import java.time.Clock;

import org.unitseek.knitid.core.api.IdGenerator;
import org.unitseek.knitid.core.api.IdType;
import org.unitseek.knitid.core.random.SecureRandomProvider;
import org.unitseek.knitid.core.spi.GeneratorConfig;
import org.unitseek.knitid.core.spi.IdGeneratorFactory;

/**
 * Factory for {@link Uuid7Generator}.
 */
public final class Uuid7IdGeneratorFactory implements IdGeneratorFactory {

    @Override
    public IdType type() {
        return IdType.UUID7;
    }

    @Override
    public IdGenerator create(GeneratorConfig config) {
        boolean monotonic = config.getBoolean("uuid7.monotonic", true);
        MonotonicOverflow overflow = Uuid7Generator.parseOverflow(
                config.getString("uuid7.monotonic-overflow", MonotonicOverflow.WAIT.name()));
        return new Uuid7Generator(Clock.systemUTC(), new SecureRandomProvider(), monotonic, overflow);
    }
}
