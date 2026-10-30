package org.unitseek.knitid.core.api;

/**
 * Framework-neutral health state for a generator.
 *
 * @since 1.0.0
 */
public enum HealthState {

    /** Generator is ready. */
    UP,

    /** Generator is unavailable or has lost a required lease. */
    DOWN,

    /** Health cannot be determined or the generator is closed. */
    UNKNOWN
}
