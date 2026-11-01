package org.unitseek.knitid.cli;

import java.io.PrintStream;

/**
 * Entry point for the KnitID command-line interface.
 *
 * @since 1.0.0
 */
public final class KnitIdCli {

    private static final String VERSION = "1.0.0";

    private KnitIdCli() {
    }

    /**
     * Runs the CLI.
     *
     * @param args command-line arguments
     * @param out standard output
     * @param err standard error
     * @return process exit code
     */
    public static int run(String[] args, PrintStream out, PrintStream err) {
        try {
            if (args.length == 0) {
                out.println(help());
                return 0;
            }
            String first = args[0];
            if ("--help".equals(first) || "-h".equals(first) || "help".equals(first)) {
                out.println(help());
                return 0;
            }
            if ("--version".equals(first) || "-V".equals(first) || "version".equals(first)) {
                out.println("knitid " + VERSION);
                return 0;
            }
            String[] rest = java.util.Arrays.copyOfRange(args, 1, args.length);
            return switch (first) {
                case "gen" -> GenCommand.run(rest, out);
                case "parse" -> ParseCommand.run(rest, out);
                default -> throw new CliException(CliException.USAGE, "Unknown command: " + first);
            };
        }
        catch (CliException ex) {
            err.println("knitid: " + ex.getMessage());
            if (ex.exitCode() == CliException.USAGE) {
                err.println("Try 'knitid --help' for usage.");
            }
            return ex.exitCode();
        }
        catch (RuntimeException ex) {
            err.println("knitid: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            return CliException.FAILURE;
        }
    }

    /**
     * Main entry point.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    static String help() {
        return """
                KnitID CLI 1.0.0

                Usage: knitid <command> [options]

                Commands:
                  gen       Generate identifiers
                  parse     Parse an identifier
                  version   Print version
                  help      Show this help

                Run 'knitid gen --help' or 'knitid parse --help' for details.
                """;
    }
}
