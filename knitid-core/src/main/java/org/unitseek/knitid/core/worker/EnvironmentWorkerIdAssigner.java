package org.unitseek.knitid.core.worker;

import java.util.Objects;
import java.util.function.Function;

import org.unitseek.knitid.core.exception.IdErrorCodes;
import org.unitseek.knitid.core.exception.WorkerIdUnavailableException;
import org.unitseek.knitid.core.spi.WorkerId;
import org.unitseek.knitid.core.spi.WorkerIdAssigner;

/**
 * WorkerId from environment variables or system properties.
 */
public final class EnvironmentWorkerIdAssigner implements WorkerIdAssigner {

    public static final String WORKER_ID_NAME = "KNITID_WORKER_ID";

    public static final String DATACENTER_ID_NAME = "KNITID_DATACENTER_ID";

    private final Function<String, String> lookup;

    public EnvironmentWorkerIdAssigner() {
        this(EnvironmentWorkerIdAssigner::lookupSystem);
    }

    public EnvironmentWorkerIdAssigner(Function<String, String> lookup) {
        this.lookup = Objects.requireNonNull(lookup, "lookup must not be null");
    }

    private static String lookupSystem(String name) {
        String value = System.getProperty(name);
        return value != null ? value : System.getenv(name);
    }

    @Override
    public WorkerId assign() {
        String worker = lookup.apply(WORKER_ID_NAME);
        String datacenter = lookup.apply(DATACENTER_ID_NAME);
        if (worker == null || worker.isBlank()) {
            throw new WorkerIdUnavailableException(
                    IdErrorCodes.WORKER_UNAVAILABLE,
                    "Missing environment WorkerId: " + WORKER_ID_NAME);
        }
        try {
            int datacenterId = datacenter == null || datacenter.isBlank()
                    ? 0
                    : Integer.parseInt(datacenter.trim());
            return new WorkerId(datacenterId, Integer.parseInt(worker.trim()));
        }
        catch (IllegalArgumentException ex) {
            throw new WorkerIdUnavailableException(
                    IdErrorCodes.WORKER_UNAVAILABLE,
                    "Invalid environment WorkerId: " + worker + ", datacenter: " + datacenter,
                    ex);
        }
    }
}
