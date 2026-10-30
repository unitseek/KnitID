package org.unitseek.knitid.generator.typeid;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.unitseek.knitid.core.api.IdContext;
import org.unitseek.knitid.core.api.IdGenerator;
import org.unitseek.knitid.core.api.IdType;
import org.unitseek.knitid.core.exception.IdErrorCodes;
import org.unitseek.knitid.core.exception.IdGenerationException;
import org.unitseek.knitid.core.spi.RandomProvider;
import org.unitseek.knitid.generator.uuid7.Uuid7Generator;

/**
 * TypeID generator: a type prefix plus a UUIDv7 encoded as 26 lowercase
 * Crockford Base32 characters.
 */
public final class TypeIdGenerator implements IdGenerator {

    private static final String ALPHABET = "0123456789abcdefghjkmnpqrstvwxyz";

    private final TypeIdConfig config;

    private final Uuid7Generator uuid7;

    public TypeIdGenerator(Clock clock, RandomProvider random, TypeIdConfig config) {
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.uuid7 = new Uuid7Generator(clock, random);
    }

    @Override
    public IdType type() {
        return IdType.TYPEID;
    }

    @Override
    public String generate() {
        return generate(null);
    }

    @Override
    public String generate(IdContext context) {
        String prefix = context != null && context.prefix() != null ? context.prefix() : config.defaultPrefix();
        config.validateForGeneration(prefix);
        UUID uuid = uuid7.generateUuid();
        String suffix = encode(uuid);
        return prefix.isEmpty() ? suffix : prefix + "_" + suffix;
    }

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

    static Instant timestamp(UUID uuid) {
        return Instant.ofEpochMilli(uuid.getMostSignificantBits() >>> 16);
    }
}
