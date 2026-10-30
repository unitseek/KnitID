package org.unitseek.knitid.samples;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.unitseek.knitid.core.api.IdGenerator;
import org.unitseek.knitid.core.api.IdGeneratorRegistry;
import org.unitseek.knitid.core.api.IdType;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SampleApplicationTest {

    @Autowired
    private IdGenerator idGenerator;

    @Autowired
    private IdGeneratorRegistry registry;

    @Test
    void startsAndGeneratesUuid7() {
        assertThat(idGenerator.type()).isEqualTo(IdType.UUID7);
        assertThat(idGenerator.generate()).hasSize(36);
        assertThat(registry.names()).contains("uuid7", "uuid8", "ulid", "ksid", "biz", "ksuid", "typeid");
    }
}
