package org.unitseek.knitid.generator.ksid;

/**
 * Strategy used when the per-millisecond sequence is exhausted.
 */
public enum SequenceOverflowStrategy {

    /** Wait for the next millisecond. */
    WAIT,

    /** Fail with {@link org.unitseek.knitid.core.exception.IdGenerationException}. */
    THROW
}
