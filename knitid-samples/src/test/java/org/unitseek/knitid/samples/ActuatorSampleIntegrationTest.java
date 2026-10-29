package org.unitseek.knitid.samples;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.unitseek.knitid.autoconfigure.endpoint.KnitIdEndpoint;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ActuatorSampleIntegrationTest {

    @Autowired
    private KnitIdEndpoint endpoint;

    @Test
    void exposesRegisteredGenerators() {
        KnitIdEndpoint.KnitIdEndpointInfo info = endpoint.info();
        assertThat(info.defaultType()).isEqualTo("UUID7");
        assertThat(info.generators()).isNotEmpty();
    }
}
