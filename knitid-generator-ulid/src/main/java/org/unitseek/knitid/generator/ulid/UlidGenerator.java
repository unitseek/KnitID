package org.unitseek.knitid.generator.ulid;

import java.time.Clock;
import java.util.Objects;
import java.util.concurrent.locks.LockSupport;

import org.unitseek.knitid.core.api.IdContext;
import org.unitseek.knitid.core.api.IdGenerator;
import org.unitseek.knitid.core.api.IdType;
import org.unitseek.knitid.core.exception.IdErrorCodes;
import org.unitseek.knitid.core.exception.IdGenerationException;
import org.unitseek.knitid.core.spi.RandomProvider;

/**
 * ULID generator.
 *
 * <p>26-character Crockford Base32: 48-bit millisecond timestamp followed by
 * 80 random bits. In monotonic mode the 80-bit random value is incremented for
 * every ID within the same millisecond.</p>
 */
public final class UlidGenerator implements IdGenerator {

    private static final String CROCKFORD = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";

    private static final long MAX_48_BITS = 0xFFFF_FFFF_FFFFL;

    private static final long MAX_64_BITS = -1L;

    private static final long MAX_OVERFLOW_WAIT_NANOS = java.time.Duration.ofSeconds(5).toNanos();

    private final Clock clock;

    private final RandomProvider random;

    private final boolean monotonic;

    private final UlidMonotonicOverflow overflow;

    private long lastTimestamp = -1L;

    private int randomHi;

    private long randomLo;

    public UlidGenerator(Clock clock, RandomProvider random) {
        this(clock, random, true, UlidMonotonicOverflow.WAIT);
    }

    public UlidGenerator(Clock clock, RandomProvider random, boolean monotonic, UlidMonotonicOverflow overflow) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.random = Objects.requireNonNull(random, "random must not be null");
        this.monotonic = monotonic;
        this.overflow = overflow;
    }

    @Override
    public IdType type() {
        return IdType.ULID;
    }

    @Override
    public String generate() {
        return generate((IdContext) null);
    }

    @Override
    public synchronized String generate(IdContext context) {
        long now = clock.millis();
        if (now < 0 || now > MAX_48_BITS) {
            throw new IdGenerationException(
                    IdErrorCodes.GENERATION_FAILED, "Timestamp out of ULID 48-bit range: " + now);
        }
        if (now > lastTimestamp) {
            lastTimestamp = now;
            newRandom();
        }
        else if (monotonic) {
            if (!incrementRandom()) {
                handleOverflow();
            }
        }
        else {
            newRandom();
        }
        return format(lastTimestamp, randomHi, randomLo);
    }

    private void newRandom() {
        byte[] bytes = new byte[10];
        random.nextBytes(bytes);
        randomHi = ((bytes[0] & 0xFF) << 8) | (bytes[1] & 0xFF);
        long low = 0L;
        for (int i = 2; i < bytes.length; i++) {
            low = (low << 8) | (bytes[i] & 0xFFL);
        }
        randomLo = low;
    }

    private boolean incrementRandom() {
        if (randomLo != MAX_64_BITS) {
            randomLo++;
            return true;
        }
        if (randomHi != 0xFFFF) {
            randomHi++;
            randomLo = 0L;
            return true;
        }
        return false;
    }

    private void handleOverflow() {
        if (overflow == UlidMonotonicOverflow.THROW) {
            throw new IdGenerationException(
                    IdErrorCodes.GENERATION_FAILED,
                    "ULID monotonic random space exhausted within millisecond " + lastTimestamp);
        }
        long target = lastTimestamp + 1;
        long deadline = System.nanoTime() + MAX_OVERFLOW_WAIT_NANOS;
        while (clock.millis() < target) {
            if (System.nanoTime() >= deadline) {
                throw new IdGenerationException(
                        IdErrorCodes.GENERATION_FAILED,
                        "Clock did not advance after ULID overflow within timeout: " + lastTimestamp);
            }
            LockSupport.parkNanos(100_000L);
        }
        lastTimestamp = clock.millis();
        newRandom();
    }

    static String format(long timestamp, int randomHi, long randomLo) {
        byte[] bytes = new byte[16];
        bytes[0] = (byte) (timestamp >>> 40);
        bytes[1] = (byte) (timestamp >>> 32);
        bytes[2] = (byte) (timestamp >>> 24);
        bytes[3] = (byte) (timestamp >>> 16);
        bytes[4] = (byte) (timestamp >>> 8);
        bytes[5] = (byte) timestamp;
        bytes[6] = (byte) (randomHi >>> 8);
        bytes[7] = (byte) randomHi;
        for (int i = 0; i < 8; i++) {
            bytes[8 + i] = (byte) (randomLo >>> (56 - (8 * i)));
        }
        char[] encoded = new char[26];
        int position = 0;
        for (int i = 0; i < encoded.length; i++) {
            int value = 0;
            for (int bit = 0; bit < 5; bit++) {
                value = (value << 1) | bitAt(bytes, position++);
            }
            encoded[i] = CROCKFORD.charAt(value);
        }
        return new String(encoded);
    }

    private static int bitAt(byte[] bytes, int position) {
        if (position < 2) {
            return 0;
        }
        int dataBit = position - 2;
        return (bytes[dataBit >> 3] >>> (7 - (dataBit & 7))) & 1;
    }
}
