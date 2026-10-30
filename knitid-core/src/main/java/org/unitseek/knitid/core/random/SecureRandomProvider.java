package org.unitseek.knitid.core.random;

import java.security.SecureRandom;

import org.unitseek.knitid.core.spi.RandomProvider;

/**
 * Thread-local {@link SecureRandom} provider (default for UUIDv7/UUIDv8).
 *
 * @since 1.0.0
 */
public final class SecureRandomProvider implements RandomProvider {

    private static final ThreadLocal<SecureRandom> RANDOM =
            ThreadLocal.withInitial(SecureRandom::new);

    /**
     * {@inheritDoc}
     */
    @Override
    public void nextBytes(byte[] bytes) {
        RANDOM.get().nextBytes(bytes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int nextInt(int bound) {
        return RANDOM.get().nextInt(bound);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean secure() {
        return true;
    }
}
