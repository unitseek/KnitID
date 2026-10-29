package org.unitseek.knitid.generator.uuid7;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RFC 9562 Appendix A.6 test vector.
 */
class Uuid7Rfc9562VectorTest {

    private static final String RFC_VECTOR = "017f22e2-79b0-7cc3-98c4-dc0c0c07398f";

    private static final long RFC_TIMESTAMP = 1645557742000L;

    private static final int RFC_RAND_A = 0x0CC3;

    private static final long RFC_RAND_B = 0x18C4DC0C0C07398FL;

    @Test
    void reproducesAppendixA6Example() {
        assertThat(Uuid7Generator.format(RFC_TIMESTAMP, RFC_RAND_A, RFC_RAND_B))
                .isEqualTo(RFC_VECTOR);
    }

    @Test
    void appendixA6ExampleHasExpectedVersionVariantAndTimestamp() {
        UUID uuid = UUID.fromString(RFC_VECTOR);
        assertThat(uuid.version()).isEqualTo(7);
        assertThat(uuid.variant()).isEqualTo(2);

        String compact = RFC_VECTOR.replace("-", "");
        long timestamp = Long.parseUnsignedLong(compact.substring(0, 12), 16);
        assertThat(timestamp).isEqualTo(RFC_TIMESTAMP);
    }
}
