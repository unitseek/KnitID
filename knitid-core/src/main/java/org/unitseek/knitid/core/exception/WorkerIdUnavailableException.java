package org.unitseek.knitid.core.exception;

/**
 * WorkerId could not be acquired, renewed or is out of range.
 *
 * @since 1.0.0
 */
public class WorkerIdUnavailableException extends IdException {

    /**
     * Creates an exception with a stable error code.
     *
     * @param code stable error code
     * @param message detail message
     */
    public WorkerIdUnavailableException(String code, String message) {
        super(code, message);
    }

    /**
     * Creates an exception with a stable error code and cause.
     *
     * @param code stable error code
     * @param message detail message
     * @param cause underlying cause
     */
    public WorkerIdUnavailableException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
