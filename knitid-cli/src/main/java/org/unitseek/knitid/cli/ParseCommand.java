package org.unitseek.knitid.cli;

import java.io.PrintStream;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.unitseek.knitid.core.api.IdType;
import org.unitseek.knitid.core.parser.IdInfo;
import org.unitseek.knitid.generator.ksuid.KsuidIdTypeParser;
import org.unitseek.knitid.generator.typeid.TypeIdIdTypeParser;

/**
 * Implements {@code knitid parse}.
 *
 * @since 1.0.0
 */
final class ParseCommand {

    private static final String CROCKFORD = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";

    private ParseCommand() {
    }

    static int run(String[] args, PrintStream out) {
        String requestedType = null;
        String format = "text";
        String raw = null;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--type" -> requestedType = value(args, ++i, arg);
                case "--format" -> format = format(value(args, ++i, arg));
                case "--help", "-h" -> {
                    out.println(help());
                    return 0;
                }
                default -> {
                    if (arg.startsWith("-")) {
                        throw usage("Unknown parse option: " + arg);
                    }
                    if (raw != null) {
                        throw usage("parse accepts exactly one identifier");
                    }
                    raw = arg;
                }
            }
        }
        if (raw == null || raw.isBlank()) {
            throw usage("parse requires an identifier");
        }
        IdInfo info = parse(raw, requestedType);
        if ("json".equals(format)) {
            out.println(toJson(info, raw, requestedType));
        }
        else {
            printText(out, info);
        }
        return 0;
    }

    private static IdInfo parse(String raw, String requestedType) {
        if (requestedType == null) {
            return autoParse(raw);
        }
        IdType type;
        try {
            type = IdType.of(requestedType);
        }
        catch (IllegalArgumentException ex) {
            throw usage("Invalid type: " + requestedType);
        }
        return switch (type.value()) {
            case "UUID7" -> parseUuid(raw, type, 7);
            case "UUID8" -> parseUuid(raw, type, 8);
            case "ULID" -> parseUlid(raw);
            case "KSUID" -> new KsuidIdTypeParser().tryParse(raw)
                    .orElseThrow(() -> failure("Not a valid KSUID"));
            case "TYPEID" -> new TypeIdIdTypeParser().tryParse(raw)
                    .orElseThrow(() -> failure("Not a valid TypeID"));
            case "KSID" -> parseNumeric(raw);
            default -> throw usage("CLI parse does not support type " + type.value());
        };
    }

    private static IdInfo autoParse(String raw) {
        try {
            UUID uuid = UUID.fromString(raw);
            if (uuid.version() == 7) {
                return fromUuid(raw, IdType.UUID7, uuid);
            }
            if (uuid.version() == 8) {
                return fromUuid(raw, IdType.UUID8, uuid);
            }
        }
        catch (IllegalArgumentException ignored) {
            // Not a UUID.
        }
        Optional<IdInfo> ksuid = new KsuidIdTypeParser().tryParse(raw);
        if (ksuid.isPresent()) {
            return ksuid.get();
        }
        Optional<IdInfo> typeid = new TypeIdIdTypeParser().tryParse(raw);
        if (typeid.isPresent()) {
            return typeid.get();
        }
        Optional<IdInfo> ulid = tryUlid(raw);
        if (ulid.isPresent()) {
            return ulid.get();
        }
        if (isNumeric(raw) && raw.length() <= 19) {
            return parseNumeric(raw);
        }
        throw failure("Not a recognized KnitID value");
    }

    private static IdInfo parseUuid(String raw, IdType type, int expectedVersion) {
        try {
            UUID uuid = UUID.fromString(raw);
            if (uuid.version() != expectedVersion) {
                throw failure("Expected UUID version " + expectedVersion + " but found " + uuid.version());
            }
            return fromUuid(raw, type, uuid);
        }
        catch (IllegalArgumentException ex) {
            throw failure("Not a valid UUID");
        }
    }

    private static IdInfo fromUuid(String raw, IdType type, UUID uuid) {
        Long timestamp = null;
        if (uuid.version() == 7) {
            timestamp = uuid.getMostSignificantBits() >>> 16;
        }
        return new IdInfo(type, raw, timestamp == null ? null : Instant.ofEpochMilli(timestamp),
                uuid.version(), null, null, null, Map.of());
    }

    private static IdInfo parseUlid(String raw) {
        return tryUlid(raw).orElseThrow(() -> failure("Not a valid ULID"));
    }

    private static Optional<IdInfo> tryUlid(String raw) {
        if (raw.length() != 26) {
            return Optional.empty();
        }
        long value = 0L;
        for (int i = 0; i < 10; i++) {
            int digit = CROCKFORD.indexOf(Character.toUpperCase(raw.charAt(i)));
            if (digit < 0) {
                return Optional.empty();
            }
            value = (value << 5) | digit;
        }
        if ((value >>> 48) != 0) {
            return Optional.empty();
        }
        for (int i = 10; i < 26; i++) {
            if (CROCKFORD.indexOf(Character.toUpperCase(raw.charAt(i))) < 0) {
                return Optional.empty();
            }
        }
        return Optional.of(new IdInfo(IdType.ULID, raw, Instant.ofEpochMilli(value), null,
                null, null, null, Map.of()));
    }

    private static IdInfo parseNumeric(String raw) {
        if (!isNumeric(raw) || raw.length() > 19) {
            throw failure("Not a numeric KSID");
        }
        try {
            Long.parseLong(raw);
        }
        catch (NumberFormatException ex) {
            throw failure("Numeric KSID is out of range");
        }
        return IdInfo.of(IdType.KSID, raw);
    }

    private static boolean isNumeric(String raw) {
        if (raw.isEmpty() || (raw.length() > 1 && raw.charAt(0) == '0')) {
            return false;
        }
        for (int i = 0; i < raw.length(); i++) {
            if (raw.charAt(i) < '0' || raw.charAt(i) > '9') {
                return false;
            }
        }
        return true;
    }

    private static void printText(PrintStream out, IdInfo info) {
        out.println("type=" + info.type().value());
        out.println("raw=" + info.raw());
        if (info.timestamp() != null) {
            out.println("timestamp=" + info.timestamp());
        }
        if (info.version() != null) {
            out.println("version=" + info.version());
        }
        if (info.nodeId() != null) {
            out.println("nodeId=" + info.nodeId());
        }
        if (info.sequence() != null) {
            out.println("sequence=" + info.sequence());
        }
        if (info.businessType() != null) {
            out.println("businessType=" + info.businessType());
        }
    }

    private static String toJson(IdInfo info, String raw, String requestedType) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("type", info.type().value());
        json.put("raw", raw);
        if (info.timestamp() != null) {
            json.put("timestamp", info.timestamp());
        }
        if (info.version() != null) {
            json.put("version", info.version());
        }
        if (info.nodeId() != null) {
            json.put("nodeId", info.nodeId());
        }
        if (info.sequence() != null) {
            json.put("sequence", info.sequence());
        }
        if (info.businessType() != null) {
            json.put("businessType", info.businessType());
        }
        if (requestedType != null) {
            json.put("requestedType", IdType.of(requestedType).value());
        }
        return Json.object(json);
    }

    private static String format(String raw) {
        String normalized = raw.toLowerCase(Locale.ROOT);
        if (!"text".equals(normalized) && !"json".equals(normalized)) {
            throw usage("--format must be text or json");
        }
        return normalized;
    }

    private static String value(String[] args, int index, String option) {
        if (index >= args.length) {
            throw usage(option + " requires a value");
        }
        return args[index];
    }

    private static CliException usage(String message) {
        return new CliException(CliException.USAGE, message);
    }

    private static CliException failure(String message) {
        return new CliException(CliException.FAILURE, message);
    }

    static String help() {
        return """
                Usage: knitid parse <id> [options]

                Parse an identifier and print decoded metadata.

                Options:
                  --type <TYPE>       Force a type; otherwise auto-detect
                  --format <FORMAT>   text | json (default: text)
                  --help, -h          Show this help

                Supported in CLI v1:
                  UUID7, UUID8, ULID, KSUID, TYPEID, numeric KSID
                """;
    }
}
