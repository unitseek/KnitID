package org.unitseek.knitid.generator.ksid;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import org.unitseek.knitid.core.exception.IdConfigurationException;
import org.unitseek.knitid.core.exception.IdErrorCodes;
import org.unitseek.knitid.core.spi.GeneratorConfig;

/**
 * Immutable KSID configuration.
 *
 * <p>Layout: 1 sign bit (always 0) + timestamp bits + datacenter bits +
 * worker bits + sequence bits = 64 bits, therefore the four configurable
 * fields must sum to 63.</p>
 *
 * @param epoch timestamp epoch; the default is 2024-01-01T00:00:00Z
 * @param timestampBits timestamp width; must fit within 42 bits
 * @param datacenterIdBits datacenter id width
 * @param workerIdBits worker id width
 * @param sequenceBits per-millisecond sequence width
 * @param clockBackwardsStrategy behaviour when the clock moves backwards
 * @param maxBackwardsWait upper bound for wait-based clock catch-up
 * @param sequenceOverflowStrategy behaviour when the sequence is exhausted
 * @param allowBorrowFuture explicit opt-in for BORROW_FUTURE
 * @param leaseRenewInterval WorkerId lease renewal interval
 * @param leaseMaxRenewFailures consecutive renewal failures before the lease
 *        is marked invalid
 * @since 1.0.0
 */
