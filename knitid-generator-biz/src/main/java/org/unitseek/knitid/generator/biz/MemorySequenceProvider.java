package org.unitseek.knitid.generator.biz;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.unitseek.knitid.core.spi.SequenceProvider;

/**
 * In-memory sequence provider. Unique per JVM and per scope; suitable for
 * single-instance usage and examples, not for multi-instance deployments.
 */
public final class MemorySequenceProvider implements SequenceProvider {

    private final ConcurrentHashMap<String, AtomicLong> sequences = new ConcurrentHashMap<>();

    @Override
    public long next(String scope) {
        if (scope == null) {
            throw new IllegalArgumentException("scope must not be null");
        }
        return sequences.computeIfAbsent(scope, key -> new AtomicLong()).getAndIncrement();
    }
}
