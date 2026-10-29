package org.unitseek.knitid.core.api;

/**
 * Identifier generator that can return numeric values.
 */
public interface NumericIdGenerator extends IdGenerator {

    long generateLong();

    long generateLong(IdContext context);

    default long[] generateLongBatch(int size) {
        return generateLongBatch(size, null);
    }

    default long[] generateLongBatch(int size, IdContext context) {
        IdGeneratorConstants.checkBatchSize(size);
        long[] result = new long[size];
        for (int i = 0; i < size; i++) {
            result[i] = context == null ? generateLong() : generateLong(context);
        }
        return result;
    }
}
