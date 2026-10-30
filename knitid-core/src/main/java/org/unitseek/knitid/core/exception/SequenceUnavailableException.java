package org.unitseek.knitid.core.exception;

/**
 * Configured sequence provider is unavailable.
 *
 * @since 1.0.0
 */
public class SequenceUnavailableException extends IdGenerationException {

    /**
     * Creates an exception with a stable error code.
     *
     * @param code stable error code
     * @param message detail message
     */
    public SequenceUnavailableException(String code, String message) {
        super(code, message);
    }

    /**
     * Creates an exception with a stable error code and cause.
     *
     * @param code stable error code
     * @param message detail message
     * @param cause underlying cause
     */
    public SequenceUnavailableException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
