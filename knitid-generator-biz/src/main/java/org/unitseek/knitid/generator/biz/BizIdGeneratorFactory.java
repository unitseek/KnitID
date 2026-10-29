package org.unitseek.knitid.generator.biz;

import java.time.Clock;

import org.unitseek.knitid.core.api.IdGenerator;
import org.unitseek.knitid.core.api.IdType;
import org.unitseek.knitid.core.exception.IdConfigurationException;
import org.unitseek.knitid.core.exception.IdErrorCodes;
import org.unitseek.knitid.core.random.SecureRandomProvider;
import org.unitseek.knitid.core.spi.GeneratorConfig;
import org.unitseek.knitid.core.spi.IdGeneratorFactory;
import org.unitseek.knitid.core.spi.SequenceProvider;

/**
 * Factory for {@link BizIdGenerator}.
 */
public final class BizIdGeneratorFactory implements IdGeneratorFactory {

    private final Clock clock;

    private final SequenceProviderResolver sequenceProviderResolver;

    public BizIdGeneratorFactory() {
        this(Clock.systemUTC(), name -> {
            if ("memory".equalsIgnoreCase(name)) {
                return new MemorySequenceProvider();
            }
            throw new IdConfigurationException(
                    IdErrorCodes.CONFIG_INVALID,
                    "Unknown biz.sequence-provider: " + name + " (built-in: memory)");
        });
    }

    public BizIdGeneratorFactory(Clock clock, SequenceProviderResolver sequenceProviderResolver) {
        this.clock = java.util.Objects.requireNonNull(clock, "clock must not be null");
        this.sequenceProviderResolver = java.util.Objects.requireNonNull(
                sequenceProviderResolver, "sequenceProviderResolver must not be null");
    }

    @Override
    public IdType type() {
        return IdType.BIZ;
    }

    @Override
    public IdGenerator create(GeneratorConfig config) {
        java.util.Objects.requireNonNull(config, "config must not be null");
        BizConfig bizConfig = BizConfig.from(config);
        BizTemplate template = BizTemplate.compile(bizConfig);
        SequenceProvider sequenceProvider = sequenceProviderResolver.resolve(bizConfig.sequenceProviderName());
        if (sequenceProvider == null) {
            throw new IdConfigurationException(
                    IdErrorCodes.CONFIG_INVALID,
                    "No SequenceProvider for name " + bizConfig.sequenceProviderName());
        }
        return new BizIdGenerator(clock, bizConfig, template, sequenceProvider, new SecureRandomProvider());
    }
}
