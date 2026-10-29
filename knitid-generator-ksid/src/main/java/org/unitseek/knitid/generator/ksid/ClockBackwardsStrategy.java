package org.unitseek.knitid.generator.ksid;

/**
 * Strategy used when the system clock moves backwards.
 */
public enum ClockBackwardsStrategy {

    /** Block until the clock catches up (bounded by max-wait). */
    WAIT,

    /** Fail immediately with {@link org.unitseek.knitid.core.exception.ClockBackwardsException}. */
    THROW,

    /** Keep the logical timestamp and continue generating (explicit opt-in). */
    BORROW_FUTURE
}
