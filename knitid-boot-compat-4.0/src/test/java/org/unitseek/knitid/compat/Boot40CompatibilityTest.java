package org.unitseek.knitid.compat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;

import org.unitseek.knitid.core.api.IdGenerator;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = Boot40CompatibilityTest.TestApplication.class,
        properties = {
                "knitid.default-type=UUID7",
                "knitid.generators.uuid7.type=UUID7"
        })
class Boot40CompatibilityTest {

    @Autowired
    private IdGenerator idGenerator;

    @Test
    void generatesUuid7OnSpringBoot40() {
        assertThat(idGenerator.generate()).hasSize(36);
    }

    @SpringBootApplication
    static class TestApplication {
    }
}
