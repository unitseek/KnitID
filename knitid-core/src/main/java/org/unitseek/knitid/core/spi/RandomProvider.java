package org.unitseek.knitid.core.spi;

/**
 * Random source abstraction.
 */
public interface RandomProvider {

    void nextBytes(byte[] bytes);

    int nextInt(int bound);

    boolean secure();
}
