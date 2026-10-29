package org.unitseek.knitid.generator.ulid;

/**
 * Random source used for the 80 random bits of a ULID.
 */
public enum UlidRandomSource {

    /** Non-cryptographic, faster source (default). */
    PERFORMANCE,

    /** Cryptographically secure source. */
    SECURE
}
