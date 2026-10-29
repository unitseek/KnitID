package org.unitseek.knitid.core.exception;

/**
 * Identifier could not be parsed.
 */
public class IdParseException extends IdException {

    public IdParseException(String code, String message) {
        super(code, message);
    }

    public IdParseException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
