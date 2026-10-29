package org.unitseek.knitid.core.util;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DurationParserTest {

    @Test
    void parsesSpringAndIsoDurations() {
        assertThat(DurationParser.parse("5s")).isEqualTo(Duration.ofSeconds(5));
        assertThat(DurationParser.parse("PT1H")).isEqualTo(Duration.ofHours(1));
        assertThat(DurationParser.parse("-PT1H")).isEqualTo(Duration.ofHours(-1));
    }

    @Test
    void rejectsMalformedAndOutOfRangeValues() {
        assertThatThrownBy(() -> DurationParser.parse("1.2.3s"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid duration");
        assertThatThrownBy(() -> DurationParser.parse("1000000d"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("out of range");
    }
}
