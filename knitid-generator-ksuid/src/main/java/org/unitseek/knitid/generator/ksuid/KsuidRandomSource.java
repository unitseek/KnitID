package org.unitseek.knitid.generator.ksuid;

/**
 * Random source used by the KSUID generator.
 */
public enum KsuidRandomSource {
    /** Cryptographically strong default; use in production. */
    SECURE,
    /** Faster non-cryptographic random; intended only for benchmarks and tests. */
    PERFORMANCE
}
