package org.unitseek.knitid.core.exception;

/**
 * Base exception for all KnitID failures.
 */
public class IdException extends RuntimeException {

    private final String code;

    public IdException(String code, String message) {
        super("[" + code + "] " + message);
        this.code = code;
    }

    public IdException(String code, String message, Throwable cause) {
        super("[" + code + "] " + message, cause);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
