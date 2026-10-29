package org.unitseek.knitid.core.spi;

/**
 * Sequence source used by business identifiers.
 */
public interface SequenceProvider {

    long next(String scope);

    /**
     * Allocates the next value for a long-lived scope that must not expire.
     * Implementations that cannot distinguish persistence may ignore this hint.
     */
    default long next(String scope, boolean persistent) {
        return next(scope);
    }

    default long[] next(String scope, int count) {
        return next(scope, count, false);
    }

    default long[] next(String scope, int count, boolean persistent) {
        if (count <= 0) {
            throw new IllegalArgumentException("count must be > 0: " + count);
        }
        long[] values = new long[count];
        for (int i = 0; i < count; i++) {
            values[i] = next(scope, persistent);
        }
        return values;
    }
}
