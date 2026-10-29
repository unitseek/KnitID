package org.unitseek.knitid.samples;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.unitseek.knitid.core.api.IdGenerator;
import org.unitseek.knitid.core.api.IdType;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "knitid.generators.order-id.type=KSID",
        "knitid.generators.order-id.ksid.worker-id=1",
        "knitid.generators.order-id.ksid.datacenter-id=0",
        "knitid.default-generator=order-id"
})
class KsidSampleIntegrationTest {

    @Autowired
    private IdGenerator idGenerator;

    @Test
    void startsAndGeneratesKsid() {
        assertThat(idGenerator.type()).isEqualTo(IdType.KSID);
        assertThat(Long.parseLong(idGenerator.generate())).isPositive();
    }
}
