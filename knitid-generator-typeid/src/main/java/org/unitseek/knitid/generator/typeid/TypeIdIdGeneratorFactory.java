package org.unitseek.knitid.generator.typeid;

import java.time.Clock;

import org.unitseek.knitid.core.api.IdGenerator;
import org.unitseek.knitid.core.api.IdType;
import org.unitseek.knitid.core.random.SecureRandomProvider;
import org.unitseek.knitid.core.spi.GeneratorConfig;
import org.unitseek.knitid.core.spi.IdGeneratorFactory;

/**
 * Factory for {@link TypeIdGenerator}.
 */
public final class TypeIdIdGeneratorFactory implements IdGeneratorFactory {

    @Override
    public IdType type() {
        return IdType.TYPEID;
    }

    @Override
    public IdGenerator create(GeneratorConfig config) {
        return new TypeIdGenerator(Clock.systemUTC(), new SecureRandomProvider(), TypeIdConfig.from(config));
    }
}
