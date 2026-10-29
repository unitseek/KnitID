package org.unitseek.knitid.generator.ulid;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UlidVectorTest {

    /** Example from the ULID specification. */
    private static final String VECTOR = "01ARZ3NDEKTSV4RRFFQ69G5FAV";

    private static final long TIMESTAMP = 1469922850259L;

    private static final int RANDOM_HI = 54902;

    private static final long RANDOM_LO = 5503943799937875291L;

    @Test
    void reproducesSpecificationExample() {
        assertThat(UlidGenerator.format(TIMESTAMP, RANDOM_HI, RANDOM_LO)).isEqualTo(VECTOR);
    }

    @Test
    void encodedValueIsCanonicalCrockfordBase32() {
        String ulid = UlidGenerator.format(TIMESTAMP, RANDOM_HI, RANDOM_LO);
        assertThat(ulid).hasSize(26);
        assertThat(ulid).matches("[0-9A-HJKMNP-TV-Z]{26}");
    }
}
