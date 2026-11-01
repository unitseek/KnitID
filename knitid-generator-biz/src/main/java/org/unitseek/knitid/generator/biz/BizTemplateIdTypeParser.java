package org.unitseek.knitid.generator.biz;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unitseek.knitid.core.api.IdType;
import org.unitseek.knitid.core.parser.IdInfo;
import org.unitseek.knitid.core.parser.IdTypeParser;

/**
 * Built-in parser for business IDs generated from a specific {@link BizConfig}
 * template.
 *
 * <p>Spring Boot auto-configuration registers this parser when a BIZ generator
 * is configured. It can also be registered manually for non-Spring usage. It
 * validates the template shape, check digit, and extracts sequence, worker,
 * prefix and date/time where present.</p>
 *
 * @since 1.0.0
 */
public final class BizTemplateIdTypeParser implements IdTypeParser {

    private static final Pattern PREFIX = Pattern.compile("[A-Za-z0-9_-]{1,16}");

    private final BizConfig config;
    private final Pattern pattern;
    private final List<Integer> groupIndexes;

    public BizTemplateIdTypeParser(BizConfig config) {
        this.config = config;
        BizTemplate template = BizTemplate.compile(config);
        StringBuilder regex = new StringBuilder("^");
        List<Integer> groups = new ArrayList<>();
        int group = 1;
        for (BizTemplate.Part part : template.parts()) {
            switch (part.kind()) {
                case LITERAL -> regex.append(Pattern.quote(part.literal()));
                case PREFIX -> {
                    regex.append('(').append(PREFIX.pattern()).append(')');
                    groups.add(group++);
                }
                case YEAR -> {
                    regex.append("(\\d{4})");
                    groups.add(group++);
                }
                case MONTH, DAY, HOUR, MINUTE, SECOND -> {
                    regex.append("(\\d{2})");
                    groups.add(group++);
                }
                case DATE -> {
                    regex.append("(\\d{8})");
                    groups.add(group++);
                }
                case DATE_TIME -> {
                    regex.append("(\\d{14})");
                    groups.add(group++);
                }
                case SEQ -> {
                    regex.append("(\\d{").append(part.width()).append("})");
                    groups.add(group++);
                }
                case RANDOM -> {
                    regex.append("([0-9A-Z]{").append(part.width()).append("})");
                    groups.add(group++);
                }
                case WORKER -> {
                    regex.append("(\\d+)");
                    groups.add(group++);
                }
                case CHECK -> {
                    regex.append("([0-9A-Z])");
                    groups.add(group++);
                }
                default -> throw new IllegalStateException("Unhandled BIZ part: " + part.kind());
            }
        }
        regex.append('$');
        this.pattern = Pattern.compile(regex.toString());
        this.groupIndexes = List.copyOf(groups);
    }

    @Override
    public IdType type() {
        return IdType.BIZ;
    }

    @Override
    public Optional<IdInfo> tryParse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = pattern.matcher(raw);
        if (!matcher.matches()) {
            return Optional.empty();
        }

        String prefix = null;
        Long sequence = null;
        Long nodeId = null;
        int[] values = new int[6];
        boolean[] present = new boolean[6];
        int dateStyle = 0;
        String dateDigits = null;
        int group = 0;
        for (BizTemplate.Part part : BizTemplate.compile(config).parts()) {
            if (part.kind() == BizTemplate.Kind.LITERAL) {
                continue;
            }
            String value = matcher.group(groupIndexes.get(group++));
            switch (part.kind()) {
                case PREFIX -> prefix = value;
                case YEAR -> {
                    values[0] = Integer.parseInt(value);
                    present[0] = true;
                }
                case MONTH -> {
                    values[1] = Integer.parseInt(value);
                    present[1] = true;
                }
                case DAY -> {
                    values[2] = Integer.parseInt(value);
                    present[2] = true;
                }
                case HOUR -> {
                    values[3] = Integer.parseInt(value);
                    present[3] = true;
                }
                case MINUTE -> {
                    values[4] = Integer.parseInt(value);
                    present[4] = true;
                }
                case SECOND -> {
                    values[5] = Integer.parseInt(value);
                    present[5] = true;
                }
                case DATE -> {
                    dateStyle = 8;
                    dateDigits = value;
                }
                case DATE_TIME -> {
                    dateStyle = 14;
                    dateDigits = value;
                }
                case SEQ -> sequence = Long.parseLong(value);
                case WORKER -> nodeId = Long.parseLong(value);
                case CHECK -> {
                    String content = raw.substring(0, matcher.start(groupIndexes.get(group - 1)));
                    try {
                        if (config.checkAlgorithm().checkDigit(content) != value.charAt(0)) {
                            return Optional.empty();
                        }
                    }
                    catch (RuntimeException ex) {
                        return Optional.empty();
                    }
                }
                case RANDOM -> {
                    // Random parts carry no metadata.
                }
                default -> throw new IllegalStateException("Unhandled BIZ part: " + part.kind());
            }
        }

        Instant timestamp = null;
        if (dateStyle > 0) {
            timestamp = parseTimestamp(dateDigits, dateStyle);
            if (timestamp == null) {
                return Optional.empty();
            }
        }
        else if (present[0]) {
            try {
                LocalDateTime dateTime = LocalDateTime.of(
                        values[0],
                        present[1] ? values[1] : 1,
                        present[2] ? values[2] : 1,
                        present[3] ? values[3] : 0,
                        present[4] ? values[4] : 0,
                        present[5] ? values[5] : 0);
                timestamp = dateTime.atZone(config.zoneId()).toInstant();
            }
            catch (RuntimeException ex) {
                return Optional.empty();
            }
        }
        return Optional.of(new IdInfo(IdType.BIZ, raw, timestamp, null, nodeId, sequence, prefix, Map.of()));
    }

    private Instant parseTimestamp(String digits, int length) {
        try {
            int year = Integer.parseInt(digits.substring(0, 4));
            int month = Integer.parseInt(digits.substring(4, 6));
            int day = Integer.parseInt(digits.substring(6, 8));
            int hour = length == 14 ? Integer.parseInt(digits.substring(8, 10)) : 0;
            int minute = length == 14 ? Integer.parseInt(digits.substring(10, 12)) : 0;
            int second = length == 14 ? Integer.parseInt(digits.substring(12, 14)) : 0;
            return LocalDateTime.of(LocalDate.of(year, month, day), LocalTime.of(hour, minute, second))
                    .atZone(config.zoneId()).toInstant();
        }
        catch (RuntimeException ex) {
            return null;
        }
    }
}
