package org.unitseek.knitid.generator.uuid7;

import java.time.Clock;
import java.util.HexFormat;
import java.util.Locale;

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
 */
public final class Uuid7Generator implements IdGenerator {

    static final long MAX_48_BITS = 0x0000_FFFF_FFFF_FFFFL;

    static final long MAX_62_BITS = 0x3FFF_FFFF_FFFF_FFFFL;

    static final int MAX_12_BITS = 0x0FFF;

    private final Clock clock;

    private final RandomProvider random;

    private final boolean monotonic;

    private final MonotonicOverflow overflow;

    private long lastTimestamp = -1L;

    private int randomA;

    private long randomB;

    public Uuid7Generator(Clock clock, RandomProvider random) {
        this(clock, random, true, MonotonicOverflow.WAIT);
    }

    public Uuid7Generator(Clock clock, RandomProvider random, boolean monotonic, MonotonicOverflow overflow) {
        this.clock = clock;
        this.random = random;
        this.monotonic = monotonic;
        this.overflow = overflow;
    }

    @Override
    public IdType type() {
        return IdType.UUID7;
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
        return format(lastTimestamp, randomA, randomB);
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
        long deadline = lastTimestamp + 1;
        while (clock.millis() < deadline) {
            Thread.onSpinWait();
        }
        lastTimestamp = clock.millis();
        newRandom();
    }

    static String format(long timestamp, int randA, long randB) {
        byte[] bytes = new byte[16];
        bytes[0] = (byte) (timestamp >>> 40);
        bytes[1] = (byte) (timestamp >>> 32);
        bytes[2] = (byte) (timestamp >>> 24);
        bytes[3] = (byte) (timestamp >>> 16);
        bytes[4] = (byte) (timestamp >>> 8);
        bytes[5] = (byte) timestamp;
        bytes[6] = (byte) (0x70 | ((randA >>> 8) & 0x0F));
        bytes[7] = (byte) randA;
        bytes[8] = (byte) (0x80 | ((randB >>> 56) & 0x3F));
        for (int i = 9; i < 16; i++) {
            bytes[i] = (byte) (randB >>> (8 * (15 - i)));
        }
        String hex = HexFormat.of().formatHex(bytes);
        return hex.substring(0, 8) + "-" + hex.substring(8, 12) + "-" + hex.substring(12, 16)
                + "-" + hex.substring(16, 20) + "-" + hex.substring(20);
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