public record KsidConfig(
        Instant epoch,
        int timestampBits,
        int datacenterIdBits,
        int workerIdBits,
        int sequenceBits,
        ClockBackwardsStrategy clockBackwardsStrategy,
        Duration maxBackwardsWait,
        SequenceOverflowStrategy sequenceOverflowStrategy,
        boolean allowBorrowFuture,
        Duration leaseRenewInterval,
        int leaseMaxRenewFailures) {

    /** Default KSID epoch: 2024-01-01T00:00:00Z. */
    public static final Instant DEFAULT_EPOCH = Instant.parse("2024-01-01T00:00:00Z");

    /** Default timestamp width: 41 bits. */
    public static final int DEFAULT_TIMESTAMP_BITS = 41;

    /** Default datacenter id width: 5 bits. */
    public static final int DEFAULT_DATACENTER_ID_BITS = 5;

    /** Default worker id width: 5 bits. */
    public static final int DEFAULT_WORKER_ID_BITS = 5;

    /** Default sequence width: 12 bits. */
    public static final int DEFAULT_SEQUENCE_BITS = 12;

    /** Default maximum clock-backwards wait: 5 seconds. */
    public static final Duration DEFAULT_MAX_BACKWARDS_WAIT = Duration.ofSeconds(5);

    /** Default WorkerId lease renewal interval: 20 seconds. */
    public static final Duration DEFAULT_LEASE_RENEW_INTERVAL = Duration.ofSeconds(20);

    /** Default consecutive lease-renewal failure threshold: 3. */
    public static final int DEFAULT_LEASE_MAX_RENEW_FAILURES = 3;

    private static final int TOTAL_BITS = 63;

    /**
     * Validates the bit layout and policy invariants.
     *
     * @throws IdConfigurationException if any component is invalid
     */
    public KsidConfig {
        if (epoch == null) {
            throw invalid("ksid.epoch must not be null");
        }
        if (timestampBits < 1 || timestampBits > 42) {
            throw invalid("ksid.timestamp-bits must be between 1 and 42: " + timestampBits);
        }
        if (datacenterIdBits < 0 || workerIdBits < 0) {
            throw invalid("ksid.datacenter-id-bits and ksid.worker-id-bits must be >= 0");
        }
        if (sequenceBits < 1 || sequenceBits > 62) {
            throw invalid("ksid.sequence-bits must be between 1 and 62: " + sequenceBits);
        }
        long total = (long) timestampBits + datacenterIdBits + workerIdBits + sequenceBits;
        if (total != TOTAL_BITS) {
            throw invalid("KSID bits must sum to " + TOTAL_BITS + " but were " + total
                    + " (timestamp=" + timestampBits + ", datacenter=" + datacenterIdBits
                    + ", worker=" + workerIdBits + ", sequence=" + sequenceBits + ")");
        }
        if (clockBackwardsStrategy == null) {
            throw invalid("ksid.clock-backwards.strategy must not be null");
        }
        if (maxBackwardsWait == null || maxBackwardsWait.isNegative() || maxBackwardsWait.isZero()) {
            throw invalid("ksid.clock-backwards.max-wait must be positive");
        }
        if (sequenceOverflowStrategy == null) {
            throw invalid("ksid.sequence-overflow must not be null");
        }
        if (clockBackwardsStrategy == ClockBackwardsStrategy.BORROW_FUTURE && !allowBorrowFuture) {
            throw invalid("ksid.clock-backwards.strategy=BORROW_FUTURE requires ksid.allow-borrow-future=true");
        }
        if (leaseRenewInterval == null || leaseRenewInterval.isNegative() || leaseRenewInterval.isZero()) {
            throw invalid("ksid.lease-renew-interval must be positive");
        }
        if (leaseMaxRenewFailures < 1) {
            throw invalid("ksid.lease-max-renew-failures must be >= 1");
        }
    }

    /**
     * Returns the default KSID configuration.
     *
     * <p>41 timestamp bits, 5 datacenter bits, 5 worker bits, 12 sequence
     * bits, WAIT strategies and a 20-second lease renewal interval.</p>
     *
     * @return default KSID configuration
     */
    public static KsidConfig defaults() {
        return new KsidConfig(DEFAULT_EPOCH, DEFAULT_TIMESTAMP_BITS, DEFAULT_DATACENTER_ID_BITS,
                DEFAULT_WORKER_ID_BITS, DEFAULT_SEQUENCE_BITS, ClockBackwardsStrategy.WAIT,
                DEFAULT_MAX_BACKWARDS_WAIT, SequenceOverflowStrategy.WAIT, false,
                DEFAULT_LEASE_RENEW_INTERVAL, DEFAULT_LEASE_MAX_RENEW_FAILURES);
    }

    /**
     * Parses KSID configuration from a generator configuration.
     *
     * @param config generator configuration
     * @return parsed configuration
     * @throws IdConfigurationException if a value is malformed or violates the
     *         63-bit layout constraints
     */
    public static KsidConfig from(GeneratorConfig config) {
        Instant epoch;
        try {
            epoch = Instant.parse(config.getString("ksid.epoch", DEFAULT_EPOCH.toString()));
        }
        catch (DateTimeParseException ex) {
            throw invalid("Invalid ksid.epoch: " + config.getString("ksid.epoch", ""));
        }
        ClockBackwardsStrategy backwards = parseEnum(ClockBackwardsStrategy.class,
                config.getString("ksid.clock-backwards.strategy", ClockBackwardsStrategy.WAIT.name()),
                "ksid.clock-backwards.strategy");
        SequenceOverflowStrategy overflow = parseEnum(SequenceOverflowStrategy.class,
                config.getString("ksid.sequence-overflow", SequenceOverflowStrategy.WAIT.name()),
                "ksid.sequence-overflow");
        Duration maxWait;
        Duration renewInterval;
        try {
            maxWait = config.getDuration("ksid.clock-backwards.max-wait", DEFAULT_MAX_BACKWARDS_WAIT);
            renewInterval = config.getDuration("ksid.lease-renew-interval", DEFAULT_LEASE_RENEW_INTERVAL);
        }
        catch (IllegalArgumentException ex) {
            throw invalid("Invalid KSID duration: " + ex.getMessage());
        }
        return new KsidConfig(epoch,
                config.getInt("ksid.timestamp-bits", DEFAULT_TIMESTAMP_BITS),
                config.getInt("ksid.datacenter-id-bits", DEFAULT_DATACENTER_ID_BITS),
                config.getInt("ksid.worker-id-bits", DEFAULT_WORKER_ID_BITS),
                config.getInt("ksid.sequence-bits", DEFAULT_SEQUENCE_BITS),
                backwards,
                maxWait,
                overflow,
                config.getBoolean("ksid.allow-borrow-future", false),
                renewInterval,
                config.getInt("ksid.lease-max-renew-failures", DEFAULT_LEASE_MAX_RENEW_FAILURES));
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String value, String key) {
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        }
        catch (IllegalArgumentException ex) {
            throw invalid("Invalid " + key + ": " + value);
        }
    }

    private static IdConfigurationException invalid(String message) {
        return new IdConfigurationException(IdErrorCodes.CONFIG_INVALID, message);
    }
}
