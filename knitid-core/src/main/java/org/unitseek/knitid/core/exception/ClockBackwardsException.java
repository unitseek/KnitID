package org.unitseek.knitid.core.exception;

/**
 * System clock moved backwards and the configured strategy rejects the gap.
 */
public class ClockBackwardsException extends IdGenerationException {

    public ClockBackwardsException(String code, String message) {
        super(code, message);
    }

    public ClockBackwardsException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
