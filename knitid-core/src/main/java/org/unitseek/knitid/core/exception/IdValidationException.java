package org.unitseek.knitid.core.exception;

/**
 * Custom identifier validation failed.
 */
public class IdValidationException extends IdException {

    public IdValidationException(String code, String message) {
        super(code, message);
    }

    public IdValidationException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
