package org.unitseek.knitid.starter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StarterBoundaryTest {

    @Test
    void plainStarterDoesNotContainGeneratorModules() {
        assertThatThrownBy(() -> Class.forName("org.unitseek.knitid.generator.uuid7.Uuid7Generator"))
                .isInstanceOf(ClassNotFoundException.class);
        assertThatThrownBy(() -> Class.forName("org.unitseek.knitid.generator.ksuid.KsuidGenerator"))
                .isInstanceOf(ClassNotFoundException.class);
        assertThatThrownBy(() -> Class.forName("org.unitseek.knitid.generator.typeid.TypeIdGenerator"))
                .isInstanceOf(ClassNotFoundException.class);
    }
}
