package org.unitseek.knitid.generator.typeid;

import java.math.BigInteger;
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

    private static final BigInteger BASE = BigInteger.valueOf(ALPHABET.length());

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
        UUID uuid = UUID.fromString(uuid7.generate());
        String suffix = encode(uuid);
        return prefix.isEmpty() ? suffix : prefix + "_" + suffix;
    }

    static String encode(UUID uuid) {
        byte[] bytes = new byte[16];
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();
        for (int i = 0; i < 8; i++) {
            bytes[i] = (byte) (msb >>> (56 - 8 * i));
            bytes[8 + i] = (byte) (lsb >>> (56 - 8 * i));
        }
        BigInteger value = new BigInteger(1, bytes).shiftLeft(2);
        char[] encoded = new char[26];
        for (int i = 25; i >= 0; i--) {
            BigInteger[] divRem = value.divideAndRemainder(BASE);
            encoded[i] = ALPHABET.charAt(divRem[1].intValue());
            value = divRem[0];
        }
        return new String(encoded);
    }

    static UUID decodeSuffix(String suffix) {
        if (suffix == null || suffix.length() != 26 || suffix.charAt(0) > '7') {
            return null;
        }
        BigInteger value = BigInteger.ZERO;
        for (int i = 0; i < suffix.length(); i++) {
            int digit = ALPHABET.indexOf(suffix.charAt(i));
            if (digit < 0) {
                return null;
            }
            value = value.multiply(BASE).add(BigInteger.valueOf(digit));
        }
        BigInteger uuidValue = value.shiftRight(2);
        if (uuidValue.bitLength() > 128) {
            return null;
        }
        byte[] bytes = new byte[16];
        byte[] source = uuidValue.toByteArray();
        int copy = Math.min(source.length, bytes.length);
        System.arraycopy(source, source.length - copy, bytes, bytes.length - copy, copy);
        long msb = 0;
        long lsb = 0;
        for (int i = 0; i < 8; i++) {
            msb = (msb << 8) | (bytes[i] & 0xFFL);
            lsb = (lsb << 8) | (bytes[8 + i] & 0xFFL);
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
