package org.unitseek.knitid.sequence.redis;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

/**
 * RedisSequenceProvider overhead benchmark using an in-memory atomic store.
 * This isolates provider logic from network latency; use a real Redis endpoint
 * for end-to-end latency measurements.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@Threads(8)
public class RedisSequenceBenchmark {

    private RedisSequenceProvider provider;

    @Setup
    public void setup() {
        provider = new RedisSequenceProvider(new AtomicStore(), "bench", Duration.ofSeconds(60));
    }

    @Benchmark
    public long next() {
        return provider.next("scope");
    }

    private static final class AtomicStore implements RedisSequenceStore {
        private final AtomicLong value = new AtomicLong();

        @Override
        public long increment(String key, long delta, Duration ttl) {
            return value.addAndGet(delta);
        }
    }
}
