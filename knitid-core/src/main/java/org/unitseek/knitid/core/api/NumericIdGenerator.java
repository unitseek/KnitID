package org.unitseek.knitid.core.api;

/**
 * Identifier generator that can return numeric values.
 *
 * @since 1.0.0
 */
public interface NumericIdGenerator extends IdGenerator {

    /**
     * Generates one numeric identifier.
     *
     * @return numeric identifier
     * @throws org.unitseek.knitid.core.exception.IdGenerationException if
     *         generation fails
     */
    long generateLong();

    /**
     * Generates one numeric identifier with optional per-call context.
     *
     * @param context context, or {@code null}
     * @return numeric identifier
     * @throws org.unitseek.knitid.core.exception.IdGenerationException if
     *         generation fails
     */
    long generateLong(IdContext context);

    /**
     * Generates a numeric batch.
     *
     * @param size batch size; must be positive and within the configured limit
     * @return numeric identifiers in generation order
     * @throws IllegalArgumentException if size is invalid
     */
    default long[] generateLongBatch(int size) {
        return generateLongBatch(size, null);
    }

    /**
     * Generates a numeric batch with optional context.
     *
     * @param size batch size; must be positive and within the configured limit
     * @param context context, or {@code null}
     * @return numeric identifiers in generation order
     * @throws IllegalArgumentException if size is invalid
     */
    default long[] generateLongBatch(int size, IdContext context) {
        IdGeneratorConstants.checkBatchSize(size);
        long[] result = new long[size];
        for (int i = 0; i < size; i++) {
            result[i] = context == null ? generateLong() : generateLong(context);
        }
        return result;
    }
}
