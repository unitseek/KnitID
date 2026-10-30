package org.unitseek.knitid.generator.ulid;

/**
 * Behaviour when the monotonic random space of the current millisecond is exhausted.
 */
public enum UlidMonotonicOverflow {

    /** Wait for the next millisecond. */
    WAIT,

    /** Fail with an {@link org.unitseek.knitid.core.exception.IdGenerationException}. */
    THROW
}
