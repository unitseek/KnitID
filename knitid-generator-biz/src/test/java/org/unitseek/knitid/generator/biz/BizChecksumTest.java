package org.unitseek.knitid.generator.biz;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BizChecksumTest {

    @Test
    void mod37_36MatchesIso7064Vector() {
        assertThat(CheckAlgorithm.MOD37_36.checkDigit("A12425GABC1234002")).isEqualTo('M');
    }

    @Test
    void mod11_2MatchesIso7064Vector() {
        assertThat(CheckAlgorithm.MOD11_2.checkDigit("0794")).isEqualTo('0');
        assertThat(CheckAlgorithm.MOD11_2.checkDigit("079")).isEqualTo('X');
    }

    @Test
    void luhnMatchesKnownVector() {
        assertThat(CheckAlgorithm.LUHN.checkDigit("7992739871")).isEqualTo('3');
    }
}
