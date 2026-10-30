package org.unitseek.knitid.core.spi;

/**
 * Random source abstraction.
 *
 * @since 1.0.0
 */
public interface RandomProvider {

    /**
     * Fills the array with random bytes.
     *
     * @param bytes destination array
     */
    void nextBytes(byte[] bytes);

    /**
     * Returns a random int in {@code [0, bound)}.
     *
     * @param bound exclusive upper bound
     * @return random int
     */
    int nextInt(int bound);

    /**
     * Indicates whether this source is cryptographically secure.
     *
     * @return {@code true} for a cryptographically strong source
     */
    boolean secure();
}
