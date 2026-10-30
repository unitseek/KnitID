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

    /**
     * Creates a factory with system clock and only the built-in {@code memory}
     * sequence provider.
     */
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

    /**
     * Creates a factory with a custom sequence-provider resolver.
     *
     * @param clock timestamp source
     * @param sequenceProviderResolver resolver for {@code biz.sequence-provider}
     */
    public BizIdGeneratorFactory(Clock clock, SequenceProviderResolver sequenceProviderResolver) {
        this.clock = java.util.Objects.requireNonNull(clock, "clock must not be null");
        this.sequenceProviderResolver = java.util.Objects.requireNonNull(
                sequenceProviderResolver, "sequenceProviderResolver must not be null");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IdType type() {
        return IdType.BIZ;
    }

    /**
     * Creates a business-ID generator from the {@code biz.*} configuration block.
     *
     * @param config generator configuration
     * @return new business-ID generator
     * @throws IdConfigurationException if the template, prefix, sequence
     *         provider or check algorithm is invalid
     */
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
