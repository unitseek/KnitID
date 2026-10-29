package org.unitseek.knitid.core.random;

import java.util.concurrent.ThreadLocalRandom;

import org.unitseek.knitid.core.spi.RandomProvider;

/**
 * Non-cryptographic random provider based on {@link ThreadLocalRandom}.
 *
 * <p>Not suitable for security tokens; intended for ULID performance mode.</p>
 */
public final class PerformanceRandomProvider implements RandomProvider {

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

    @Override
    public int nextInt(int bound) {
        return ThreadLocalRandom.current().nextInt(bound);
    }

    @Override
    public boolean secure() {
        return false;
    }
}
