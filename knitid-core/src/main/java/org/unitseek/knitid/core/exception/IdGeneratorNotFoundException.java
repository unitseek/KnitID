package org.unitseek.knitid.core.exception;

/**
 * Requested generator does not exist.
 *
 * @since 1.0.0
 */
public class IdGeneratorNotFoundException extends IdException {

    /**
     * Creates an exception with a stable error code.
     *
     * @param code stable error code
     * @param message detail message
     */
    public IdGeneratorNotFoundException(String code, String message) {
        super(code, message);
    }

    /**
     * Creates an exception with a stable error code and cause.
     *
     * @param code stable error code
     * @param message detail message
     * @param cause underlying cause
     */
    public IdGeneratorNotFoundException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
