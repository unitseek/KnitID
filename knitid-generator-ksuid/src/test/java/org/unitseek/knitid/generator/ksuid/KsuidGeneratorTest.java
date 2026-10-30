package org.unitseek.knitid.generator.ksuid;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import org.unitseek.knitid.core.exception.IdGenerationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KsuidGeneratorTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void generates27CharacterKsuidAndParsesTimestamp() {
        KsuidGenerator generator = new KsuidGenerator(
                Clock.fixed(NOW, ZoneOffset.UTC), new ZeroRandom());
        String id = generator.generate();
        assertThat(id).hasSize(27).matches("[0-9A-Za-z]{27}");
        byte[] bytes = KsuidGenerator.decode(id);
        assertThat(bytes).isNotNull();
        assertThat(KsuidGenerator.timestamp(bytes)).isEqualTo(NOW);
        KsuidIdTypeParser parser = new KsuidIdTypeParser();
        assertThat(parser.tryParse(id)).isPresent();
        assertThat(parser.tryParse(id).orElseThrow().timestamp()).isEqualTo(NOW);
    }

    @Test
    void roundTripsBoundaryAndRandomTwentyByteValues() {
        byte[] zeros = new byte[20];
        assertThat(KsuidGenerator.encode(zeros)).isEqualTo("0".repeat(27));
        assertThat(KsuidGenerator.decode("0".repeat(27))).containsExactly(zeros);
        byte[] ones = new byte[20];
        java.util.Arrays.fill(ones, (byte) 0xFF);
        assertThat(KsuidGenerator.decode(KsuidGenerator.encode(ones))).containsExactly(ones);
        java.util.Random random = new java.util.Random(42L);
        for (int i = 0; i < 1000; i++) {
            byte[] bytes = new byte[20];
            random.nextBytes(bytes);
            assertThat(KsuidGenerator.decode(KsuidGenerator.encode(bytes))).containsExactly(bytes);
        }
    }

    @Test
    void rejectsClockBeforeEpochAndInvalidInput() {
        KsuidGenerator generator = new KsuidGenerator(
                Clock.fixed(Instant.parse("2000-01-01T00:00:00Z"), ZoneOffset.UTC), new ZeroRandom());
        assertThatThrownBy(generator::generate).isInstanceOf(IdGenerationException.class);
        assertThat(KsuidGenerator.decode("short")).isNull();
        assertThat(KsuidGenerator.decode("0".repeat(27).replace('0', '!'))).isNull();
        assertThat(KsuidGenerator.decode("z".repeat(27))).isNull();
        assertThat(new KsuidIdTypeParser().tryParse("not-a-ksuid")).isEmpty();
    }

    private static final class ZeroRandom implements org.unitseek.knitid.core.spi.RandomProvider {
        @Override public void nextBytes(byte[] bytes) { java.util.Arrays.fill(bytes, (byte) 0); }
        @Override public int nextInt(int bound) { return 0; }
        @Override public boolean secure() { return false; }
    }
}
