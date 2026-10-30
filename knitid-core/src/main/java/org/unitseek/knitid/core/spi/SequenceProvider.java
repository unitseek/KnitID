package org.unitseek.knitid.core.spi;

/**
 * Sequence source used by business identifiers.
 *
 * @since 1.0.0
 */
public interface SequenceProvider {

    /**
     * Returns the next value for a scope.
     *
     * @param scope sequence scope
     * @return next value
     */
    long next(String scope);

    /**
     * Allocates the next value for a long-lived scope that must not expire.
     * Implementations that cannot distinguish persistence may ignore this hint.
     */
    default long next(String scope, boolean persistent) {
        return next(scope);
    }

    /**
     * Allocates a batch of values.
     *
     * @param scope sequence scope
     * @param count number of values
     * @return allocated values
     */
    default long[] next(String scope, int count) {
        return next(scope, count, false);
    }

    /**
     * Allocates a batch of values with a persistence hint.
     *
     * @param scope sequence scope
     * @param count number of values
     * @param persistent whether the scope must not expire
     * @return allocated values
     * @throws IllegalArgumentException if count is non-positive
     */
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
