package org.unitseek.knitid.core.api;

import java.util.Objects;

/**
 * Identifier type value object.
 *
 * <p>Built-in types are {@link #UUID7}, {@link #UUID8}, {@link #ULID},
 * {@link #KSID}, {@link #BIZ}, {@link #KSUID} and {@link #TYPEID}. Custom
 * types can be created with {@link #of(String)} so that third-party
 * generators can be registered.</p>
 */
public final class IdType {

    public static final IdType UUID7 = new IdType("UUID7");
    public static final IdType UUID8 = new IdType("UUID8");
    public static final IdType ULID = new IdType("ULID");
    public static final IdType KSID = new IdType("KSID");
    public static final IdType BIZ = new IdType("BIZ");
    public static final IdType KSUID = new IdType("KSUID");
    public static final IdType TYPEID = new IdType("TYPEID");

    private static final int MAX_LENGTH = 64;

    private final String value;

    private IdType(String value) {
        this.value = value;
    }

    public static IdType of(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("idType must not be null");
        }
        return new IdType(normalize(raw));
    }

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
