package org.unitseek.knitid.core.api;

/**
 * Shared constants and argument checks.
 */
public final class IdGeneratorConstants {

    /** Default upper bound for a single batch call. */
    public static final int DEFAULT_MAX_BATCH_SIZE = 10_000;

    private static volatile int maxBatchSize = DEFAULT_MAX_BATCH_SIZE;

    private IdGeneratorConstants() {
    }

    public static int maxBatchSize() {
        return maxBatchSize;
    }

    /**
     * Configures the process-wide batch limit. Spring Boot auto-configuration
     * applies {@code knitid.limits.max-batch-size} here.
     */
    public static void configureMaxBatchSize(int value) {
        if (value <= 0) {
            throw new IllegalArgumentException("max batch size must be > 0: " + value);
        }
        maxBatchSize = value;
    }

    public static void resetMaxBatchSize() {
        maxBatchSize = DEFAULT_MAX_BATCH_SIZE;
    }

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
