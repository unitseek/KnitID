package org.unitseek.knitid.generator.biz;

import org.unitseek.knitid.core.spi.SequenceProvider;

/**
 * Resolves a sequence provider by configuration name.
 */
@FunctionalInterface
public interface SequenceProviderResolver {

    SequenceProvider resolve(String name);
}
