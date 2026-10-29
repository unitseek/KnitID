package org.unitseek.knitid.core.exception;

/**
 * Runtime identifier generation failure.
 */
public class IdGenerationException extends IdException {

    public IdGenerationException(String code, String message) {
        super(code, message);
    }

    public IdGenerationException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
