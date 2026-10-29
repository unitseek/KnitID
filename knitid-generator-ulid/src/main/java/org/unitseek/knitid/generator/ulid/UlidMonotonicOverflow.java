package org.unitseek.knitid.generator.ulid;

/**
 * Behaviour when the monotonic random space of the current millisecond is exhausted.
 */
public enum UlidMonotonicOverflow {

    WAIT,

    THROW
}
