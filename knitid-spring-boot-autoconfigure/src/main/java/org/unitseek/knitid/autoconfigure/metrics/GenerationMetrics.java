package org.unitseek.knitid.autoconfigure.metrics;

import java.util.Locale;
import java.util.function.Supplier;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import org.unitseek.knitid.core.api.IdType;
import org.unitseek.knitid.core.exception.ClockBackwardsException;
import org.unitseek.knitid.core.exception.IdConfigurationException;
import org.unitseek.knitid.core.exception.IdErrorCodes;
import org.unitseek.knitid.core.exception.IdException;
import org.unitseek.knitid.core.exception.IdGenerationException;
import org.unitseek.knitid.core.exception.WorkerIdUnavailableException;

/**
 * Per-generator Micrometer instruments.
 */
final class GenerationMetrics {

    private final MeterRegistry registry;

    private final String type;

    private final String name;

    private final Timer timer;

    private final DistributionSummary batchSize;

    private final java.util.concurrent.ConcurrentMap<String, Counter> errorCounters =
            new java.util.concurrent.ConcurrentHashMap<>();

    GenerationMetrics(MeterRegistry registry, String name, IdType type) {
        this.registry = registry;
        this.type = type.value().toLowerCase(Locale.ROOT);
        this.name = name;
        this.timer = Timer.builder("knitid.id.generation.duration")
                .tag("type", this.type)
                .tag("name", name)
                .tag("id.type", this.type)
                .tag("id.name", name)
                .description("KnitID generation duration")
                .register(registry);
        this.batchSize = DistributionSummary.builder("knitid.id.batch.size")
                .tag("type", this.type)
                .tag("name", name)
                .tag("id.type", this.type)
                .tag("id.name", name)
                .description("KnitID batch sizes")
                .register(registry);
    }

    <T> T record(Supplier<T> call) {
        long start = System.nanoTime();
        long elapsed = 0L;
        try {
            T result = call.get();
            elapsed = System.nanoTime() - start;
            return result;
        }
        catch (RuntimeException ex) {
            elapsed = System.nanoTime() - start;
            String reason = reason(ex);
            errorCounters.computeIfAbsent(reason, key -> registry.counter("knitid.id.generation.errors",
                    "type", type, "name", name, "reason", key,
                    "id.type", type, "id.name", name, "error.type", key)).increment();
            throw ex;
        }
        finally {
            timer.record(elapsed, java.util.concurrent.TimeUnit.NANOSECONDS);
        }
    }

    void recordBatch(int size) {
        batchSize.record(size);
    }

    private static String reason(Throwable failure) {
        if (failure instanceof ClockBackwardsException) {
            return "CLOCK_BACKWARDS";
        }
        if (failure instanceof WorkerIdUnavailableException) {
            return "WORKER_UNAVAILABLE";
        }
        if (failure instanceof IdConfigurationException) {
            return "CONFIG";
        }
        if (failure instanceof IdException idException
                && IdErrorCodes.WORKER_LEASE_LOST.equals(idException.code())) {
            return "WORKER_UNAVAILABLE";
        }
        if (failure instanceof IdException idException
                && IdErrorCodes.SEQUENCE_EXHAUSTED.equals(idException.code())) {
            return "SEQ_EXHAUSTED";
        }
        if (failure instanceof IdGenerationException) {
            return "INTERNAL";
        }
        return "INTERNAL";
    }
}
