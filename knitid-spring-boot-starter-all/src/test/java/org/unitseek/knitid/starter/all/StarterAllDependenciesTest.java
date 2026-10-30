package org.unitseek.knitid.starter.all;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StarterAllDependenciesTest {

    @Test
    void starterAllContainsAllSevenGenerators() throws ClassNotFoundException {
        assertThat(Class.forName("org.unitseek.knitid.generator.uuid7.Uuid7Generator")).isNotNull();
        assertThat(Class.forName("org.unitseek.knitid.generator.uuid8.Uuid8Generator")).isNotNull();
        assertThat(Class.forName("org.unitseek.knitid.generator.ulid.UlidGenerator")).isNotNull();
        assertThat(Class.forName("org.unitseek.knitid.generator.ksid.KsidGenerator")).isNotNull();
        assertThat(Class.forName("org.unitseek.knitid.generator.biz.BizIdGenerator")).isNotNull();
        assertThat(Class.forName("org.unitseek.knitid.generator.ksuid.KsuidGenerator")).isNotNull();
        assertThat(Class.forName("org.unitseek.knitid.generator.typeid.TypeIdGenerator")).isNotNull();
    }
}
