package org.unitseek.knitid.generator.biz;

/**
 * Sequence reset policy for business identifiers.
 */
public enum SequenceReset {

    /** Reset per local date in the configured zone. */
    DAILY,

    /** Never reset. */
    NEVER
}
