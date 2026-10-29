package org.unitseek.knitid.samples;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.unitseek.knitid.core.api.IdGenerator;
import org.unitseek.knitid.core.api.IdType;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "knitid.generators.custom.type=UUID8",
        "knitid.generators.custom.uuid8.preset=TIME_NODE_RANDOM",
        "knitid.generators.custom.uuid8.datacenter-id=1",
        "knitid.generators.custom.uuid8.worker-id=2",
        "knitid.default-generator=custom"
})
class Uuid8SampleIntegrationTest {

    @Autowired
    private IdGenerator idGenerator;

    @Test
    void startsAndGeneratesUuid8() {
        assertThat(idGenerator.type()).isEqualTo(IdType.UUID8);
        assertThat(UUID.fromString(idGenerator.generate()).version()).isEqualTo(8);
    }
}
