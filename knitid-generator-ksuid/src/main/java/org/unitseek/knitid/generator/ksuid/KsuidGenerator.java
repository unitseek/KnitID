package org.unitseek.knitid.generator.ksuid;

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
        int[] limbs = new int[5];
        for (int i = 0; i < 5; i++) {
            int offset = i * 4;
            limbs[i] = ((bytes[offset] & 0xFF) << 24)
                    | ((bytes[offset + 1] & 0xFF) << 16)
                    | ((bytes[offset + 2] & 0xFF) << 8)
                    | (bytes[offset + 3] & 0xFF);
        }
        char[] encoded = new char[27];
        for (int position = encoded.length - 1; position >= 0; position--) {
            long remainder = 0;
            for (int i = 0; i < limbs.length; i++) {
                long current = (remainder << 32) | (limbs[i] & 0xFFFF_FFFFL);
                limbs[i] = (int) (current / 62L);
                remainder = current % 62L;
            }
            encoded[position] = ALPHABET.charAt((int) remainder);
        }
        return new String(encoded);
    }

    static byte[] decode(String raw) {
        if (raw == null || raw.length() != 27) {
            return null;
        }
        int[] limbs = new int[5];
        for (int i = 0; i < raw.length(); i++) {
            int digit = ALPHABET.indexOf(raw.charAt(i));
            if (digit < 0) {
                return null;
            }
            long carry = digit;
            for (int limb = limbs.length - 1; limb >= 0; limb--) {
                long current = (limbs[limb] & 0xFFFF_FFFFL) * 62L + carry;
                limbs[limb] = (int) current;
                carry = current >>> 32;
            }
            if (carry != 0) {
                return null;
            }
        }
        byte[] out = new byte[20];
        for (int i = 0; i < 5; i++) {
            int offset = i * 4;
            out[offset] = (byte) (limbs[i] >>> 24);
            out[offset + 1] = (byte) (limbs[i] >>> 16);
            out[offset + 2] = (byte) (limbs[i] >>> 8);
            out[offset + 3] = (byte) limbs[i];
        }
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
