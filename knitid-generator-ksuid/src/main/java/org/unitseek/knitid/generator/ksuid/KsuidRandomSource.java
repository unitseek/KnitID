package org.unitseek.knitid.generator.ksuid;

/**
 * Random source used by the KSUID generator.
 *
 * @since 1.0.0
 */
public enum KsuidRandomSource {
    /**
     * Cryptographically strong source based on {@link java.security.SecureRandom}.
     * This is the default and the only production-safe choice.
     */
    SECURE,
    /**
     * Faster non-cryptographic source.
     *
     * <p>Intended only for benchmarks and tests; do not use where IDs must be
     * unpredictable or resistant to guessing.</p>
     */
    PERFORMANCE
}
