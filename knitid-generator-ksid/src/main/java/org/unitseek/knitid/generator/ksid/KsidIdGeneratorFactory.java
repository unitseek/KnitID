package org.unitseek.knitid.generator.ksid;

import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.unitseek.knitid.core.api.IdGenerator;
import org.unitseek.knitid.core.api.IdType;
import org.unitseek.knitid.core.exception.IdConfigurationException;
import org.unitseek.knitid.core.exception.IdErrorCodes;
import org.unitseek.knitid.core.spi.GeneratorConfig;
import org.unitseek.knitid.core.spi.IdGeneratorFactory;
import org.unitseek.knitid.core.spi.WorkerId;
import org.unitseek.knitid.core.spi.WorkerIdAssigner;
import org.unitseek.knitid.core.spi.WorkerIdAssignerProvider;
import org.unitseek.knitid.core.worker.EnvironmentWorkerIdAssigner;
import org.unitseek.knitid.core.worker.FixedWorkerIdAssigner;

/**
 * Factory for {@link KsidGenerator}.
 *
 * <p>This milestone supports {@code fixed} and {@code environment} WorkerId
 * assigners; Redis/ZooKeeper/Kubernetes assigners are delivered in M2.</p>
 */
public final class KsidIdGeneratorFactory implements IdGeneratorFactory {

    private final Clock clock;

    private final List<WorkerIdAssignerProvider> assignerProviders;

    public KsidIdGeneratorFactory() {
        this(Clock.systemUTC(), List.of());
    }

    public KsidIdGeneratorFactory(Clock clock) {
        this(clock, List.of());
    }

    public KsidIdGeneratorFactory(Clock clock, List<WorkerIdAssignerProvider> assignerProviders) {
        this.clock = clock;
        this.assignerProviders = assignerProviders == null ? List.of() : List.copyOf(assignerProviders);
    }

    @Override
    public IdType type() {
        return IdType.KSID;
    }

    @Override
    public IdGenerator create(GeneratorConfig config) {
        KsidConfig ksidConfig = KsidConfig.from(config);
        String assignerType = config.getString("ksid.worker-id-assigner", "fixed")
                .trim()
                .toLowerCase(Locale.ROOT);
        WorkerIdAssigner assigner;
        WorkerId workerId;
        switch (assignerType) {
            case "fixed" -> {
                int worker = config.getInt("ksid.worker-id", -1);
                if (worker < 0) {
                    throw new IdConfigurationException(
                            IdErrorCodes.CONFIG_INVALID,
                            "ksid.worker-id is required when ksid.worker-id-assigner=fixed");
                }
                int datacenter = config.getInt("ksid.datacenter-id", 0);
                if (datacenter < 0) {
                    throw new IdConfigurationException(
                            IdErrorCodes.CONFIG_INVALID, "ksid.datacenter-id must be >= 0");
                }
                FixedWorkerIdAssigner fixed = new FixedWorkerIdAssigner(datacenter, worker);
                assigner = fixed;
                workerId = fixed.assign();
            }
            case "environment" -> {
                EnvironmentWorkerIdAssigner environment = new EnvironmentWorkerIdAssigner();
                assigner = environment;
                workerId = environment.assign();
            }
            default -> {
                WorkerIdAssignerProvider provider = assignerProviders.stream()
                        .filter(candidate -> candidate.name().equalsIgnoreCase(assignerType))
                        .findFirst()
                        .orElseThrow(() -> new IdConfigurationException(
                                IdErrorCodes.CONFIG_INVALID,
                                "Unsupported ksid.worker-id-assigner: " + assignerType
                                        + " (available: fixed, environment"
                                        + providerNames() + ")"));
                int datacenter = config.getInt("ksid.datacenter-id", 0);
                assigner = provider.create(datacenter, ksidConfig.workerIdBits());
                workerId = assigner.assign();
            }
        }
        return new KsidGenerator(clock, workerId, ksidConfig, assigner);
    }

    private String providerNames() {
        if (assignerProviders.isEmpty()) {
            return "";
        }
        return ", " + assignerProviders.stream()
                .map(WorkerIdAssignerProvider::name)
                .sorted()
                .collect(Collectors.joining(", "));
    }
}
