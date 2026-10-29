package org.unitseek.knitid.generator.biz;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import org.unitseek.knitid.core.api.IdContext;
import org.unitseek.knitid.core.api.IdGenerator;
import org.unitseek.knitid.core.api.IdType;
import org.unitseek.knitid.core.exception.IdErrorCodes;
import org.unitseek.knitid.core.exception.IdGenerationException;
import org.unitseek.knitid.core.spi.RandomProvider;
import org.unitseek.knitid.core.spi.SequenceProvider;

/**
 * Template driven business identifier generator.
 */
public final class BizIdGenerator implements IdGenerator {

    private static final String RANDOM_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private static final int MAX_OUTPUT_LENGTH = 128;

    private final Clock clock;

    private final BizConfig config;

    private final BizTemplate template;

    private final SequenceProvider sequenceProvider;

    private final RandomProvider random;

    public BizIdGenerator(Clock clock, BizConfig config, BizTemplate template,
            SequenceProvider sequenceProvider, RandomProvider random) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.template = Objects.requireNonNull(template, "template must not be null");
        this.sequenceProvider = Objects.requireNonNull(sequenceProvider, "sequenceProvider must not be null");
        this.random = Objects.requireNonNull(random, "random must not be null");
    }

    @Override
    public IdType type() {
        return IdType.BIZ;
    }

    @Override
    public String generate() {
        return generate(null);
    }

    @Override
    public synchronized String generate(IdContext context) {
        LocalDateTime now = LocalDateTime.ofInstant(Instant.ofEpochMilli(clock.millis()), config.zoneId());
        LocalDate businessDate = context != null && context.businessDate() != null
                ? context.businessDate()
                : now.toLocalDate();
        LocalDateTime effective = LocalDateTime.of(businessDate, now.toLocalTime());
        String prefix = resolvePrefix(context);
        Long workerId = context != null && context.nodeId() != null ? context.nodeId() : config.nodeId();
        long sequence = 0L;
        if (template.usesSeq()) {
            String scope = sequenceScope(prefix, businessDate);
            sequence = sequenceProvider.next(scope);
        }
        StringBuilder output = new StringBuilder(64);
        for (BizTemplate.Part part : template.parts()) {
            switch (part.kind()) {
                case LITERAL -> output.append(part.literal());
                case PREFIX -> output.append(prefix);
                case YEAR -> output.append(String.format("%04d", effective.getYear()));
                case MONTH -> output.append(String.format("%02d", effective.getMonthValue()));
                case DAY -> output.append(String.format("%02d", effective.getDayOfMonth()));
                case HOUR -> output.append(String.format("%02d", effective.getHour()));
                case MINUTE -> output.append(String.format("%02d", effective.getMinute()));
                case SECOND -> output.append(String.format("%02d", effective.getSecond()));
                case DATE -> output.append(String.format("%04d%02d%02d",
                        effective.getYear(), effective.getMonthValue(), effective.getDayOfMonth()));
                case DATE_TIME -> output.append(String.format("%04d%02d%02d%02d%02d%02d",
                        effective.getYear(), effective.getMonthValue(), effective.getDayOfMonth(),
                        effective.getHour(), effective.getMinute(), effective.getSecond()));
                case SEQ -> output.append(padded(sequence, part.width()));
                case RANDOM -> output.append(randomDigits(part.width()));
                case WORKER -> {
                    if (workerId == null) {
                        throw new IdGenerationException(
                                IdErrorCodes.GENERATION_FAILED, "No worker id available for {worker}");
                    }
                    output.append(workerId);
                }
                case CHECK -> output.append(config.checkAlgorithm().checkDigit(output.toString()));
                default -> throw new IllegalStateException("Unhandled part: " + part.kind());
            }
        }
        if (output.length() > MAX_OUTPUT_LENGTH) {
            throw new IdGenerationException(
                    IdErrorCodes.TEMPLATE_TOO_LONG,
                    "Business ID length " + output.length() + " exceeds " + MAX_OUTPUT_LENGTH);
        }
        return output.toString();
    }

    private String resolvePrefix(IdContext context) {
        String prefix = context != null && context.prefix() != null ? context.prefix() : config.prefix();
        if (prefix == null) {
            return "";
        }
        if (!BizConfig.isValidPrefix(prefix)) {
            throw new IdGenerationException(
                    IdErrorCodes.GENERATION_FAILED, "Invalid business prefix: " + prefix);
        }
        return prefix;
    }

    private String sequenceScope(String prefix, LocalDate date) {
        String scopePrefix = prefix.isEmpty() ? "_" : prefix;
        return config.reset() == SequenceReset.DAILY
                ? scopePrefix + ":" + date.getYear() + String.format("%02d%02d", date.getMonthValue(), date.getDayOfMonth())
                : scopePrefix;
    }

    private String padded(long value, int width) {
        long max = 1L;
        for (int i = 0; i < width; i++) {
            max *= 10L;
        }
        if (value < 0 || value >= max) {
            throw new IdGenerationException(
                    IdErrorCodes.SEQUENCE_EXHAUSTED,
                    "Business sequence value " + value + " does not fit in " + width + " digits");
        }
        return String.format("%0" + width + "d", value);
    }

    private String randomDigits(int width) {
        StringBuilder builder = new StringBuilder(width);
        for (int i = 0; i < width; i++) {
            builder.append(RANDOM_ALPHABET.charAt(random.nextInt(RANDOM_ALPHABET.length())));
        }
        return builder.toString();
    }
}
