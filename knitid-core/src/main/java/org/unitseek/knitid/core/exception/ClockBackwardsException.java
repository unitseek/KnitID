package org.unitseek.knitid.core.exception;

/**
 * System clock moved backwards and the configured strategy rejects the gap.
 *
 * @since 1.0.0
 */
public class ClockBackwardsException extends IdGenerationException {

    /**
     * Creates an exception with a stable error code.
     *
     * @param code stable error code
     * @param message detail message
     */
    public ClockBackwardsException(String code, String message) {
        super(code, message);
    }

    /**
     * Creates an exception with a stable error code and cause.
     *
     * @param code stable error code
     * @param message detail message
     * @param cause underlying cause
     */
    public ClockBackwardsException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
