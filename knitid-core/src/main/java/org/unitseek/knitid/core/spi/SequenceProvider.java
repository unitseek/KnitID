package org.unitseek.knitid.core.spi;

/**
 * Sequence source used by business identifiers.
 */
public interface SequenceProvider {

    long next(String scope);

    default long[] next(String scope, int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("count must be > 0: " + count);
        }
        long[] values = new long[count];
        for (int i = 0; i < count; i++) {
            values[i] = next(scope);
        }
        return values;
    }
}
