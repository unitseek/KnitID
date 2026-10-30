package org.unitseek.knitid.core.api;

import java.util.ArrayList;
import java.util.List;

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
}
