package org.unitseek.knitid.generator.ksid;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import org.unitseek.knitid.core.exception.IdConfigurationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KsidConfigTest {

    @Test
    void defaultsAreValid() {
        KsidConfig config = KsidConfig.defaults();
        assertThat(config.timestampBits()).isEqualTo(41);
        assertThat(config.datacenterIdBits()).isEqualTo(5);
        assertThat(config.workerIdBits()).isEqualTo(5);
        assertThat(config.sequenceBits()).isEqualTo(12);
        assertThat(config.clockBackwardsStrategy()).isEqualTo(ClockBackwardsStrategy.WAIT);
        assertThat(config.sequenceOverflowStrategy()).isEqualTo(SequenceOverflowStrategy.WAIT);
    }

    @Test
    void bitsMustSumTo63() {
        assertThatThrownBy(() -> config(40, 5, 5, 12, ClockBackwardsStrategy.WAIT,
                SequenceOverflowStrategy.WAIT, false))
                .isInstanceOf(IdConfigurationException.class)
                .hasMessageContaining("KNITID-1001")
                .hasMessageContaining("sum to 63");
    }

    @Test
    void sequenceBitsMustBePositive() {
        assertThatThrownBy(() -> config(41, 5, 5, 0, ClockBackwardsStrategy.WAIT,
                SequenceOverflowStrategy.WAIT, false))
                .isInstanceOf(IdConfigurationException.class)
                .hasMessageContaining("ksid.sequence-bits");
    }

    @Test
    void sequenceBitsMustFitIn63Bits() {
        assertThatThrownBy(() -> config(0, 0, 0, 63, ClockBackwardsStrategy.WAIT,
                SequenceOverflowStrategy.WAIT, false))
                .isInstanceOf(IdConfigurationException.class);
    }

    @Test
    void borrowFutureRequiresExplicitOptIn() {
        assertThatThrownBy(() -> config(41, 5, 5, 12, ClockBackwardsStrategy.BORROW_FUTURE,
                SequenceOverflowStrategy.WAIT, false))
                .isInstanceOf(IdConfigurationException.class)
                .hasMessageContaining("allow-borrow-future");
        assertThat(config(41, 5, 5, 12, ClockBackwardsStrategy.BORROW_FUTURE,
                SequenceOverflowStrategy.WAIT, true).allowBorrowFuture()).isTrue();
    }

    @Test
    void maxWaitAndLeaseIntervalMustBePositive() {
        assertThatThrownBy(() -> new KsidConfig(Instant.parse("2024-01-01T00:00:00Z"),
                41, 5, 5, 12, ClockBackwardsStrategy.WAIT, Duration.ZERO,
                SequenceOverflowStrategy.WAIT, false, Duration.ofSeconds(20), 3))
                .isInstanceOf(IdConfigurationException.class)
                .hasMessageContaining("max-wait");
        assertThatThrownBy(() -> new KsidConfig(Instant.parse("2024-01-01T00:00:00Z"),
                41, 5, 5, 12, ClockBackwardsStrategy.WAIT, Duration.ofSeconds(5),
                SequenceOverflowStrategy.WAIT, false, Duration.ZERO, 3))
                .isInstanceOf(IdConfigurationException.class)
                .hasMessageContaining("lease-renew-interval");
    }

    private static KsidConfig config(int timestampBits, int datacenterBits, int workerBits, int sequenceBits,
            ClockBackwardsStrategy backwards, SequenceOverflowStrategy overflow, boolean allowBorrow) {
        return new KsidConfig(Instant.parse("2024-01-01T00:00:00Z"), timestampBits, datacenterBits,
                workerBits, sequenceBits, backwards, Duration.ofSeconds(5), overflow, allowBorrow,
                Duration.ofSeconds(20), 3);
    }
}
