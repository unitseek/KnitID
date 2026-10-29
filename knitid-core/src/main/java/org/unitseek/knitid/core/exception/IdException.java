package org.unitseek.knitid.core.exception;

/**
 * Base exception for all KnitID failures.
 */
public class IdException extends RuntimeException {

    private final String code;

    public IdException(String code, String message) {
        super("[" + requireCode(code) + "] " + message);
        this.code = requireCode(code);
    }

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

    public String code() {
        return code;
    }
}
