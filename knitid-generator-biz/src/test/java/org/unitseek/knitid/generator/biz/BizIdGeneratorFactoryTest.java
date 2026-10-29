package org.unitseek.knitid.generator.biz;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.unitseek.knitid.core.api.IdGenerator;
import org.unitseek.knitid.core.api.IdType;
import org.unitseek.knitid.core.exception.IdConfigurationException;
import org.unitseek.knitid.core.spi.GeneratorConfig;
import org.unitseek.knitid.core.spi.SequenceProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BizIdGeneratorFactoryTest {

    @Test
    void createsGeneratorWithDefaultMemorySequence() {
        BizIdGeneratorFactory factory = new BizIdGeneratorFactory(
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC),
                name -> new MemorySequenceProvider());
        IdGenerator generator = factory.create(config(Map.of(
                "biz.template", "{seq:3}",
                "biz.zone-id", "UTC",
                "biz.check-algorithm", "NONE")));
        assertThat(generator.type()).isEqualTo(IdType.BIZ);
        assertThat(generator.generate()).isEqualTo("000");
    }

    @Test
    void supportsCustomSequenceProviderByName() {
        SequenceProvider custom = scope -> 42L;
        BizIdGeneratorFactory factory = new BizIdGeneratorFactory(
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC),
                name -> custom);
        IdGenerator generator = factory.create(config(Map.of(
                "biz.template", "{seq:3}",
                "biz.zone-id", "UTC",
                "biz.sequence-provider", "custom",
                "biz.check-algorithm", "NONE")));
        assertThat(generator.generate()).isEqualTo("042");
    }

    @Test
    void unknownSequenceProviderFails() {
        BizIdGeneratorFactory factory = new BizIdGeneratorFactory();
        assertThatThrownBy(() -> factory.create(config(Map.of(
                "biz.template", "{seq:3}",
                "biz.zone-id", "UTC",
                "biz.sequence-provider", "missing",
                "biz.check-algorithm", "NONE"))))
                .isInstanceOf(IdConfigurationException.class)
                .hasMessageContaining("missing");
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
                return IdType.BIZ;
            }

            @Override
            public Optional<Object> get(String key) {
                return Optional.ofNullable(parameters.get(key));
            }
        };
    }
}
