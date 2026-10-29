package org.unitseek.knitid.benchmarks;

import java.time.Clock;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import org.unitseek.knitid.core.random.SecureRandomProvider;
import org.unitseek.knitid.generator.uuid8.Uuid8Config;
import org.unitseek.knitid.generator.uuid8.Uuid8Generator;

@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class Uuid8Benchmark {

    private Uuid8Generator generator;

    @Setup
    public void setup() {
        generator = new Uuid8Generator(Clock.systemUTC(), Uuid8Config.defaults(), new SecureRandomProvider());
    }

    @Benchmark
    public String generate() {
        return generator.generate();
    }
}
