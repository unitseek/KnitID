package org.unitseek.knitid.core.exception;

/**
 * Stable error codes. The values are part of the public contract.
 */
public final class IdErrorCodes {

    public static final String CONFIG_INVALID = "KNITID-1001";
    public static final String MODULE_MISSING = "KNITID-1002";
    public static final String CONFIG_CONFLICT = "KNITID-1003";

    public static final String GENERATION_FAILED = "KNITID-2001";
    public static final String CLOCK_BACKWARDS = "KNITID-2002";
    public static final String WORKER_UNAVAILABLE = "KNITID-2003";
    public static final String SEQUENCE_UNAVAILABLE = "KNITID-2004";
    public static final String SEQUENCE_EXHAUSTED = "KNITID-2005";
    public static final String TEMPLATE_TOO_LONG = "KNITID-2006";
    public static final String WORKER_LEASE_LOST = "KNITID-2007";

    public static final String PARSE_FAILED = "KNITID-3001";
    public static final String VALIDATION_FAILED = "KNITID-3002";

    public static final String GENERATOR_NOT_FOUND = "KNITID-4001";

    private IdErrorCodes() {
    }
}
