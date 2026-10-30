package org.unitseek.knitid.generator.typeid;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.unitseek.knitid.core.api.IdType;
import org.unitseek.knitid.core.spi.GeneratorConfig;

import static org.assertj.core.api.Assertions.assertThat;

class TypeIdConfigTest {

    @Test
    void parsesSpringIndexedPrefixMapAndDefault() {
        Map<String, Object> values = Map.of(
                "typeid.prefixes", Map.of("0", "user", "1", "order"),
                "typeid.default-prefix", "order",
                "typeid.allow-empty-prefix", false);
        TypeIdConfig config = TypeIdConfig.from(config(values));
        assertThat(config.prefixes()).containsExactly("user", "order");
        assertThat(config.defaultPrefix()).isEqualTo("order");
    }

    @Test
    void parsesArrayShape() {
        Map<String, Object> values = Map.of("typeid.prefixes", new String[] {"user", "order"});
        TypeIdConfig config = TypeIdConfig.from(config(values));
        assertThat(config.prefixes()).containsExactly("user", "order");
    }

    private static GeneratorConfig config(Map<String, Object> values) {
        return new GeneratorConfig() {
            @Override public String name() { return "typeid"; }
            @Override public IdType type() { return IdType.TYPEID; }
            @Override public Optional<Object> get(String key) { return Optional.ofNullable(values.get(key)); }
        };
    }
}
