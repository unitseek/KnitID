package org.unitseek.knitid.core.exception;

/**
 * Configuration or startup failure.
 */
public class IdConfigurationException extends IdException {

    public IdConfigurationException(String code, String message) {
        super(code, message);
    }

    public IdConfigurationException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
