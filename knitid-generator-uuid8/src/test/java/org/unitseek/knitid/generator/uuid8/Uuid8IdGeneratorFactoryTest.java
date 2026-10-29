package org.unitseek.knitid.generator.uuid8;

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

class Uuid8IdGeneratorFactoryTest {

    @Test
    void defaultsToTimeRandomPreset() {
        Uuid8Config config = Uuid8Config.from(helper(Map.of()));
        assertThat(config.preset()).isEqualTo(Uuid8Preset.TIME_RANDOM);
        IdGenerator generator = new Uuid8IdGeneratorFactory().create(helper(Map.of()));
        assertThat(generator.type()).isEqualTo(IdType.UUID8);
        assertThat(generator.generate()).hasSize(36);
    }

    @Test
    void acceptsNodePresetConfiguration() {
        IdGenerator generator = new Uuid8IdGeneratorFactory().create(helper(Map.of(
                "uuid8.preset", "TIME_NODE_RANDOM",
                "uuid8.datacenter-id", 2,
                "uuid8.worker-id", 100)));
        assertThat(generator.generate()).hasSize(36);
    }

    @Test
    void rejectsInvalidPresetAndNodeIds() {
        assertThatThrownBy(() -> Uuid8Config.from(helper(Map.of("uuid8.preset", "NOPE"))))
                .isInstanceOf(IdConfigurationException.class)
                .hasMessageContaining("KNITID-1001");
        assertThatThrownBy(() -> Uuid8Config.from(helper(Map.of("uuid8.datacenter-id", 16))))
                .isInstanceOf(IdConfigurationException.class)
                .hasMessageContaining("datacenter-id");
        assertThatThrownBy(() -> Uuid8Config.from(helper(Map.of("uuid8.worker-id", 256))))
                .isInstanceOf(IdConfigurationException.class)
                .hasMessageContaining("worker-id");
    }

    private static GeneratorConfig helper(Map<String, Object> values) {
        Map<String, Object> parameters = new LinkedHashMap<>(values);
        return new GeneratorConfig() {
            @Override
            public String name() {
                return "test";
            }

            @Override
            public IdType type() {
                return IdType.UUID8;
            }

            @Override
            public Optional<Object> get(String key) {
                return Optional.ofNullable(parameters.get(key));
            }
        };
    }
}
