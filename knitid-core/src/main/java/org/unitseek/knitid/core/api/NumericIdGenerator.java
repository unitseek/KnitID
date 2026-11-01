package org.unitseek.knitid.core.api;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.LongStream;

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

    /**
     * Generates a numeric batch asynchronously on the common fork-join pool.
     *
     * @param size batch size
     * @return future containing values in generation order
     * @since 1.0.0
     */
    default CompletableFuture<long[]> generateLongBatchAsync(int size) {
        return generateLongBatchAsync(size, null, java.util.concurrent.ForkJoinPool.commonPool());
    }

    /**
     * Generates a numeric batch asynchronously.
     *
     * @param size batch size
     * @param context context, or {@code null}
     * @param executor executor used to run the batch
     * @return future containing values in generation order
     * @since 1.0.0
     */
    default CompletableFuture<long[]> generateLongBatchAsync(int size, IdContext context, Executor executor) {
        IdGeneratorConstants.checkBatchSize(size);
        Objects.requireNonNull(executor, "executor must not be null");
        return CompletableFuture.supplyAsync(() -> generateLongBatch(size, context), executor);
    }

    /**
     * Returns a lazy sequential stream of numeric identifiers.
     *
     * @param size number of identifiers
     * @return lazy numeric stream
     * @since 1.0.0
     */
    default LongStream generateLongStream(int size) {
        return generateLongStream(size, null);
    }

    /**
     * Returns a lazy sequential stream of numeric identifiers with context.
     *
     * @param size number of identifiers
     * @param context context, or {@code null}
     * @return lazy numeric stream
     * @since 1.0.0
     */
    default LongStream generateLongStream(int size, IdContext context) {
        IdGeneratorConstants.checkBatchSize(size);
        return LongStream.range(0, size).map(ignored -> context == null ? generateLong() : generateLong(context));
    }
}
