package org.unitseek.knitid.autoconfigure.metrics;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.unitseek.knitid.core.api.HealthState;
import org.unitseek.knitid.core.api.IdContext;
import org.unitseek.knitid.core.api.IdGenerator;
import org.unitseek.knitid.core.api.IdGeneratorHealth;
import org.unitseek.knitid.core.api.IdGeneratorHealthSnapshot;
import org.unitseek.knitid.core.api.IdGeneratorConstants;
import org.unitseek.knitid.core.api.IdType;

/**
 * Instrumented {@link IdGenerator}.
 */
final class MeteredIdGenerator implements IdGenerator, IdGeneratorHealth {

    private final IdGenerator delegate;

    private final GenerationMetrics metrics;

    MeteredIdGenerator(IdGenerator delegate, GenerationMetrics metrics) {
        this.delegate = delegate;
        this.metrics = metrics;
    }

    @Override
    public IdType type() {
        return delegate.type();
    }

    @Override
    public boolean blocking() {
        return delegate.blocking();
    }

    @Override
    public String generate() {
        return metrics.record(delegate::generate);
    }

    @Override
    public String generate(IdContext context) {
        return metrics.record(() -> delegate.generate(context));
    }

    @Override
    public List<String> generateBatch(int size) {
        metrics.recordBatch(size);
        IdGeneratorConstants.checkBatchSize(size);
        List<String> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            result.add(generate());
        }
        return result;
    }

    @Override
    public List<String> generateBatch(int size, IdContext context) {
        metrics.recordBatch(size);
        IdGeneratorConstants.checkBatchSize(size);
        List<String> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            result.add(generate(context));
        }
        return result;
    }

    @Override
    public IdGeneratorHealthSnapshot health(Duration clockBackwardsWindow) {
        if (delegate instanceof IdGeneratorHealth health) {
            return health.health(clockBackwardsWindow);
        }
        return new IdGeneratorHealthSnapshot(HealthState.UP, "ready",
                Map.of("type", type().value()));
    }
}
