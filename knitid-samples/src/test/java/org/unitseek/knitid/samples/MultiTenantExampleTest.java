package org.unitseek.knitid.samples;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.unitseek.knitid.core.api.IdContext;
import org.unitseek.knitid.core.api.IdGeneratorRegistry;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = SampleApplication.class)
class MultiTenantExampleTest {

    @Autowired
    private IdGeneratorRegistry registry;

    @Test
    void overridesBizPrefixForTenantOrBusinessType() {
        String id = registry.get("biz").generate(
                IdContext.builder().tenantId("tenant-a").prefix("TENANT_A").build());
        assertThat(id).startsWith("TENANT_A-");
    }
}
