package org.unitseek.knitid.core.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdTypeTest {

    @Test
    void normalizesCaseHyphenAndUnderscore() {
        assertThat(IdType.of("uuid-7")).isEqualTo(IdType.UUID7);
        assertThat(IdType.of(" uuid_7 ")).isEqualTo(IdType.UUID7);
        assertThat(IdType.of("KSID")).isEqualTo(IdType.KSID);
    }

    @Test
    void acceptsCustomTypes() {
        assertThat(IdType.of("custom_type").value()).isEqualTo("CUSTOMTYPE");
    }

    @Test
    void rejectsNullAndBlank() {
        assertThatThrownBy(() -> IdType.of(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> IdType.of("  ")).isInstanceOf(IllegalArgumentException.class);
    }
}
