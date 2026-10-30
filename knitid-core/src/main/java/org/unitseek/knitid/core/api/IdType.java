package org.unitseek.knitid.core.api;

import java.util.Objects;

/**
 * Identifier type value object.
 *
 * <p>Built-in types are {@link #UUID7}, {@link #UUID8}, {@link #ULID},
 * {@link #KSID}, {@link #BIZ}, {@link #KSUID} and {@link #TYPEID}. Custom
 * types can be created with {@link #of(String)} so that third-party
 * generators can be registered.</p>
 *
 * @since 1.0.0
 */
public final class IdType {

    /** RFC 9562 UUIDv7. */
    public static final IdType UUID7 = new IdType("UUID7");
    /** RFC 9562 UUIDv8 with KnitID presets. */
    public static final IdType UUID8 = new IdType("UUID8");
    /** ULID. */
    public static final IdType ULID = new IdType("ULID");
    /** KnitID Snowflake-style numeric ID. */
    public static final IdType KSID = new IdType("KSID");
    /** Template-driven business identifier. */
    public static final IdType BIZ = new IdType("BIZ");

    /**
     * KSUID (K-Sortable Unique ID); provided by the optional
     * {@code knitid-generator-ksuid} module.
     *
     * @since 1.0.0
     */
    public static final IdType KSUID = new IdType("KSUID");

    /**
     * TypeID (type-prefixed UUIDv7); provided by the optional
     * {@code knitid-generator-typeid} module.
     *
     * @since 1.0.0
     */
    public static final IdType TYPEID = new IdType("TYPEID");

    private static final int MAX_LENGTH = 64;

    private final String value;

    private IdType(String value) {
        this.value = value;
    }

    /**
     * Creates a normalized identifier type.
     *
     * <p>Input is trimmed, and {@code -}, {@code _} and whitespace are removed;
     * remaining characters are upper-cased. The normalized value must be
     * 1-64 alphanumeric characters.</p>
     *
     * @param raw type name
     * @return normalized identifier type
     * @throws IllegalArgumentException if the value is null or invalid
     */
    public static IdType of(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("idType must not be null");
        }
        return new IdType(normalize(raw));
    }

    /**
     * Tests whether a raw value can be normalized to a valid type.
     *
     * @param raw candidate value
     * @return {@code true} if {@link #of(String)} would accept the value
     */
    public static boolean isValid(String raw) {
        if (raw == null) {
            return false;
        }
        try {
            normalize(raw);
            return true;
        }
        catch (IllegalArgumentException ex) {
            return false;
        }
    }

    // Normalize by trimming, removing separators and upper-casing ASCII.
    private static String normalize(String raw) {
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("idType must not be empty");
        }
        StringBuilder builder = new StringBuilder(trimmed.length());
        for (int i = 0; i < trimmed.length(); i++) {
            char ch = trimmed.charAt(i);
            if (ch == '-' || ch == '_' || Character.isWhitespace(ch)) {
                continue;
            }
            char upper = Character.toUpperCase(ch);
            boolean alphanumeric = (upper >= 'A' && upper <= 'Z') || (upper >= '0' && upper <= '9');
            if (!alphanumeric) {
                throw new IllegalArgumentException("invalid idType: " + raw);
            }
            builder.append(upper);
        }
        if (builder.length() == 0 || builder.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("idType length must be between 1 and " + MAX_LENGTH + ": " + raw);
        }
        return builder.toString();
    }

    /**
     * Returns the normalized type name.
     *
     * @return normalized type name
     */
    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof IdType idType)) {
            return false;
        }
        return value.equals(idType.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
