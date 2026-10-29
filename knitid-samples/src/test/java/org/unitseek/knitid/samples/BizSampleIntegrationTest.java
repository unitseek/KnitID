package org.unitseek.knitid.samples;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.unitseek.knitid.core.api.IdGenerator;
import org.unitseek.knitid.core.api.IdType;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "knitid.generators.order-id.type=BIZ",
        "knitid.generators.order-id.biz.template={prefix}-{yyyyMMdd}-{seq:4}",
        "knitid.generators.order-id.biz.prefix=ORD",
        "knitid.generators.order-id.biz.zone-id=Asia/Shanghai",
        "knitid.generators.order-id.biz.sequence-provider=memory",
        "knitid.generators.order-id.biz.reset=DAILY",
        "knitid.generators.order-id.biz.check-algorithm=NONE",
        "knitid.default-generator=order-id"
})
class BizSampleIntegrationTest {

    @Autowired
    private IdGenerator idGenerator;

    @Test
    void startsAndGeneratesBizId() {
        assertThat(idGenerator.type()).isEqualTo(IdType.BIZ);
        assertThat(idGenerator.generate()).matches("ORD-\\d{8}-\\d{4}");
    }
}
