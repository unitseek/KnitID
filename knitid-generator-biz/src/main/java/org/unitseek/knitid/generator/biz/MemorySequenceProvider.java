package org.unitseek.knitid.generator.biz;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.unitseek.knitid.core.spi.SequenceProvider;

/**
 * In-memory sequence provider. Unique per JVM and per scope; suitable for
 * single-instance usage and examples, not for multi-instance deployments.
 */
public final class MemorySequenceProvider implements SequenceProvider {

    private static final int MAX_SCOPES = 10_000;

    private final Map<String, AtomicLong> sequences = Collections.synchronizedMap(
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, AtomicLong> eldest) {
                    return size() > MAX_SCOPES;
                }
            });

    /**
     * Returns the next value for a scope.
     *
     * <p>Only the most recent 10,000 scopes are retained in memory.</p>
     *
     * @param scope non-null sequence scope
     * @return next sequence value
     */
    @Override
    public long next(String scope) {
        if (scope == null) {
            throw new IllegalArgumentException("scope must not be null");
        }
        synchronized (sequences) {
            return sequences.computeIfAbsent(scope, key -> new AtomicLong()).getAndIncrement();
        }
    }
}
