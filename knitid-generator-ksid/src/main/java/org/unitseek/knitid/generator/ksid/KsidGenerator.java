package org.unitseek.knitid.generator.ksid;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

import org.unitseek.knitid.core.api.HealthState;
import org.unitseek.knitid.core.api.IdContext;
import org.unitseek.knitid.core.api.IdGeneratorHealth;
import org.unitseek.knitid.core.api.IdGeneratorHealthSnapshot;
import org.unitseek.knitid.core.api.IdType;
import org.unitseek.knitid.core.api.NumericIdGenerator;
import org.unitseek.knitid.core.exception.ClockBackwardsException;
import org.unitseek.knitid.core.exception.IdConfigurationException;
import org.unitseek.knitid.core.exception.IdErrorCodes;
import org.unitseek.knitid.core.exception.IdGenerationException;
import org.unitseek.knitid.core.spi.WorkerId;
import org.unitseek.knitid.core.spi.WorkerIdAssigner;

/**
 * KSID (KnitID Snowflake) generator.
 *
 * <p>Layout: 1 sign bit + timestamp + datacenter + worker + sequence, with the
 * four configurable parts summing to 63 bits. Generation is thread-safe.</p>
 */
public final class KsidGenerator implements NumericIdGenerator, IdGeneratorHealth, AutoCloseable {

    private static final System.Logger LOGGER = System.getLogger(KsidGenerator.class.getName());

    private static final long LEASE_PAUSE_NANOS = 100_000L;

    private final Clock clock;

    private final WorkerId workerId;

    private final KsidConfig config;

    private final long epochMillis;

    private final long maxTimestamp;

    private final long maxSequence;

    private final long maxDatacenterId;

    private final long maxWorkerId;

    private final int workerShift;

    private final int datacenterShift;

    private final int timestampShift;

    private final WorkerIdAssigner assigner;

    private final ScheduledExecutorService leaseScheduler;

    private final AtomicInteger renewFailures = new AtomicInteger();

    private final AtomicBoolean closed = new AtomicBoolean();

    private final Deque<Long> backwardsEvents = new ArrayDeque<>();

    private volatile boolean leaseValid = true;

    private long lastTimestamp = -1L;

    private long sequence;

    public KsidGenerator(Clock clock, WorkerId workerId, KsidConfig config) {
        this(clock, workerId, config, null);
    }

