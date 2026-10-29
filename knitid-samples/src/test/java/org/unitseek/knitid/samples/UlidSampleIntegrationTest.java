package org.unitseek.knitid.samples;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.unitseek.knitid.core.api.IdGenerator;
import org.unitseek.knitid.core.api.IdType;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "knitid.default-type=ULID")
class UlidSampleIntegrationTest {

    @Autowired
    private IdGenerator idGenerator;

    @Test
    void startsAndGeneratesUlid() {
        assertThat(idGenerator.type()).isEqualTo(IdType.ULID);
        assertThat(idGenerator.generate()).hasSize(26);
    }
}
