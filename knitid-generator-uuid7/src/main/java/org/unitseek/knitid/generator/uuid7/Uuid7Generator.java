package org.unitseek.knitid.generator.uuid7;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import java.util.Locale;
import java.util.concurrent.locks.LockSupport;

import org.unitseek.knitid.core.api.IdContext;
import org.unitseek.knitid.core.api.IdGenerator;
import org.unitseek.knitid.core.api.IdType;
import org.unitseek.knitid.core.exception.IdConfigurationException;
import org.unitseek.knitid.core.exception.IdErrorCodes;
import org.unitseek.knitid.core.exception.IdGenerationException;
import org.unitseek.knitid.core.spi.RandomProvider;

/**
 * RFC 9562 UUIDv7 generator.
 *
 * <p>Layout: 48-bit Unix millisecond timestamp, version 7, variant {@code 10},
 * then 74 random bits. In monotonic mode the 74 random bits are treated as a
 * counter (RFC 9562 section 6.2, method 2).</p>
 *
 * <p>Instances are thread-safe. When monotonic mode is enabled and the random
 * space of the current millisecond is exhausted, the configured
 * {@link MonotonicOverflow} strategy decides whether to wait or fail.</p>
 *
 * @see <a href="https://www.rfc-editor.org/rfc/rfc9562">RFC 9562</a>
 * @since 1.0.0
 */
public final class Uuid7Generator implements IdGenerator {

    static final long MAX_48_BITS = 0x0000_FFFF_FFFF_FFFFL;

    static final long MAX_62_BITS = 0x3FFF_FFFF_FFFF_FFFFL;

    static final int MAX_12_BITS = 0x0FFF;

    private static final long MAX_OVERFLOW_WAIT_NANOS = Duration.ofSeconds(5).toNanos();

    private final Clock clock;

    private final RandomProvider random;

    private final boolean monotonic;

    private final MonotonicOverflow overflow;

    private long lastTimestamp = -1L;

    private int randomA;

    private long randomB;

    /**
     * Creates a monotonic UUIDv7 generator with {@link MonotonicOverflow#WAIT}.
     *
     * @param clock timestamp source
     * @param random random source
     */
    public Uuid7Generator(Clock clock, RandomProvider random) {
        this(clock, random, true, MonotonicOverflow.WAIT);
    }

    /**
     * Creates a UUIDv7 generator.
     *
     * @param clock timestamp source
     * @param random random source
     * @param monotonic whether to increment the 74-bit random space within the
     *        same millisecond
     * @param overflow behaviour when monotonic space is exhausted
     */
    public Uuid7Generator(Clock clock, RandomProvider random, boolean monotonic, MonotonicOverflow overflow) {
        this.clock = clock;
        this.random = random;
        this.monotonic = monotonic;
        this.overflow = overflow;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IdType type() {
        return IdType.UUID7;
    }

    /**
     * Generates a UUIDv7 string.
     *
     * @return canonical UUIDv7 string
     */
    @Override
    public String generate() {
        return generate((IdContext) null);
    }

    /**
     * Generates a UUIDv7 string. UUIDv7 has no context-dependent fields; the
     * context is ignored.
     *
     * @param context ignored
     * @return canonical UUIDv7 string
     */
    @Override
    public synchronized String generate(IdContext context) {
        return generateUuid().toString();
    }

    /**
     * Generates a UUIDv7 without formatting it as a string.
     *
     * <p>This is intended for generators such as TypeID that need the raw
     * timestamp and random bits and would otherwise pay for a UUID
     * string/parse round trip.</p>
     *
     * @return generated UUIDv7
     * @throws IdGenerationException if the clock is outside the 48-bit range
     *         or the monotonic random space is exhausted
     * @since 1.0.0
     */
    public synchronized UUID generateUuid() {
        long now = clock.millis();
        if (now < 0 || now > MAX_48_BITS) {
            throw new IdGenerationException(
                    IdErrorCodes.GENERATION_FAILED,
                    "Timestamp out of UUIDv7 48-bit range: " + now);
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
            lastTimestamp = now;
            newRandom();
        }
        return toUuid(lastTimestamp, randomA, randomB);
    }

    private void newRandom() {
        byte[] bytes = new byte[10];
        random.nextBytes(bytes);
        randomA = ((bytes[0] & 0xFF) << 4) | ((bytes[1] & 0xF0) >>> 4);
        long value = 0L;
        for (int i = 2; i < bytes.length; i++) {
            value = (value << 8) | (bytes[i] & 0xFFL);
        }
        randomB = value & MAX_62_BITS;
    }

    private boolean incrementRandom() {
        if (randomB < MAX_62_BITS) {
            randomB++;
            return true;
        }
        if (randomA < MAX_12_BITS) {
            randomA++;
            randomB = 0L;
            return true;
        }
        return false;
    }

    private void handleOverflow() {
        if (overflow == MonotonicOverflow.THROW) {
            throw new IdGenerationException(
                    IdErrorCodes.GENERATION_FAILED,
                    "UUIDv7 monotonic random space exhausted within millisecond " + lastTimestamp);
        }
        long target = lastTimestamp + 1;
        long deadline = System.nanoTime() + MAX_OVERFLOW_WAIT_NANOS;
        while (clock.millis() < target) {
            if (System.nanoTime() >= deadline) {
                throw new IdGenerationException(
                        IdErrorCodes.GENERATION_FAILED,
                        "Clock did not advance after UUIDv7 overflow within timeout: " + lastTimestamp);
            }
            LockSupport.parkNanos(100_000L);
        }
        lastTimestamp = clock.millis();
        newRandom();
    }

    static String format(long timestamp, int randA, long randB) {
        return toUuid(timestamp, randA, randB).toString();
    }

    private static UUID toUuid(long timestamp, int randA, long randB) {
        long msb = (timestamp << 16) | 0x7000L | (randA & 0x0FFFL);
        long lsb = 0x8000_0000_0000_0000L | (randB & 0x3FFF_FFFF_FFFF_FFFFL);
        return new UUID(msb, lsb);
    }

    static MonotonicOverflow parseOverflow(String value) {
        try {
            return MonotonicOverflow.valueOf(value.trim().toUpperCase(Locale.ROOT));
        }
        catch (IllegalArgumentException ex) {
            throw new IdConfigurationException(
                    IdErrorCodes.CONFIG_INVALID,
                    "Invalid uuid7.monotonic-overflow: " + value + " (expected WAIT or THROW)");
        }
    }
}
