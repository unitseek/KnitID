package org.unitseek.knitid.generator.ksuid;

import java.math.BigInteger;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

import org.unitseek.knitid.core.api.IdContext;
import org.unitseek.knitid.core.api.IdGenerator;
import org.unitseek.knitid.core.api.IdType;
import org.unitseek.knitid.core.exception.IdErrorCodes;
import org.unitseek.knitid.core.exception.IdGenerationException;
import org.unitseek.knitid.core.spi.RandomProvider;

/**
 * KSUID generator: 4-byte timestamp (seconds since 2014-05-13T16:53:20Z)
 * followed by 16 random bytes, encoded as 27 Base62 characters.
 */
public final class KsuidGenerator implements IdGenerator {

    public static final Instant EPOCH = Instant.ofEpochSecond(1_400_000_000L);

    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    private static final BigInteger BASE = BigInteger.valueOf(ALPHABET.length());

    private static final long MAX_SECONDS = 0xFFFF_FFFFL;

    private final Clock clock;

    private final RandomProvider random;

    public KsuidGenerator(Clock clock, RandomProvider random) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.random = Objects.requireNonNull(random, "random must not be null");
    }

    @Override
    public IdType type() {
        return IdType.KSUID;
    }

    @Override
    public String generate() {
        long now = clock.millis();
        if (now < EPOCH.toEpochMilli()) {
            throw new IdGenerationException(
                    IdErrorCodes.GENERATION_FAILED, "Clock is before KSUID epoch: " + now);
        }
        long seconds = (now - EPOCH.toEpochMilli()) / 1000L;
        if (seconds > MAX_SECONDS) {
            throw new IdGenerationException(
                    IdErrorCodes.GENERATION_FAILED, "KSUID timestamp exhausted: " + seconds);
        }
        byte[] bytes = new byte[20];
        bytes[0] = (byte) (seconds >>> 24);
        bytes[1] = (byte) (seconds >>> 16);
        bytes[2] = (byte) (seconds >>> 8);
        bytes[3] = (byte) seconds;
        byte[] randomBytes = new byte[16];
        random.nextBytes(randomBytes);
        System.arraycopy(randomBytes, 0, bytes, 4, 16);
        return encode(bytes);
    }

    @Override
    public String generate(IdContext context) {
        return generate();
    }

    static String encode(byte[] bytes) {
        BigInteger value = new BigInteger(1, bytes);
        char[] encoded = new char[27];
        for (int i = encoded.length - 1; i >= 0; i--) {
            BigInteger[] divRem = value.divideAndRemainder(BASE);
            encoded[i] = ALPHABET.charAt(divRem[1].intValue());
            value = divRem[0];
        }
        return new String(encoded);
    }

    static byte[] decode(String raw) {
        if (raw == null || raw.length() != 27) {
            return null;
        }
        BigInteger value = BigInteger.ZERO;
        for (int i = 0; i < raw.length(); i++) {
            int digit = ALPHABET.indexOf(raw.charAt(i));
            if (digit < 0) {
                return null;
            }
            value = value.multiply(BASE).add(BigInteger.valueOf(digit));
        }
        if (value.bitLength() > 160) {
            return null;
        }
        byte[] bytes = value.toByteArray();
        byte[] out = new byte[20];
        int copy = Math.min(bytes.length, out.length);
        System.arraycopy(bytes, bytes.length - copy, out, out.length - copy, copy);
        return out;
    }

    static Instant timestamp(byte[] bytes) {
        long seconds = ((long) (bytes[0] & 0xFF) << 24)
                | ((long) (bytes[1] & 0xFF) << 16)
                | ((long) (bytes[2] & 0xFF) << 8)
                | (bytes[3] & 0xFFL);
        return Instant.ofEpochSecond(EPOCH.getEpochSecond() + seconds);
    }
}
