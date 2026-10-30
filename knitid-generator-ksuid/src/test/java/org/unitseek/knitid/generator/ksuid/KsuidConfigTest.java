package org.unitseek.knitid.generator.ksuid;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.unitseek.knitid.core.api.IdContext;
import org.unitseek.knitid.core.api.IdGenerator;
import org.unitseek.knitid.core.api.IdType;
import org.unitseek.knitid.core.spi.GeneratorConfig;

import static org.assertj.core.api.Assertions.assertThat;

class KsuidConfigTest {

    @Test
    void parsesRandomSource() {
        assertThat(KsuidConfig.from(config(Map.of("ksuid.random-source", "PERFORMANCE"))).randomSource())
                .isEqualTo(KsuidRandomSource.PERFORMANCE);
        assertThat(KsuidConfig.defaults().randomSource()).isEqualTo(KsuidRandomSource.SECURE);
    }

    @Test
    void factoryCreatesKsuidGenerator() {
        IdGenerator generator = new KsuidIdGeneratorFactory().create(
                config(Map.of("ksuid.random-source", "SECURE")));
        assertThat(generator.type()).isEqualTo(IdType.KSUID);
        assertThat(generator.generate()).hasSize(27);
    }

    @Test
    void rejectsUnknownNestedKeys() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> KsuidConfig.from(config(Map.of("ksuid.random-sourcee", "SECURE"))))
                .isInstanceOf(org.unitseek.knitid.core.exception.IdConfigurationException.class);
    }

    @Test
    void recognizesOfficialKsuidShape() {
        assertThat(KsuidGenerator.decode("0ujtsYcgvSTl8PAuAdqWYSMnLOv")).isNotNull();
    }

    private static GeneratorConfig config(Map<String, Object> values) {
        return new GeneratorConfig() {
            @Override public String name() { return "ksuid"; }
            @Override public IdType type() { return IdType.KSUID; }
            @Override public Optional<Object> get(String key) { return Optional.ofNullable(values.get(key)); }
            @Override public java.util.Set<String> keys() { return values.keySet(); }
        };
    }
}
