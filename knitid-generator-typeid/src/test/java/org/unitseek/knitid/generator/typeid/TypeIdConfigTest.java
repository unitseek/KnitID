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
    void preservesNumericIndexOrderBeyondSingleDigits() {
        java.util.Map<String, Object> indexed = new java.util.LinkedHashMap<>();
        for (int i = 0; i < 12; i++) {
            indexed.put(String.valueOf(i), "p" + (char) ('a' + i));
        }
        java.util.Map<String, Object> values = java.util.Map.of(
                "typeid.prefixes", indexed,
                "typeid.default-prefix", "pl");
        TypeIdConfig config = TypeIdConfig.from(config(values));
        assertThat(config.prefixes()).containsExactly(
                "pa", "pb", "pc", "pd", "pe", "pf", "pg", "ph", "pi", "pj", "pk", "pl");
    }

    @Test
    void rejectsUnknownNestedKeys() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> TypeIdConfig.from(config(Map.of(
                        "typeid.prefixes", java.util.List.of("user"),
                        "typeid.default-prefix", "user",
                        "typeid.default-prefx", "order"))))
                .isInstanceOf(org.unitseek.knitid.core.exception.IdConfigurationException.class);
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
            @Override public java.util.Set<String> keys() { return values.keySet(); }
        };
    }
}
