package org.unitseek.knitid.core.exception;

/**
 * Identifier could not be parsed.
 *
 * @since 1.0.0
 */
public class IdParseException extends IdException {

    /**
     * Creates an exception with a stable error code.
     *
     * @param code stable error code
     * @param message detail message
     */
    public IdParseException(String code, String message) {
        super(code, message);
    }

    /**
     * Creates an exception with a stable error code and cause.
     *
     * @param code stable error code
     * @param message detail message
     * @param cause underlying cause
     */
    public IdParseException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
