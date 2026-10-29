package org.unitseek.knitid.generator.ulid;

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

class UlidIdGeneratorFactoryTest {

    @Test
    void createsGeneratorWithConfiguredSources() {
        UlidIdGeneratorFactory factory = new UlidIdGeneratorFactory();
        IdGenerator generator = factory.create(config(Map.of(
                "ulid.monotonic", false,
                "ulid.random-source", "SECURE",
                "ulid.monotonic-overflow", "THROW")));
        assertThat(generator.type()).isEqualTo(IdType.ULID);
        assertThat(generator.generate()).hasSize(26);
    }

    @Test
    void defaultsAreApplied() {
        UlidConfig config = UlidConfig.from(config(Map.of()));
        assertThat(config.monotonic()).isTrue();
        assertThat(config.monotonicOverflow()).isEqualTo(UlidMonotonicOverflow.WAIT);
        assertThat(config.randomSource()).isEqualTo(UlidRandomSource.PERFORMANCE);
    }

    @Test
    void invalidValuesAreRejected() {
        assertThatThrownBy(() -> UlidConfig.from(config(Map.of("ulid.random-source", "nope"))))
                .isInstanceOf(IdConfigurationException.class)
                .hasMessageContaining("KNITID-1001");
        assertThatThrownBy(() -> UlidConfig.from(config(Map.of("ulid.monotonic-overflow", "nope"))))
                .isInstanceOf(IdConfigurationException.class)
                .hasMessageContaining("KNITID-1001");
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
                return IdType.ULID;
            }

            @Override
            public Optional<Object> get(String key) {
                return Optional.ofNullable(parameters.get(key));
            }
        };
    }
}
