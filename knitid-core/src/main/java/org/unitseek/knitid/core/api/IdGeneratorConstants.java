package org.unitseek.knitid.core.api;

/**
 * Shared constants and argument checks.
 */
public final class IdGeneratorConstants {

    /** Default upper bound for a single batch call. */
    public static final int DEFAULT_MAX_BATCH_SIZE = 10_000;

    private IdGeneratorConstants() {
    }

    public static void checkBatchSize(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("batch size must be > 0: " + size);
        }
        if (size > DEFAULT_MAX_BATCH_SIZE) {
            throw new IllegalArgumentException(
                    "batch size must be <= " + DEFAULT_MAX_BATCH_SIZE + ": " + size);
        }
    }
}
