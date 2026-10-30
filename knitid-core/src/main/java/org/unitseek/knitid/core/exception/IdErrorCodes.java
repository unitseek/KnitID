package org.unitseek.knitid.core.exception;

/**
 * Stable error codes. The values are part of the public contract.
 *
 * <p>Codes are grouped as configuration (1000), generation (2000), parsing
 * (3000) and registry (4000) failures.</p>
 *
 * @since 1.0.0
 */
public final class IdErrorCodes {

    /** Invalid or missing configuration. */
    public static final String CONFIG_INVALID = "KNITID-1001";
    /** Required generator/worker module is absent. */
    public static final String MODULE_MISSING = "KNITID-1002";
    /** Conflicting generators or configuration. */
    public static final String CONFIG_CONFLICT = "KNITID-1003";

    /** Generic identifier generation failure. */
    public static final String GENERATION_FAILED = "KNITID-2001";
    /** System clock moved backwards. */
    public static final String CLOCK_BACKWARDS = "KNITID-2002";
    /** WorkerId could not be acquired or renewed. */
    public static final String WORKER_UNAVAILABLE = "KNITID-2003";
    /** Sequence provider is unavailable. */
    public static final String SEQUENCE_UNAVAILABLE = "KNITID-2004";
    /** Sequence space is exhausted. */
    public static final String SEQUENCE_EXHAUSTED = "KNITID-2005";
    /** Generated business ID exceeds the template limit. */
    public static final String TEMPLATE_TOO_LONG = "KNITID-2006";
    /** WorkerId lease is no longer valid. */
    public static final String WORKER_LEASE_LOST = "KNITID-2007";

    /** Identifier could not be parsed. */
    public static final String PARSE_FAILED = "KNITID-3001";
    /** Custom identifier validation failed (reserved). */
    public static final String VALIDATION_FAILED = "KNITID-3002";

    /** Requested generator is not registered. */
    public static final String GENERATOR_NOT_FOUND = "KNITID-4001";

    private IdErrorCodes() {
    }
}
