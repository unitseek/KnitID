package org.unitseek.knitid.generator.typeid;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.unitseek.knitid.core.api.IdContext;
import org.unitseek.knitid.core.api.IdGenerator;
import org.unitseek.knitid.core.api.IdType;
import org.unitseek.knitid.core.exception.IdConfigurationException;
import org.unitseek.knitid.core.exception.IdErrorCodes;
import org.unitseek.knitid.core.exception.IdGenerationException;
import org.unitseek.knitid.core.spi.RandomProvider;
import org.unitseek.knitid.generator.uuid7.Uuid7Generator;

/**
 * TypeID generator.
 *
 * <p>A TypeID is a lower-case type prefix, an underscore, and a UUIDv7 encoded
 * as 26 lower-case Crockford Base32 characters. The suffix layout is 2 leading
 * zero bits followed by the 128 UUID bits, split into 26 groups of 5 bits.</p>
 *
 * <p>The generator reuses KnitID's UUIDv7 generator; it does not define a new
 * time or random layout. When no prefix is supplied in the {@link IdContext},
 * the configured default prefix is used.</p>
 *
 * @see <a href="https://github.com/jetify-com/typeid/tree/main/spec">TypeID Specification v0.3.0</a>
 * @since 1.0.0
 */
public final class TypeIdGenerator implements IdGenerator {

    private static final String ALPHABET = "0123456789abcdefghjkmnpqrstvwxyz";

    private final TypeIdConfig config;

    private final Uuid7Generator uuid7;

    /**
     * Creates a TypeID generator.
     *
     * @param clock clock used by the underlying UUIDv7 generator
     * @param random random source used by the underlying UUIDv7 generator
     * @param config TypeID prefix configuration
     */
    public TypeIdGenerator(Clock clock, RandomProvider random, TypeIdConfig config) {
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.uuid7 = new Uuid7Generator(clock, random);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IdType type() {
        return IdType.TYPEID;
    }

    /**
     * Generates a TypeID using the configured default prefix.
     *
     * @return TypeID string
     * @throws IdConfigurationException if the configured prefix is invalid
     */
    @Override
    public String generate() {
        return generate(null);
    }

    /**
     * Generates a TypeID using the context prefix, or the configured default
     * prefix when the context has no prefix.
     *
     * @param context optional generation context
     * @return TypeID string
     * @throws IdConfigurationException if the selected prefix is not registered
     */
    @Override
    public String generate(IdContext context) {
        String prefix = context != null && context.prefix() != null ? context.prefix() : config.defaultPrefix();
        config.validateForGeneration(prefix);
        UUID uuid = uuid7.generateUuid();
        String suffix = encode(uuid);
        return prefix.isEmpty() ? suffix : prefix + "_" + suffix;
    }

    // Encodes the 130-bit layout: two zero bits followed by the 128 UUID bits.
    static String encode(UUID uuid) {
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();
        char[] encoded = new char[26];
        for (int group = 0; group < 26; group++) {
            int value = 0;
            for (int bit = 0; bit < 5; bit++) {
                int bitIndex = group * 5 + bit;
                int current = 0;
                if (bitIndex >= 2) {
                    int uuidBit = bitIndex - 2;
                    current = uuidBit < 64
                            ? (int) ((msb >>> (63 - uuidBit)) & 1L)
                            : (int) ((lsb >>> (63 - (uuidBit - 64))) & 1L);
                }
                value = (value << 1) | current;
            }
            encoded[group] = ALPHABET.charAt(value);
        }
        return new String(encoded);
    }

    // Decodes the 26-character suffix and validates UUID version/variant.
    static UUID decodeSuffix(String suffix) {
        if (suffix == null || suffix.length() != 26 || suffix.charAt(0) > '7') {
            return null;
        }
        long msb = 0;
        long lsb = 0;
        for (int group = 0; group < 26; group++) {
            int digit = ALPHABET.indexOf(suffix.charAt(group));
            if (digit < 0) {
                return null;
            }
            for (int bit = 4; bit >= 0; bit--) {
                int bitIndex = group * 5 + (4 - bit);
                if (bitIndex < 2) {
                    continue;
                }
                int uuidBit = bitIndex - 2;
                long current = (digit >>> bit) & 1;
                if (uuidBit < 64) {
                    msb = (msb << 1) | current;
                }
                else {
                    lsb = (lsb << 1) | current;
                }
            }
        }
        UUID uuid = new UUID(msb, lsb);
        if (uuid.version() != 7 || uuid.variant() != 2) {
            return null;
        }
        return uuid;
    }

    // Extracts the UUIDv7 millisecond timestamp from the most significant bits.
    static Instant timestamp(UUID uuid) {
        return Instant.ofEpochMilli(uuid.getMostSignificantBits() >>> 16);
    }
}
