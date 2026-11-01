package org.unitseek.knitid.cli;

import java.io.PrintStream;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.unitseek.knitid.core.api.IdGenerator;
import org.unitseek.knitid.core.api.IdType;
import org.unitseek.knitid.core.random.SecureRandomProvider;
import org.unitseek.knitid.generator.ksuid.KsuidGenerator;
import org.unitseek.knitid.generator.typeid.TypeIdConfig;
import org.unitseek.knitid.generator.typeid.TypeIdGenerator;
import org.unitseek.knitid.generator.ulid.UlidGenerator;
import org.unitseek.knitid.generator.uuid7.Uuid7Generator;
import org.unitseek.knitid.generator.uuid8.Uuid8Config;
import org.unitseek.knitid.generator.uuid8.Uuid8Generator;

/**
 * Implements {@code knitid gen}.
 *
 * @since 1.0.0
 */
final class GenCommand {

    private static final int MAX_COUNT = 10_000;

    private GenCommand() {
    }

    static int run(String[] args, PrintStream out) {
        String type = null;
        String prefix = null;
        String format = "text";
        int count = 1;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--type" -> type = value(args, ++i, arg);
                case "--count" -> count = parseCount(value(args, ++i, arg));
                case "--prefix" -> prefix = value(args, ++i, arg);
                case "--format" -> format = format(value(args, ++i, arg));
                case "--help", "-h" -> {
                    out.println(help());
                    return 0;
                }
                default -> throw usage("Unknown gen option: " + arg);
            }
        }
        if (type == null) {
            throw usage("gen requires --type");
        }
        IdGenerator generator = generator(type, prefix);
        List<String> ids = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ids.add(generator.generate());
        }
        if ("json".equals(format)) {
            out.println(array(ids, type));
        }
        else {
            ids.forEach(out::println);
        }
        return 0;
    }

    private static IdGenerator generator(String rawType, String prefix) {
        IdType type;
        try {
            type = IdType.of(rawType);
        }
        catch (IllegalArgumentException ex) {
            throw usage("Invalid type: " + rawType);
        }
        String normalized = type.value();
        if ("UUID7".equals(normalized)) {
            return new Uuid7Generator(Clock.systemUTC(), new SecureRandomProvider());
        }
        if ("UUID8".equals(normalized)) {
            return new Uuid8Generator(Clock.systemUTC(), Uuid8Config.defaults(), new SecureRandomProvider());
        }
        if ("ULID".equals(normalized)) {
            return new UlidGenerator(Clock.systemUTC(), new SecureRandomProvider());
        }
        if ("KSUID".equals(normalized)) {
            return new KsuidGenerator(Clock.systemUTC(), new SecureRandomProvider());
        }
        if ("TYPEID".equals(normalized)) {
            String idPrefix = prefix == null || prefix.isBlank() ? "knitid" : prefix;
            try {
                TypeIdConfig config = new TypeIdConfig(Set.of(idPrefix), idPrefix, false);
                return new TypeIdGenerator(Clock.systemUTC(), new SecureRandomProvider(), config);
            }
            catch (RuntimeException ex) {
                throw usage("Invalid TypeID prefix: " + idPrefix);
            }
        }
        throw usage("CLI gen supports UUID7, UUID8, ULID, KSUID, TYPEID; "
                + "use the Java/Spring API for " + normalized);
    }

    private static int parseCount(String raw) {
        try {
            int count = Integer.parseInt(raw);
            if (count < 1 || count > MAX_COUNT) {
                throw usage("--count must be between 1 and " + MAX_COUNT);
            }
            return count;
        }
        catch (NumberFormatException ex) {
            throw usage("--count must be an integer");
        }
    }

    private static String format(String raw) {
        String normalized = raw.toLowerCase(Locale.ROOT);
        if (!"text".equals(normalized) && !"json".equals(normalized)) {
            throw usage("--format must be text or json");
        }
        return normalized;
    }

    private static String array(List<String> ids, String type) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("type", IdType.of(type).value());
            item.put("id", ids.get(i));
            json.append(Json.object(item));
        }
        return json.append(']').toString();
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

    static String help() {
        return """
                Usage: knitid gen --type <TYPE> [options]

                Generate identifiers.

                Options:
                  --type <TYPE>       UUID7 | UUID8 | ULID | KSUID | TYPEID
                  --count <N>         Number of identifiers, 1..10000 (default: 1)
                  --prefix <PREFIX>   TypeID prefix (default: knitid)
                  --format <FORMAT>   text | json (default: text)
                  --help, -h          Show this help
                """;
    }
}
