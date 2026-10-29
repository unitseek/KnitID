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

    public static final Instant DEFAULT_EPOCH = Instant.parse("2024-01-01T00:00:00Z");

    public static final int DEFAULT_TIMESTAMP_BITS = 41;

    public static final int DEFAULT_DATACENTER_ID_BITS = 5;

    public static final int DEFAULT_WORKER_ID_BITS = 5;

    public static final int DEFAULT_SEQUENCE_BITS = 12;

    public static final Duration DEFAULT_MAX_BACKWARDS_WAIT = Duration.ofSeconds(5);

    public static final Duration DEFAULT_LEASE_RENEW_INTERVAL = Duration.ofSeconds(20);

    public static final int DEFAULT_LEASE_MAX_RENEW_FAILURES = 3;

    private static final int TOTAL_BITS = 63;

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
        int total = timestampBits + datacenterIdBits + workerIdBits + sequenceBits;
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

    public static KsidConfig defaults() {
        return new KsidConfig(DEFAULT_EPOCH, DEFAULT_TIMESTAMP_BITS, DEFAULT_DATACENTER_ID_BITS,
                DEFAULT_WORKER_ID_BITS, DEFAULT_SEQUENCE_BITS, ClockBackwardsStrategy.WAIT,
                DEFAULT_MAX_BACKWARDS_WAIT, SequenceOverflowStrategy.WAIT, false,
                DEFAULT_LEASE_RENEW_INTERVAL, DEFAULT_LEASE_MAX_RENEW_FAILURES);
    }

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
