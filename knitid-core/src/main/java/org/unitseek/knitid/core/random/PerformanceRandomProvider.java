package org.unitseek.knitid.core.random;

import java.util.concurrent.ThreadLocalRandom;

import org.unitseek.knitid.core.spi.RandomProvider;

/**
 * Non-cryptographic random provider based on {@link ThreadLocalRandom}.
 *
 * <p>Not suitable for security tokens; intended for ULID performance mode.</p>
 *
 * @since 1.0.0
 */
public final class PerformanceRandomProvider implements RandomProvider {

    /**
     * {@inheritDoc}
     *
     * <p>Not cryptographically secure.</p>
     */
    @Override
    public void nextBytes(byte[] bytes) {
        int index = 0;
        while (index + Long.BYTES <= bytes.length) {
            long value = ThreadLocalRandom.current().nextLong();
            for (int i = 0; i < Long.BYTES; i++) {
                bytes[index + i] = (byte) (value >>> (56 - (8 * i)));
            }
            index += Long.BYTES;
        }
        if (index < bytes.length) {
            long value = ThreadLocalRandom.current().nextLong();
            for (int i = 0; index < bytes.length; index++, i++) {
                bytes[index] = (byte) (value >>> (56 - (8 * i)));
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int nextInt(int bound) {
        return ThreadLocalRandom.current().nextInt(bound);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean secure() {
        return false;
    }
}
