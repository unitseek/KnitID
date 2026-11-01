package org.unitseek.knitid.core.api;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdGeneratorBatchApiTest {

    private final SequenceGenerator generator = new SequenceGenerator();

    @Test
    void asyncBatchUsesExecutor() {
        List<String> values = generator.generateBatchAsync(4, Runnable::run).join();
        assertThat(values).containsExactly("1", "2", "3", "4");
    }

    @Test
    void streamIsLazyAndBounded() {
        List<String> values = generator.generateStream(3).collect(Collectors.toList());
        assertThat(values).containsExactly("1", "2", "3");
    }

    @Test
    void asyncNumericBatchWorks() {
        NumericSequenceGenerator numeric = new NumericSequenceGenerator();
        long[] values = numeric.generateLongBatchAsync(3, null, Runnable::run).join();
        assertThat(values).containsExactly(1L, 2L, 3L);
    }

    @Test
    void streamRejectsInvalidBatchSize() {
        assertThatThrownBy(() -> generator.generateStream(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> generator.generateBatchAsync(0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static final class SequenceGenerator implements IdGenerator {
        private final AtomicLong value = new AtomicLong();

        @Override public IdType type() { return IdType.UUID7; }
        @Override public String generate() { return Long.toString(value.incrementAndGet()); }
        @Override public String generate(IdContext context) { return generate(); }
    }

    private static final class NumericSequenceGenerator implements NumericIdGenerator {
        private final AtomicLong value = new AtomicLong();

        @Override public IdType type() { return IdType.KSID; }
        @Override public String generate() { return Long.toString(generateLong()); }
        @Override public String generate(IdContext context) { return generate(); }
        @Override public long generateLong() { return value.incrementAndGet(); }
        @Override public long generateLong(IdContext context) { return generateLong(); }
    }
}
