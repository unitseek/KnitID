package org.unitseek.knitid.cli;

/**
 * CLI usage or execution failure with a stable process exit code.
 *
 * @since 1.0.0
 */
final class CliException extends RuntimeException {

    static final int USAGE = 2;
    static final int FAILURE = 3;

    private final int exitCode;

    CliException(int exitCode, String message) {
        super(message);
        this.exitCode = exitCode;
    }

    int exitCode() {
        return exitCode;
    }
}
