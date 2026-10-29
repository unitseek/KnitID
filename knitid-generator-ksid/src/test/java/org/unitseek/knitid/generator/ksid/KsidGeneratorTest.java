package org.unitseek.knitid.generator.ksid;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import org.unitseek.knitid.core.exception.ClockBackwardsException;
import org.unitseek.knitid.core.exception.IdConfigurationException;
import org.unitseek.knitid.core.exception.IdGenerationException;
import org.unitseek.knitid.core.spi.WorkerId;
import org.unitseek.knitid.core.spi.WorkerIdAssigner;
import org.unitseek.knitid.test.MutableClock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KsidGeneratorTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private static final Instant EPOCH = Instant.parse("2024-01-01T00:00:00Z");

    private static final long EPOCH_MILLIS = EPOCH.toEpochMilli();

    @Test
    void generatedIdsDecodeToConfiguredFields() {
        MutableClock clock = new MutableClock(NOW);
        KsidConfig config = KsidConfig.defaults();
        KsidGenerator generator = new KsidGenerator(clock, new WorkerId(1, 2), config);
        long previous = -1;
        for (int i = 0; i < 100; i++) {
            long id = generator.generateLong();
            assertThat(id).isPositive().isGreaterThan(previous);
            previous = id;
            long timestamp = id >>> (config.datacenterIdBits() + config.workerIdBits() + config.sequenceBits());
            long datacenter = (id >>> (config.workerIdBits() + config.sequenceBits()))
                    & ((1L << config.datacenterIdBits()) - 1);
            long worker = (id >>> config.sequenceBits()) & ((1L << config.workerIdBits()) - 1);
            long sequence = id & ((1L << config.sequenceBits()) - 1);
            assertThat(timestamp).isEqualTo(NOW.toEpochMilli() - EPOCH_MILLIS);
            assertThat(datacenter).isEqualTo(1);
            assertThat(worker).isEqualTo(2);
            assertThat(sequence).isEqualTo(i);
        }
        assertThat(Long.parseLong(generator.generate())).isPositive();
    }

    @Test
    void clockBackwardsThrowsWhenConfigured() {
        KsidConfig config = new KsidConfig(EPOCH, 41, 5, 5, 12,
                ClockBackwardsStrategy.THROW, Duration.ofSeconds(5),
                SequenceOverflowStrategy.WAIT, false, Duration.ofSeconds(20), 3);
        MutableClock clock = new MutableClock(NOW);
        KsidGenerator generator = new KsidGenerator(clock, new WorkerId(0, 1), config);
        generator.generateLong();
        clock.setInstant(NOW.minus(Duration.ofMillis(100)));
        assertThatThrownBy(generator::generateLong)
                .isInstanceOf(ClockBackwardsException.class)
                .hasMessageContaining("KNITID-2002");
    }

    @Test
    void clockBackwardsWaitTimesOut() {
        KsidConfig config = new KsidConfig(EPOCH, 41, 5, 5, 12,
                ClockBackwardsStrategy.WAIT, Duration.ofMillis(50),
                SequenceOverflowStrategy.WAIT, false, Duration.ofSeconds(20), 3);
        MutableClock clock = new MutableClock(NOW);
        KsidGenerator generator = new KsidGenerator(clock, new WorkerId(0, 1), config);
        generator.generateLong();
        clock.setInstant(NOW.minus(Duration.ofMillis(100)));
        assertThatThrownBy(generator::generateLong)
                .isInstanceOf(ClockBackwardsException.class)
                .hasMessageContaining("did not catch up");
    }

    @Test
    void clockBackwardsWaitBlocksUntilClockCatchesUp() throws Exception {
        MutableClock clock = new MutableClock(NOW);
        KsidGenerator generator = new KsidGenerator(clock, new WorkerId(0, 1), KsidConfig.defaults());
        long first = generator.generateLong();
        clock.setInstant(NOW.minus(Duration.ofMillis(100)));
        Thread advancer = new Thread(() -> {
            try {
                Thread.sleep(30);
            }
            catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            clock.setInstant(NOW.plus(Duration.ofMillis(1)));
        });
        advancer.setDaemon(true);
        advancer.start();
        long second = generator.generateLong();
        assertThat(second).isGreaterThan(first);
    }

    @Test
    void borrowFutureKeepsLogicalTimestampAndStaysMonotonic() {
        KsidConfig config = new KsidConfig(EPOCH, 41, 5, 5, 12,
                ClockBackwardsStrategy.BORROW_FUTURE, Duration.ofSeconds(5),
                SequenceOverflowStrategy.THROW, true, Duration.ofSeconds(20), 3);
        MutableClock clock = new MutableClock(NOW);
        KsidGenerator generator = new KsidGenerator(clock, new WorkerId(0, 1), config);
        long first = generator.generateLong();
        clock.setInstant(NOW.minus(Duration.ofSeconds(10)));
        long second = generator.generateLong();
        assertThat(second).isGreaterThan(first);
        long firstTimestamp = first >>> 22;
        long secondTimestamp = second >>> 22;
        assertThat(secondTimestamp).isEqualTo(firstTimestamp);
    }

    @Test
    void sequenceOverflowThrowsWhenConfigured() {
        KsidConfig config = sequenceOverflowConfig(SequenceOverflowStrategy.THROW);
        KsidGenerator generator = new KsidGenerator(new MutableClock(NOW), new WorkerId(0, 0), config);
        generator.generateLong();
        generator.generateLong();
        assertThatThrownBy(generator::generateLong)
                .isInstanceOf(IdGenerationException.class)
                .hasMessageContaining("KNITID-2005");
    }

    @Test
    void sequenceOverflowWaitsForNextMillisecond() throws Exception {
        KsidConfig config = sequenceOverflowConfig(SequenceOverflowStrategy.WAIT);
        MutableClock clock = new MutableClock(NOW);
        KsidGenerator generator = new KsidGenerator(clock, new WorkerId(0, 0), config);
        generator.generateLong();
        generator.generateLong();
        Thread advancer = new Thread(() -> {
            try {
                Thread.sleep(30);
            }
            catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            clock.advance(Duration.ofMillis(2));
        });
        advancer.setDaemon(true);
        advancer.start();
        long third = generator.generateLong();
        assertThat(third).isPositive();
        assertThat(third & 1L).isEqualTo(0L);
    }

    @Test
    void workerIdMustFitConfiguredBits() {
        KsidConfig config = KsidConfig.defaults();
        assertThatThrownBy(() -> new KsidGenerator(new MutableClock(NOW), new WorkerId(0, 32), config))
                .isInstanceOf(IdConfigurationException.class)
                .hasMessageContaining("workerId");
        assertThatThrownBy(() -> new KsidGenerator(new MutableClock(NOW), new WorkerId(32, 0), config))
                .isInstanceOf(IdConfigurationException.class)
                .hasMessageContaining("datacenterId");
    }

    @Test
    void epochInTheFutureIsRejected() {
        assertThatThrownBy(() -> new KsidGenerator(
                new MutableClock(Instant.parse("2023-01-01T00:00:00Z")),
                new WorkerId(0, 0), KsidConfig.defaults()))
                .isInstanceOf(IdConfigurationException.class)
                .hasMessageContaining("epoch");
    }

    @Test
    void renewableAssignerFailureMarksLeaseLostAndCloseReleases() throws Exception {
        KsidConfig config = new KsidConfig(EPOCH, 41, 5, 5, 12,
                ClockBackwardsStrategy.WAIT, Duration.ofSeconds(5),
                SequenceOverflowStrategy.WAIT, false, Duration.ofMillis(10), 1);
        FakeLeasableWorkerIdAssigner assigner = new FakeLeasableWorkerIdAssigner();
        KsidGenerator generator = new KsidGenerator(new MutableClock(NOW), new WorkerId(0, 1), config, assigner);
        assertThat(assigner.firstRenew.await(2, TimeUnit.SECONDS)).isTrue();
        assigner.failRenew.set(true);
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (generator.isLeaseValid() && System.nanoTime() < deadline) {
            Thread.sleep(5);
        }
        assertThat(generator.isLeaseValid()).isFalse();
        assertThatThrownBy(generator::generateLong)
                .isInstanceOf(IdGenerationException.class)
                .hasMessageContaining("KNITID-2007");
        generator.close();
        assertThat(assigner.released).isEqualTo(new WorkerId(0, 1));
        generator.close();
        assertThat(assigner.releaseCount.get()).isEqualTo(1);
    }

    private static KsidConfig sequenceOverflowConfig(SequenceOverflowStrategy overflow) {
        return new KsidConfig(EPOCH, 42, 10, 10, 1,
                ClockBackwardsStrategy.WAIT, Duration.ofSeconds(5),
                overflow, false, Duration.ofSeconds(20), 3);
    }

    private static final class FakeLeasableWorkerIdAssigner implements WorkerIdAssigner {

        private final CountDownLatch firstRenew = new CountDownLatch(1);

        private final AtomicBoolean failRenew = new AtomicBoolean();

        private final AtomicInteger releaseCount = new AtomicInteger();

        private volatile WorkerId released;

        @Override
        public WorkerId assign() {
            return new WorkerId(0, 1);
        }

        @Override
        public boolean renewable() {
            return true;
        }

        @Override
        public void renew() {
            firstRenew.countDown();
            if (failRenew.get()) {
                throw new IllegalStateException("simulated lease failure");
            }
        }

        @Override
        public void release(WorkerId workerId) {
            released = workerId;
            releaseCount.incrementAndGet();
        }
    }
}
