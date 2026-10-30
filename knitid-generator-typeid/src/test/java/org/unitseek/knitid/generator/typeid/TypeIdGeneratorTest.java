package org.unitseek.knitid.generator.typeid;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.unitseek.knitid.core.api.IdContext;
import org.unitseek.knitid.core.api.IdType;
import org.unitseek.knitid.core.exception.IdConfigurationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TypeIdGeneratorTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final TypeIdConfig config = new TypeIdConfig(
            java.util.Set.of("user", "order"), "user", false);

    @Test
    void generatesPrefixedTypeIdAndParsesIt() {
        TypeIdGenerator generator = new TypeIdGenerator(
                Clock.fixed(NOW, ZoneOffset.UTC), new ZeroRandom(), config);
        String id = generator.generate();
        assertThat(id).startsWith("user_").hasSize(31);
        String suffix = id.substring(5);
        assertThat(suffix).hasSize(26).matches("[0-9a-z]{26}");
        assertThat(suffix.charAt(0)).isLessThanOrEqualTo('7');
        TypeIdIdTypeParser parser = new TypeIdIdTypeParser();
        assertThat(parser.tryParse(id)).isPresent();
        assertThat(parser.tryParse(id).orElseThrow().businessType()).isEqualTo("user");
        assertThat(parser.tryParse(id).orElseThrow().timestamp()).isEqualTo(NOW);
    }

    @Test
    void usesContextPrefixAndRejectsUnregisteredPrefix() {
        TypeIdGenerator generator = new TypeIdGenerator(
                Clock.fixed(NOW, ZoneOffset.UTC), new ZeroRandom(), config);
        assertThat(generator.generate(IdContext.builder().prefix("order").build())).startsWith("order_");
        assertThatThrownBy(() -> generator.generate(IdContext.builder().prefix("goods").build()))
                .isInstanceOf(IdConfigurationException.class);
    }

    @Test
    void validatesOfficialPrefixRules() {
        assertThat(TypeIdConfig.isValidPrefix("user")).isTrue();
        assertThat(TypeIdConfig.isValidPrefix("user_profile")).isTrue();
        assertThat(TypeIdConfig.isValidPrefix("a__b")).isTrue();
        assertThat(TypeIdConfig.isValidPrefix("")).isTrue();
        assertThat(TypeIdConfig.isValidPrefix("User")).isFalse();
        assertThat(TypeIdConfig.isValidPrefix("user1")).isFalse();
        assertThat(TypeIdConfig.isValidPrefix("_user")).isFalse();
    }

    @Test
    void rejectsSuffixOnlyTypeIdInBuiltInParser() {
        TypeIdIdTypeParser parser = new TypeIdIdTypeParser();
        assertThat(parser.tryParse("2x4y6z8a0b1c2d3e4f5g6h7j8k")).isEmpty();
    }

    private static final class ZeroRandom implements org.unitseek.knitid.core.spi.RandomProvider {
        @Override public void nextBytes(byte[] bytes) { java.util.Arrays.fill(bytes, (byte) 0); }
        @Override public int nextInt(int bound) { return 0; }
        @Override public boolean secure() { return false; }
    }
}
