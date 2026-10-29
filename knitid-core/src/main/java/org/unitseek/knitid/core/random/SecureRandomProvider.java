package org.unitseek.knitid.core.random;

import java.security.SecureRandom;

import org.unitseek.knitid.core.spi.RandomProvider;

/**
 * Thread-local {@link SecureRandom} provider (default for UUIDv7/UUIDv8).
 */
public final class SecureRandomProvider implements RandomProvider {

    private static final ThreadLocal<SecureRandom> RANDOM =
            ThreadLocal.withInitial(SecureRandom::new);

    @Override
    public void nextBytes(byte[] bytes) {
        RANDOM.get().nextBytes(bytes);
    }

    @Override
    public int nextInt(int bound) {
        return RANDOM.get().nextInt(bound);
    }

    @Override
    public boolean secure() {
        return true;
    }
}
