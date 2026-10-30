package org.unitseek.knitid.core.api;

/**
 * Shared constants and argument checks.
 *
 * @since 1.0.0
 */
public final class IdGeneratorConstants {

    /** Default upper bound for a single batch call. */
    public static final int DEFAULT_MAX_BATCH_SIZE = 10_000;

    private static volatile int maxBatchSize = DEFAULT_MAX_BATCH_SIZE;

    private IdGeneratorConstants() {
    }

    /**
     * Returns the current process-wide batch limit.
     *
     * @return maximum batch size
     */
    public static int maxBatchSize() {
        return maxBatchSize;
    }

    /**
     * Configures the process-wide batch limit. Spring Boot auto-configuration
     * applies {@code knitid.limits.max-batch-size} here.
     *
     * @param value positive batch limit
     * @throws IllegalArgumentException if value is not positive
     */
    public static void configureMaxBatchSize(int value) {
        if (value <= 0) {
            throw new IllegalArgumentException("max batch size must be > 0: " + value);
        }
        maxBatchSize = value;
    }

    /**
     * Restores the default batch limit.
     */
    public static void resetMaxBatchSize() {
        maxBatchSize = DEFAULT_MAX_BATCH_SIZE;
    }

    /**
     * Validates a requested batch size against the current limit.
     *
     * @param size batch size
     * @throws IllegalArgumentException if size is non-positive or exceeds the
     *         configured limit
     */
    public static void checkBatchSize(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("batch size must be > 0: " + size);
        }
        int limit = maxBatchSize;
        if (size > limit) {
            throw new IllegalArgumentException(
                    "batch size must be <= " + limit + ": " + size);
        }
    }
}
