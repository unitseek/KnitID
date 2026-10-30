package org.unitseek.knitid.core.exception;

/**
 * Base exception for all KnitID failures.
 *
 * @since 1.0.0
 */
public class IdException extends RuntimeException {

    private final String code;

    /**
     * Creates an exception with a stable error code.
     *
     * @param code stable error code
     * @param message detail message
     */
    public IdException(String code, String message) {
        super("[" + requireCode(code) + "] " + message);
        this.code = requireCode(code);
    }

    /**
     * Creates an exception with a stable error code and cause.
     *
     * @param code stable error code
     * @param message detail message
     * @param cause underlying cause
     */
    public IdException(String code, String message, Throwable cause) {
        super("[" + requireCode(code) + "] " + message, cause);
        this.code = requireCode(code);
    }

    private static String requireCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("error code must not be blank");
        }
        return code;
    }

    /**
     * Returns the stable error code.
     *
     * @return error code
     */
    public String code() {
        return code;
    }
}
