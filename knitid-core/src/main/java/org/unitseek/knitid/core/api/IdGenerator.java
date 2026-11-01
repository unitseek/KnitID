package org.unitseek.knitid.core.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Unified identifier generator contract.
 *
 * <p>Implementations must be thread-safe. Some implementations may block
 * (for example KSID with the WAIT clock-backwards strategy); such behaviour
 * must be documented by the implementation.</p>
 *
 * @since 1.0.0
 */
public interface IdGenerator {

    /**
     * Returns the identifier type produced by this generator.
     *
     * @return identifier type
     */
    IdType type();

    /**
     * Generates one identifier.
     *
     * @return identifier
     * @throws org.unitseek.knitid.core.exception.IdGenerationException if
     *         generation fails
     */
    String generate();

    /**
     * Generates one identifier with optional per-call context.
     *
     * @param context context, or {@code null} for no context
     * @return identifier
     * @throws org.unitseek.knitid.core.exception.IdGenerationException if
     *         generation fails
     */
    String generate(IdContext context);

    /**
     * Indicates whether {@link #generate()} may block.
     *
     * <p>The default is {@code false}; time-based generators with WAIT
     * policies override this.</p>
     *
     * @return {@code true} if generation may block
     */
    default boolean blocking() {
        return false;
    }

    /**
     * Generates a batch of identifiers.
     *
     * @param size batch size; must be positive and within the configured limit
     * @return identifiers in generation order
     * @throws IllegalArgumentException if size is invalid
     */
    default List<String> generateBatch(int size) {
        return generateBatch(size, null);
    }

    /**
     * Generates a batch of identifiers with an optional context.
     *
     * @param size batch size; must be positive and within the configured limit
     * @param context context, or {@code null}
     * @return identifiers in generation order
     * @throws IllegalArgumentException if size is invalid
     */
    default List<String> generateBatch(int size, IdContext context) {
        IdGeneratorConstants.checkBatchSize(size);
        List<String> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            result.add(context == null ? generate() : generate(context));
        }
        return result;
    }

    /**
     * Generates a batch asynchronously on the common fork-join pool.
     *
     * @param size batch size; must be positive and within the configured limit
     * @return future containing identifiers in generation order
     * @since 1.0.0
     */
    default CompletableFuture<List<String>> generateBatchAsync(int size) {
        return generateBatchAsync(size, null, java.util.concurrent.ForkJoinPool.commonPool());
    }

    /**
     * Generates a batch asynchronously on the supplied executor.
     *
     * @param size batch size; must be positive and within the configured limit
     * @param executor executor used to run the batch
     * @return future containing identifiers in generation order
     * @since 1.0.0
     */
    default CompletableFuture<List<String>> generateBatchAsync(int size, Executor executor) {
        return generateBatchAsync(size, null, executor);
    }

    /**
     * Generates a batch asynchronously with an optional context.
     *
     * @param size batch size; must be positive and within the configured limit
     * @param context context, or {@code null}
     * @param executor executor used to run the batch
     * @return future containing identifiers in generation order
     * @since 1.0.0
     */
    default CompletableFuture<List<String>> generateBatchAsync(int size, IdContext context, Executor executor) {
        IdGeneratorConstants.checkBatchSize(size);
        Objects.requireNonNull(executor, "executor must not be null");
        return CompletableFuture.supplyAsync(() -> generateBatch(size, context), executor);
    }

    /**
     * Returns a lazy sequential stream of identifiers.
     *
     * <p>The batch limit is checked before the stream is returned. Generation
     * happens when the stream is consumed.</p>
     *
     * @param size number of identifiers
     * @return lazy identifier stream
     * @since 1.0.0
     */
    default Stream<String> generateStream(int size) {
        return generateStream(size, null);
    }

    /**
     * Returns a lazy sequential stream of identifiers with optional context.
     *
     * @param size number of identifiers
     * @param context context, or {@code null}
     * @return lazy identifier stream
     * @since 1.0.0
     */
    default Stream<String> generateStream(int size, IdContext context) {
        IdGeneratorConstants.checkBatchSize(size);
        return IntStream.range(0, size).mapToObj(ignored -> context == null ? generate() : generate(context));
    }
}
