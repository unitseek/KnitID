package org.unitseek.knitid.core.exception;

/**
 * Requested generator does not exist.
 */
public class IdGeneratorNotFoundException extends IdException {

    public IdGeneratorNotFoundException(String code, String message) {
        super(code, message);
    }

    public IdGeneratorNotFoundException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
