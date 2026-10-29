package org.unitseek.knitid.generator.uuid8;

/**
 * Fixed UUIDv8 layouts supported by KnitID.
 */
public enum Uuid8Preset {

    /** 48-bit timestamp, 12-bit monotonic counter, 62 random bits. */
    TIME_RANDOM,

    /** 48-bit timestamp, 4-bit datacenter + 8-bit worker, 62 random bits. */
    TIME_NODE_RANDOM,

    /** 48 random bits, 12 random bits, 62 random bits. */
    RANDOM
}
