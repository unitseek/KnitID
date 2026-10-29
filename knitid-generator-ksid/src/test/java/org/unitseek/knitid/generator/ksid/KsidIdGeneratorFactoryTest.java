package org.unitseek.knitid.generator.ksid;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.unitseek.knitid.core.api.IdGenerator;
import org.unitseek.knitid.core.api.IdType;
import org.unitseek.knitid.core.exception.IdConfigurationException;
import org.unitseek.knitid.core.spi.GeneratorConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KsidIdGeneratorFactoryTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void createsFixedWorkerIdGenerator() {
        KsidIdGeneratorFactory factory = new KsidIdGeneratorFactory(FIXED_CLOCK);
        IdGenerator generator = factory.create(config(Map.of("ksid.worker-id", 1, "ksid.datacenter-id", 2)));
        assertThat(generator.type()).isEqualTo(IdType.KSID);
        assertThat(Long.parseLong(generator.generate())).isPositive();
    }

    @Test
    void missingFixedWorkerIdFails() {
        KsidIdGeneratorFactory factory = new KsidIdGeneratorFactory(FIXED_CLOCK);
        assertThatThrownBy(() -> factory.create(config(Map.of())))
                .isInstanceOf(IdConfigurationException.class)
                .hasMessageContaining("ksid.worker-id");
    }

    @Test
    void unsupportedAssignerFailsWithActionableMessage() {
        KsidIdGeneratorFactory factory = new KsidIdGeneratorFactory(FIXED_CLOCK);
        assertThatThrownBy(() -> factory.create(config(Map.of("ksid.worker-id-assigner", "redis"))))
                .isInstanceOf(IdConfigurationException.class)
                .hasMessageContaining("fixed, environment");
    }

    @Test
    void invalidBitLayoutFails() {
        KsidIdGeneratorFactory factory = new KsidIdGeneratorFactory(FIXED_CLOCK);
        assertThatThrownBy(() -> factory.create(config(Map.of(
                "ksid.worker-id", 1, "ksid.timestamp-bits", 40))))
                .isInstanceOf(IdConfigurationException.class)
                .hasMessageContaining("sum to 63");
    }

    @Test
    void environmentAssignerReadsSystemProperties() {
        System.setProperty("KNITID_WORKER_ID", "3");
        System.setProperty("KNITID_DATACENTER_ID", "1");
        try {
            KsidIdGeneratorFactory factory = new KsidIdGeneratorFactory(FIXED_CLOCK);
            IdGenerator generator = factory.create(config(Map.of("ksid.worker-id-assigner", "environment")));
            assertThat(generator.type()).isEqualTo(IdType.KSID);
            long id = Long.parseLong(generator.generate());
            assertThat((id >>> 12) & 0x1F).isEqualTo(3L);
            assertThat((id >>> 17) & 0x1F).isEqualTo(1L);
        }
        finally {
            System.clearProperty("KNITID_WORKER_ID");
            System.clearProperty("KNITID_DATACENTER_ID");
        }
    }

    private static GeneratorConfig config(Map<String, Object> values) {
        Map<String, Object> parameters = new LinkedHashMap<>(values);
        return new GeneratorConfig() {
            @Override
            public String name() {
                return "test";
            }

            @Override
            public IdType type() {
                return IdType.KSID;
            }

            @Override
            public Optional<Object> get(String key) {
                return Optional.ofNullable(parameters.get(key));
            }
        };
    }
}
