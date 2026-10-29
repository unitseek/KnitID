package org.unitseek.knitid.autoconfigure.endpoint;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.unitseek.knitid.core.api.IdContext;
import org.unitseek.knitid.core.api.IdGenerator;
import org.unitseek.knitid.core.api.IdGeneratorRegistry;
import org.unitseek.knitid.core.api.IdType;

import static org.assertj.core.api.Assertions.assertThat;

class KnitIdEndpointTest {

    @Test
    void listsGeneratorsAndMarksDefault() {
        Map<String, IdGenerator> generators = new LinkedHashMap<>();
        generators.put("order-id", new FakeGenerator(IdType.KSID, true));
        generators.put("event-id", new FakeGenerator(IdType.ULID, false));
        KnitIdEndpoint endpoint = new KnitIdEndpoint(new FakeRegistry(generators, "order-id"));

        KnitIdEndpoint.KnitIdEndpointInfo info = endpoint.info();

        assertThat(info.defaultGenerator()).isEqualTo("order-id");
        assertThat(info.defaultType()).isEqualTo("KSID");
        assertThat(info.generators()).hasSize(2);
        assertThat(info.generators().get(0).name()).isEqualTo("order-id");
        assertThat(info.generators().get(0).defaultGenerator()).isTrue();
        assertThat(info.generators().get(1).defaultGenerator()).isFalse();
    }

    private static final class FakeGenerator implements IdGenerator {

        private final IdType type;

        private final boolean blocking;

        private FakeGenerator(IdType type, boolean blocking) {
            this.type = type;
            this.blocking = blocking;
        }

        @Override
        public IdType type() {
            return type;
        }

        @Override
        public boolean blocking() {
            return blocking;
        }

        @Override
        public String generate() {
            return "x";
        }

        @Override
        public String generate(IdContext context) {
            return generate();
        }
    }

    private static final class FakeRegistry implements IdGeneratorRegistry {

        private final Map<String, IdGenerator> generators;

        private final String defaultName;

        private FakeRegistry(Map<String, IdGenerator> generators, String defaultName) {
            this.generators = generators;
            this.defaultName = defaultName;
        }

        @Override
        public IdGenerator getDefault() {
            return generators.get(defaultName);
        }

        @Override
        public IdGenerator get(String name) {
            return generators.get(name);
        }

        @Override
        public Optional<IdGenerator> find(String name) {
            return Optional.ofNullable(generators.get(name));
        }

        @Override
        public Optional<IdGenerator> find(IdType type) {
            return generators.values().stream().filter(g -> g.type().equals(type)).findFirst();
        }

        @Override
        public Set<String> names() {
            return generators.keySet();
        }

        @Override
        public Map<String, IdGenerator> asMap() {
            return generators;
        }
    }
}
