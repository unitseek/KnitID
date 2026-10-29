package org.unitseek.knitid.generator.uuid7;

/**
 * Behaviour when the monotonic random space of the current millisecond is exhausted.
 */
public enum MonotonicOverflow {

    /** Wait for the next millisecond. */
    WAIT,

    /** Throw an {@link org.unitseek.knitid.core.exception.IdGenerationException}. */
    THROW
}
