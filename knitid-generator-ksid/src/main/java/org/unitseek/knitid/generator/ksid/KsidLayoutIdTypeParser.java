package org.unitseek.knitid.generator.ksid;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import org.unitseek.knitid.core.api.IdType;
import org.unitseek.knitid.core.parser.IdInfo;
import org.unitseek.knitid.core.parser.IdTypeParser;

/**
 * Built-in parser for numeric KSID values using a specific bit layout.
 *
 * <p>Spring Boot auto-configuration registers this parser when a KSID generator
 * is configured. It can also be registered manually for non-Spring usage.
 * Without a configured layout, {@code KsidNumericIdTypeParser} only exposes the
 * raw decimal value.</p>
 *
 * @since 1.0.0
 */
public final class KsidLayoutIdTypeParser implements IdTypeParser {

    private final KsidConfig config;
    private final int workerShift;
    private final int datacenterShift;
    private final int timestampShift;
    private final long maxSequence;
    private final long maxDatacenterId;
    private final long maxWorkerId;

    /**
     * Creates a parser for the supplied layout.
     *
     * @param config KSID layout configuration
     */
    public KsidLayoutIdTypeParser(KsidConfig config) {
        this.config = config;
        this.workerShift = config.sequenceBits();
        this.datacenterShift = config.workerIdBits() + config.sequenceBits();
        this.timestampShift = config.datacenterIdBits() + config.workerIdBits() + config.sequenceBits();
        this.maxSequence = (1L << config.sequenceBits()) - 1L;
        this.maxDatacenterId = (1L << config.datacenterIdBits()) - 1L;
        this.maxWorkerId = (1L << config.workerIdBits()) - 1L;
    }

    @Override
    public IdType type() {
        return IdType.KSID;
    }

    @Override
    public Optional<IdInfo> tryParse(String raw) {
        if (raw == null || raw.isEmpty() || raw.length() > 19
                || (raw.length() > 1 && raw.charAt(0) == '0')) {
            return Optional.empty();
        }
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            if (ch < '0' || ch > '9') {
                return Optional.empty();
            }
        }
        long value;
        try {
            value = Long.parseLong(raw);
        }
        catch (NumberFormatException ex) {
            return Optional.empty();
        }
        long timestamp = value >>> timestampShift;
        long datacenter = (value >>> datacenterShift) & maxDatacenterId;
        long worker = (value >>> workerShift) & maxWorkerId;
        long sequence = value & maxSequence;
        Instant instant = config.epoch().plusMillis(timestamp);
        long nodeId = (datacenter << config.workerIdBits()) | worker;
        return Optional.of(new IdInfo(IdType.KSID, raw, instant, null, nodeId, sequence, null,
                Map.of("datacenterId", datacenter, "workerId", worker)));
    }
}
