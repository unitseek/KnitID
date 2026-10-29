package org.unitseek.knitid.generator.uuid8;

import java.time.Clock;
import java.util.HexFormat;
import java.util.Objects;
import java.util.concurrent.locks.LockSupport;

import org.unitseek.knitid.core.api.IdContext;
import org.unitseek.knitid.core.api.IdGenerator;
import org.unitseek.knitid.core.api.IdType;
import org.unitseek.knitid.core.exception.IdErrorCodes;
import org.unitseek.knitid.core.exception.IdGenerationException;
import org.unitseek.knitid.core.spi.RandomProvider;

/**
 * RFC 9562 UUIDv8 generator with fixed preset layouts.
 *
 * <p>UUIDv8 has application-defined semantics; KnitID does not claim
 * cross-system interoperability for this type.</p>
 */
public final class Uuid8Generator implements IdGenerator {

    private static final long MAX_48_BITS = 0x0000_FFFF_FFFF_FFFFL;

    private static final long MAX_62_BITS = 0x3FFF_FFFF_FFFF_FFFFL;

    private static final int MAX_COUNTER = 0x0FFF;

    private static final long MAX_OVERFLOW_WAIT_NANOS = java.time.Duration.ofSeconds(5).toNanos();

    private final Clock clock;

    private final Uuid8Config config;

    private final RandomProvider random;

    private long lastTimestamp = -1L;

    private int counter;

    private long fixedRandom;

    public Uuid8Generator(Clock clock, Uuid8Config config, RandomProvider random) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.random = Objects.requireNonNull(random, "random must not be null");
    }

    @Override
    public IdType type() {
        return IdType.UUID8;
    }

    @Override
    public String generate() {
        return generate((IdContext) null);
    }

    @Override
    public synchronized String generate(IdContext context) {
        return switch (config.preset()) {
            case TIME_RANDOM -> generateTimeRandom();
            case TIME_NODE_RANDOM -> generateTimeNodeRandom();
            case RANDOM -> generateRandom();
        };
    }

    private String generateTimeRandom() {
        long now = clock.millis();
        if (now > lastTimestamp) {
            lastTimestamp = now;
            counter = 0;
            fixedRandom = random62();
        }
        if (counter > MAX_COUNTER) {
            waitNextMillisecond();
            lastTimestamp = clock.millis();
            counter = 0;
            fixedRandom = random62();
        }
        return format(lastTimestamp, counter++, fixedRandom);
    }

    private String generateTimeNodeRandom() {
        long now = clock.millis();
        if (now > lastTimestamp) {
            lastTimestamp = now;
        }
        int node = ((config.datacenterId() & 0x0F) << 8) | (config.workerId() & 0xFF);
        return format(lastTimestamp, node, random62());
    }

    private String generateRandom() {
        return format(random48(), random.nextInt(4096), random62());
    }

    private void waitNextMillisecond() {
        long target = lastTimestamp + 1;
        long deadline = System.nanoTime() + MAX_OVERFLOW_WAIT_NANOS;
        while (clock.millis() < target) {
            if (System.nanoTime() >= deadline) {
                throw new IdGenerationException(
                        IdErrorCodes.GENERATION_FAILED,
                        "Clock did not advance after UUIDv8 counter overflow within timeout: " + lastTimestamp);
            }
            LockSupport.parkNanos(100_000L);
        }
    }

    private long random48() {
        byte[] bytes = new byte[6];
        random.nextBytes(bytes);
        long value = 0L;
        for (byte b : bytes) {
            value = (value << 8) | (b & 0xFFL);
        }
        return value & MAX_48_BITS;
    }

    private long random62() {
        byte[] bytes = new byte[8];
        random.nextBytes(bytes);
        long value = 0L;
        for (byte b : bytes) {
            value = (value << 8) | (b & 0xFFL);
        }
        return value & MAX_62_BITS;
    }

    static String format(long customA, int customB, long customC) {
        if (customA < 0 || customA > MAX_48_BITS) {
            throw new IdGenerationException(
                    IdErrorCodes.GENERATION_FAILED, "UUIDv8 custom_a out of 48-bit range: " + customA);
        }
        if (customB < 0 || customB > MAX_COUNTER) {
            throw new IdGenerationException(
                    IdErrorCodes.GENERATION_FAILED, "UUIDv8 custom_b out of 12-bit range: " + customB);
        }
        if (customC < 0 || customC > MAX_62_BITS) {
            throw new IdGenerationException(
                    IdErrorCodes.GENERATION_FAILED, "UUIDv8 custom_c out of 62-bit range: " + customC);
        }
        byte[] bytes = new byte[16];
        bytes[0] = (byte) (customA >>> 40);
        bytes[1] = (byte) (customA >>> 32);
        bytes[2] = (byte) (customA >>> 24);
        bytes[3] = (byte) (customA >>> 16);
        bytes[4] = (byte) (customA >>> 8);
        bytes[5] = (byte) customA;
        bytes[6] = (byte) (0x80 | ((customB >>> 8) & 0x0F));
        bytes[7] = (byte) customB;
        bytes[8] = (byte) (0x80 | ((customC >>> 56) & 0x3F));
        for (int i = 9; i < 16; i++) {
            bytes[i] = (byte) (customC >>> (8 * (15 - i)));
        }
        String hex = HexFormat.of().formatHex(bytes);
        return hex.substring(0, 8) + "-" + hex.substring(8, 12) + "-" + hex.substring(12, 16)
                + "-" + hex.substring(16, 20) + "-" + hex.substring(20);
    }
}
