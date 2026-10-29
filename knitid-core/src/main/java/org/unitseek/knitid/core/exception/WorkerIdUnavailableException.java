package org.unitseek.knitid.core.exception;

/**
 * WorkerId could not be acquired, renewed or is out of range.
 */
public class WorkerIdUnavailableException extends IdException {

    public WorkerIdUnavailableException(String code, String message) {
        super(code, message);
    }

    public WorkerIdUnavailableException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
