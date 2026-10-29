package org.unitseek.knitid.benchmarks;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
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

import org.unitseek.knitid.core.api.IdType;
import org.unitseek.knitid.core.spi.GeneratorConfig;
import org.unitseek.knitid.generator.biz.BizIdGeneratorFactory;
import org.unitseek.knitid.generator.biz.MemorySequenceProvider;

@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class BizBenchmark {

    private org.unitseek.knitid.core.api.IdGenerator generator;

    @Setup
    public void setup() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("biz.template", "{prefix}-{yyyyMMdd}-{seq:6}-{check}");
        values.put("biz.prefix", "ORD");
        values.put("biz.zone-id", "UTC");
        values.put("biz.sequence-provider", "memory");
        values.put("biz.reset", "DAILY");
        values.put("biz.check-algorithm", "MOD37_36");
        GeneratorConfig config = new GeneratorConfig() {
            @Override
            public String name() {
                return "order-id";
            }

            @Override
            public IdType type() {
                return IdType.BIZ;
            }

            @Override
            public Optional<Object> get(String key) {
                return Optional.ofNullable(values.get(key));
            }
        };
        BizIdGeneratorFactory factory = new BizIdGeneratorFactory(
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC),
                name -> new MemorySequenceProvider());
        generator = factory.create(config);
    }

    @Benchmark
    public String generate() {
        return generator.generate();
    }
}