    public KsidGenerator(Clock clock, WorkerId workerId, KsidConfig config, WorkerIdAssigner assigner) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.workerId = Objects.requireNonNull(workerId, "workerId must not be null");
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.assigner = assigner;
        this.epochMillis = config.epoch().toEpochMilli();
        if (clock.millis() < epochMillis) {
            throw new IdConfigurationException(
                    IdErrorCodes.CONFIG_INVALID,
                    "ksid.epoch is in the future: " + config.epoch());
        }
        this.maxTimestamp = (1L << config.timestampBits()) - 1L;
        this.maxSequence = (1L << config.sequenceBits()) - 1L;
        this.maxDatacenterId = (1L << config.datacenterIdBits()) - 1L;
        this.maxWorkerId = (1L << config.workerIdBits()) - 1L;
        if (workerId.datacenterId() > maxDatacenterId) {
            throw new IdConfigurationException(
                    IdErrorCodes.CONFIG_INVALID,
                    "datacenterId " + workerId.datacenterId() + " does not fit in "
                            + config.datacenterIdBits() + " bits");
        }
        if (workerId.workerId() > maxWorkerId) {
            throw new IdConfigurationException(
                    IdErrorCodes.CONFIG_INVALID,
                    "workerId " + workerId.workerId() + " does not fit in " + config.workerIdBits() + " bits");
        }
        this.workerShift = config.sequenceBits();
        this.datacenterShift = config.workerIdBits() + config.sequenceBits();
        this.timestampShift = config.datacenterIdBits() + config.workerIdBits() + config.sequenceBits();
        this.leaseScheduler = createLeaseScheduler(assigner);
    }

    private ScheduledExecutorService createLeaseScheduler(WorkerIdAssigner candidate) {
        if (candidate == null || !candidate.renewable()) {
            return null;
        }
        Duration suggestedInterval = candidate.renewalInterval();
        Duration effectiveInterval = suggestedInterval != null ? suggestedInterval : config.leaseRenewInterval();
        long interval = Math.max(1L, effectiveInterval.toMillis());
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "knitid-ksid-lease-" + workerId.workerId());
            thread.setDaemon(true);
            return thread;
        });
        scheduler.scheduleAtFixedRate(this::renewLease, interval, interval, TimeUnit.MILLISECONDS);
        return scheduler;
    }

    private void renewLease() {
        if (closed.get()) {
            return;
        }
        try {
            assigner.renew();
            renewFailures.set(0);
        }
        catch (Throwable ex) {
            int failures = renewFailures.incrementAndGet();
            if (failures >= config.leaseMaxRenewFailures()) {
                leaseValid = false;
                LOGGER.log(System.Logger.Level.WARNING,
                        "KSID WorkerId lease lost after " + failures + " renew failures (worker=" + workerId + ")",
                        ex);
            }
        }
    }

    @Override
    public IdType type() {
        return IdType.KSID;
    }

    @Override
    public boolean blocking() {
        return config.clockBackwardsStrategy() == ClockBackwardsStrategy.WAIT;
    }

    @Override
    public String generate() {
        return Long.toString(generateLong());
    }

    @Override
    public String generate(IdContext context) {
        return Long.toString(generateLong(context));
    }

    @Override
    public long generateLong() {
        return generateLong(null);
    }

    @Override
    public long generateLong(IdContext context) {
        synchronized (this) {
            if (closed.get()) {
                throw new IdGenerationException(
                        IdErrorCodes.GENERATION_FAILED, "KSID generator is closed");
            }
            if (!leaseValid) {
                throw new IdGenerationException(
                        IdErrorCodes.WORKER_LEASE_LOST,
                        "KSID WorkerId lease is no longer valid (worker=" + workerId + ")");
            }
            while (true) {
                long now = clock.millis();
                long timestamp = now - epochMillis;
                if (timestamp < 0) {
                    throw new IdGenerationException(
                            IdErrorCodes.GENERATION_FAILED,
                            "System clock is before ksid.epoch: now=" + now + ", epoch=" + epochMillis);
                }
                if (timestamp > maxTimestamp) {
                    throw new IdGenerationException(
                            IdErrorCodes.GENERATION_FAILED,
                            "KSID timestamp exhausted: timestamp=" + timestamp + ", max=" + maxTimestamp);
                }
                if (timestamp > lastTimestamp) {
                    lastTimestamp = timestamp;
                    sequence = 0L;
                    return buildId(timestamp, 0L);
                }
                if (timestamp < lastTimestamp) {
                    recordBackwards(now);
                    switch (config.clockBackwardsStrategy()) {
                        case THROW:
                            throw new ClockBackwardsException(
                                    IdErrorCodes.CLOCK_BACKWARDS,
                                    "KSID clock moved backwards: now=" + timestamp + ", last=" + lastTimestamp);
                        case WAIT:
                            awaitClockCatchUp();
                            continue;
                        case BORROW_FUTURE:
                            timestamp = lastTimestamp;
                            break;
                        default:
                            throw new IllegalStateException("Unhandled strategy: "
                                    + config.clockBackwardsStrategy());
                    }
                }
                if (sequence < maxSequence) {
                    sequence++;
                    return buildId(timestamp, sequence);
                }
                if (config.clockBackwardsStrategy() == ClockBackwardsStrategy.BORROW_FUTURE) {
                    if (lastTimestamp >= maxTimestamp) {
                        throw new IdGenerationException(
                                IdErrorCodes.GENERATION_FAILED,
                                "KSID borrowed logical timestamp exhausted: " + lastTimestamp);
                    }
                    lastTimestamp++;
                    sequence = 0L;
                    return buildId(lastTimestamp, 0L);
                }
                switch (config.sequenceOverflowStrategy()) {
                    case THROW:
                        throw new IdGenerationException(
                                IdErrorCodes.SEQUENCE_EXHAUSTED,
                                "KSID sequence exhausted within millisecond " + timestamp
                                        + " (sequence-bits=" + config.sequenceBits() + ")");
                    case WAIT:
                        awaitNextMillisecond();
                        continue;
                    default:
                        throw new IllegalStateException("Unhandled strategy: "
                                + config.sequenceOverflowStrategy());
                }
            }
        }
    }

    private void awaitClockCatchUp() {
        long targetMillis = epochMillis + lastTimestamp;
        long deadline = System.nanoTime() + config.maxBackwardsWait().toNanos();
        while (clock.millis() < targetMillis) {
            if (System.nanoTime() >= deadline) {
                throw new ClockBackwardsException(
                        IdErrorCodes.CLOCK_BACKWARDS,
                        "KSID clock did not catch up within " + config.maxBackwardsWait());
            }
            LockSupport.parkNanos(LEASE_PAUSE_NANOS);
        }
    }

    private void awaitNextMillisecond() {
        long targetMillis = epochMillis + lastTimestamp + 1L;
        long deadline = System.nanoTime() + config.maxBackwardsWait().toNanos();
        while (clock.millis() < targetMillis) {
            if (System.nanoTime() >= deadline) {
                throw new IdGenerationException(
                        IdErrorCodes.SEQUENCE_EXHAUSTED,
                        "KSID sequence exhausted and next millisecond did not arrive within "
                                + config.maxBackwardsWait());
            }
            LockSupport.parkNanos(LEASE_PAUSE_NANOS);
        }
    }

    private void recordBackwards(long nowMillis) {
        synchronized (backwardsEvents) {
            backwardsEvents.addLast(nowMillis);
        }
    }

    private int countBackwards(Duration window) {
        long cutoff = clock.millis() - window.toMillis();
        synchronized (backwardsEvents) {
            while (!backwardsEvents.isEmpty() && backwardsEvents.peekFirst() < cutoff) {
                backwardsEvents.removeFirst();
            }
            return backwardsEvents.size();
        }
    }

    @Override
    public IdGeneratorHealthSnapshot health(Duration clockBackwardsWindow) {
        Duration window = clockBackwardsWindow == null ? Duration.ofMinutes(1) : clockBackwardsWindow;
        int backwards = countBackwards(window);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("type", "KSID");
        details.put("datacenterId", workerId.datacenterId());
        details.put("workerId", workerId.workerId());
        details.put("leaseValid", leaseValid);
        details.put("clockBackwardsCount", backwards);
        details.put("clockBackwardsStrategy", config.clockBackwardsStrategy().name());
        details.put("epoch", config.epoch().toString());
        details.put("closed", closed.get());
        HealthState state;
        String message;
        if (closed.get()) {
            state = HealthState.UNKNOWN;
            message = "KSID generator is closed";
        }
        else if (!leaseValid) {
            state = HealthState.DOWN;
            message = "KSID WorkerId lease lost";
        }
        else {
            state = HealthState.UP;
            message = "KSID ready";
        }
        return new IdGeneratorHealthSnapshot(state, message, details);
    }

    private long buildId(long timestamp, long seq) {
        return (timestamp << timestampShift)
                | ((long) workerId.datacenterId() << datacenterShift)
                | ((long) workerId.workerId() << workerShift)
                | seq;
    }

    boolean isLeaseValid() {
        return leaseValid;
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        if (leaseScheduler != null) {
            leaseScheduler.shutdownNow();
        }
        if (assigner != null) {
            try {
                assigner.release(workerId);
            }
            catch (RuntimeException ex) {
                LOGGER.log(System.Logger.Level.WARNING,
                        "Failed to release KSID WorkerId " + workerId, ex);
            }
            if (assigner instanceof AutoCloseable closeable) {
                try {
                    closeable.close();
                }
                catch (Exception ex) {
                    LOGGER.log(System.Logger.Level.WARNING,
                            "Failed to close KSID WorkerId assigner " + assigner, ex);
                }
            }
        }
    }
}
