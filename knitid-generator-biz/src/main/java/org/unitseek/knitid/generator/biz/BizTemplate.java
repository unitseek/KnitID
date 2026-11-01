package org.unitseek.knitid.generator.biz;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.unitseek.knitid.core.exception.IdConfigurationException;
import org.unitseek.knitid.core.exception.IdErrorCodes;

/**
 * Compiled business-ID template.
 */
final class BizTemplate {

    enum Kind {
        LITERAL,
        PREFIX,
        YEAR,
        MONTH,
        DAY,
        HOUR,
        MINUTE,
        SECOND,
        DATE,
        DATE_TIME,
        SEQ,
        RANDOM,
        WORKER,
        CHECK
    }

    record Part(Kind kind, String literal, int width) {
    }

    private final List<Part> parts;

    private final boolean usesPrefix;

    private final boolean usesWorker;

    private final boolean usesCheck;

    private final boolean usesSeq;

    private final boolean usesRandom;

    private BizTemplate(List<Part> parts) {
        this.parts = Collections.unmodifiableList(parts);
        this.usesPrefix = parts.stream().anyMatch(part -> part.kind() == Kind.PREFIX);
        this.usesWorker = parts.stream().anyMatch(part -> part.kind() == Kind.WORKER);
        this.usesCheck = parts.stream().anyMatch(part -> part.kind() == Kind.CHECK);
        this.usesSeq = parts.stream().anyMatch(part -> part.kind() == Kind.SEQ);
        this.usesRandom = parts.stream().anyMatch(part -> part.kind() == Kind.RANDOM);
    }

    List<Part> parts() {
        return parts;
    }

    boolean usesPrefix() {
        return usesPrefix;
    }

    boolean usesWorker() {
        return usesWorker;
    }

    boolean usesCheck() {
        return usesCheck;
    }

    boolean usesSeq() {
        return usesSeq;
    }

    boolean usesRandom() {
        return usesRandom;
    }

    static BizTemplate compile(BizConfig config) {
        String template = config.template();
        List<Part> parts = new ArrayList<>();
        StringBuilder literal = new StringBuilder();
        boolean checkSeen = false;
        int index = 0;
        while (index < template.length()) {
            char ch = template.charAt(index);
            if (ch == '{') {
                if (index + 1 < template.length() && template.charAt(index + 1) == '{') {
                    literal.append('{');
                    index += 2;
                    continue;
                }
                int close = template.indexOf('}', index + 1);
                if (close < 0) {
                    throw invalid("Unclosed placeholder in biz.template: " + template);
                }
                String token = template.substring(index + 1, close);
                if (token.isBlank()) {
                    throw invalid("Empty placeholder in biz.template");
                }
                if (checkSeen) {
                    throw invalid("{check} must be the last template element");
                }
                if (!literal.isEmpty()) {
                    parts.add(new Part(Kind.LITERAL, literal.toString(), 0));
                    literal.setLength(0);
                }
                Part part = parsePart(token, config);
                if (part.kind() == Kind.CHECK) {
                    checkSeen = true;
                }
                parts.add(part);
                index = close + 1;
            }
            else if (ch == '}') {
                if (index + 1 < template.length() && template.charAt(index + 1) == '}') {
                    literal.append('}');
                    index += 2;
                }
                else {
                    throw invalid("Unmatched '}' in biz.template: " + template);
                }
            }
            else {
                literal.append(ch);
                index++;
            }
        }
        if (!literal.isEmpty()) {
            parts.add(new Part(Kind.LITERAL, literal.toString(), 0));
        }
        if (checkSeen && parts.get(parts.size() - 1).kind() != Kind.CHECK) {
            throw invalid("{check} must be the last template element");
        }
        if (parts.isEmpty()) {
            throw invalid("biz.template must not be empty");
        }
        BizTemplate compiled = new BizTemplate(parts);
        if (compiled.usesPrefix && config.prefix() == null) {
            throw invalid("biz.prefix is required when the template contains {prefix}");
        }
        if (compiled.usesWorker && config.nodeId() == null) {
            throw invalid("biz.node-id is required when the template contains {worker}");
        }
        if (compiled.usesCheck && config.checkAlgorithm() == CheckAlgorithm.NONE) {
            throw invalid("biz.check-algorithm must not be NONE when the template contains {check}");
        }
        return compiled;
    }

    private static Part parsePart(String token, BizConfig config) {
        return switch (token) {
            case "prefix" -> new Part(Kind.PREFIX, null, 0);
            case "yyyy" -> new Part(Kind.YEAR, null, 0);
            case "MM" -> new Part(Kind.MONTH, null, 0);
            case "dd" -> new Part(Kind.DAY, null, 0);
            case "HH" -> new Part(Kind.HOUR, null, 0);
            case "mm" -> new Part(Kind.MINUTE, null, 0);
            case "ss" -> new Part(Kind.SECOND, null, 0);
            case "yyyyMMdd" -> new Part(Kind.DATE, null, 0);
            case "yyyyMMddHHmmss" -> new Part(Kind.DATE_TIME, null, 0);
            case "worker" -> new Part(Kind.WORKER, null, 0);
            case "check" -> new Part(Kind.CHECK, null, 0);
            default -> {
                if (token.startsWith("seq:")) {
                    int width = parseWidth(token.substring(4), 1, 18, "seq");
                    yield new Part(Kind.SEQ, null, width);
                }
                if (token.startsWith("random:")) {
                    int width = parseWidth(token.substring(7), 1, 32, "random");
                    yield new Part(Kind.RANDOM, null, width);
                }
                throw invalid("Unknown or misplaced placeholder in biz.template: {" + token + "}");
            }
        };
    }

    private static int parseWidth(String value, int min, int max, String token) {
        try {
            int width = Integer.parseInt(value);
            if (width < min || width > max) {
                throw invalid("{" + token + ":" + value + "} width must be between " + min + " and " + max);
            }
            return width;
        }
        catch (NumberFormatException ex) {
            throw invalid("Invalid width in {" + token + ":" + value + "}");
        }
    }

    private static IdConfigurationException invalid(String message) {
        return new IdConfigurationException(IdErrorCodes.CONFIG_INVALID, message);
    }
}
