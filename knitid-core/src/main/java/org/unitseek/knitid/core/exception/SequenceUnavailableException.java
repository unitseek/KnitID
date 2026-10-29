package org.unitseek.knitid.core.exception;

/**
 * Configured sequence provider is unavailable.
 */
public class SequenceUnavailableException extends IdGenerationException {

    public SequenceUnavailableException(String code, String message) {
        super(code, message);
    }

    public SequenceUnavailableException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
