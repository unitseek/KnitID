package org.unitseek.knitid.generator.uuid7;

/**
 * Behaviour when the monotonic random space of the current millisecond is exhausted.
 *
 * @since 1.0.0
 */
public enum MonotonicOverflow {

    /** Wait for the next millisecond. */
    WAIT,

    /** Throw an {@link org.unitseek.knitid.core.exception.IdGenerationException}. */
    THROW
}
