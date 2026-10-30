package org.unitseek.knitid.core.exception;

/**
 * Configuration or startup failure.
 *
 * @since 1.0.0
 */
public class IdConfigurationException extends IdException {

    /**
     * Creates an exception with a stable error code.
     *
     * @param code stable error code
     * @param message detail message
     */
    public IdConfigurationException(String code, String message) {
        super(code, message);
    }

    /**
     * Creates an exception with a stable error code and cause.
     *
     * @param code stable error code
     * @param message detail message
     * @param cause underlying cause
     */
    public IdConfigurationException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
