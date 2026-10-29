package org.unitseek.knitid.generator.uuid8;

import java.time.Clock;

import org.unitseek.knitid.core.api.IdGenerator;
import org.unitseek.knitid.core.api.IdType;
import org.unitseek.knitid.core.random.SecureRandomProvider;
import org.unitseek.knitid.core.spi.GeneratorConfig;
import org.unitseek.knitid.core.spi.IdGeneratorFactory;

/**
 * Factory for {@link Uuid8Generator}.
 */
public final class Uuid8IdGeneratorFactory implements IdGeneratorFactory {

    @Override
    public IdType type() {
        return IdType.UUID8;
    }

    @Override
    public IdGenerator create(GeneratorConfig config) {
        return new Uuid8Generator(Clock.systemUTC(), Uuid8Config.from(config), new SecureRandomProvider());
    }
}
