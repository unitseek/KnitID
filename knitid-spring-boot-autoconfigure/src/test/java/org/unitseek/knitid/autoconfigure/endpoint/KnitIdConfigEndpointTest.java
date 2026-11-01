package org.unitseek.knitid.autoconfigure.endpoint;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.unitseek.knitid.autoconfigure.KnitIdProperties;
import org.unitseek.knitid.core.api.IdContext;
import org.unitseek.knitid.core.api.IdGenerator;
import org.unitseek.knitid.core.api.IdGeneratorRegistry;
import org.unitseek.knitid.core.api.IdType;

import static org.assertj.core.api.Assertions.assertThat;

class KnitIdConfigEndpointTest {

    @Test
    void exposesSanitizedConfigurationOnly() {
        KnitIdProperties properties = new KnitIdProperties();
        properties.setDefaultType("UUID7");
        properties.setGenerators(new LinkedHashMap<>(Map.of("event-id", Map.of(
                "type", "UUID7",
                "enabled", "true",
                "secret", "do-not-leak"))));
        KnitIdConfigEndpoint endpoint = new KnitIdConfigEndpoint(properties, registry());

        KnitIdConfigEndpoint.KnitIdConfigInfo info = endpoint.diagnostics();

        assertThat(info.defaultType()).isEqualTo("UUID7");
        assertThat(info.configuredGenerators()).hasSize(1);
        assertThat(info.configuredGenerators().get(0).name()).isEqualTo("event-id");
        assertThat(info.configuredGenerators().get(0).type()).isEqualTo("UUID7");
        assertThat(info.configuredGenerators().get(0).enabled()).isTrue();
        assertThat(info.toString()).doesNotContain("do-not-leak");
    }

    private static IdGeneratorRegistry registry() {
        Map<String, IdGenerator> generators = new LinkedHashMap<>();
        generators.put("event-id", new FakeGenerator());
        return new IdGeneratorRegistry() {
            @Override public IdGenerator getDefault() { return generators.get("event-id"); }
            @Override public IdGenerator get(String name) { return generators.get(name); }
            @Override public Optional<IdGenerator> find(String name) { return Optional.ofNullable(generators.get(name)); }
            @Override public Optional<IdGenerator> find(IdType type) { return generators.values().stream().filter(g -> g.type().equals(type)).findFirst(); }
            @Override public Set<String> names() { return generators.keySet(); }
            @Override public Map<String, IdGenerator> asMap() { return generators; }
        };
    }

    private static final class FakeGenerator implements IdGenerator {
        @Override public IdType type() { return IdType.UUID7; }
        @Override public String generate() { return "x"; }
        @Override public String generate(IdContext context) { return "x"; }
    }
}
