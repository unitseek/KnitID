package org.unitseek.knitid.autoconfigure.metrics;

import io.micrometer.core.instrument.MeterRegistry;

import org.unitseek.knitid.core.api.IdGenerator;
import org.unitseek.knitid.core.api.NumericIdGenerator;

/**
 * Wraps generators with timing, error and batch instruments.
 */
public final class IdGeneratorMetrics {

    private IdGeneratorMetrics() {
    }

    public static IdGenerator wrap(IdGenerator generator, String name, MeterRegistry registry) {
        if (registry == null) {
            return generator;
        }
        GenerationMetrics metrics = new GenerationMetrics(registry, name, generator.type());
        if (generator instanceof NumericIdGenerator numeric) {
            return new MeteredNumericIdGenerator(numeric, metrics);
        }
        return new MeteredIdGenerator(generator, metrics);
    }
}
