package org.unitseek.knitid.core.api;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable per-call context.
 *
 * <p>Fields are optional. Generators that do not use a field ignore it.
 * {@link #businessDateTime()} takes precedence over
 * {@link #businessDate()} when both are set.</p>
 *
 * @since 1.0.0
 */
public final class IdContext {

    private static final IdContext EMPTY = builder().build();

    private final String businessType;
    private final String prefix;
    private final String tenantId;
    private final Long nodeId;
    private final LocalDate businessDate;
    private final LocalDateTime businessDateTime;
    private final Map<String, Object> attributes;

    private IdContext(Builder builder) {
        this.businessType = builder.businessType;
        this.prefix = builder.prefix;
        this.tenantId = builder.tenantId;
        this.nodeId = builder.nodeId;
        this.businessDate = builder.businessDate;
        this.businessDateTime = builder.businessDateTime;
        this.attributes = builder.attributes == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(builder.attributes));
    }

    /**
     * Returns the shared empty context.
     *
     * @return empty context
     */
    public static IdContext empty() {
        return EMPTY;
    }

    /**
     * Creates a context builder.
     *
     * @return new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the optional business type.
     *
     * @return business type, or {@code null}
     */
    public String businessType() {
        return businessType;
    }

    /**
     * Returns the optional prefix override.
     *
     * @return prefix, or {@code null}
     */
    public String prefix() {
        return prefix;
    }

    /**
     * Returns the optional tenant id.
     *
     * @return tenant id, or {@code null}
     */
    public String tenantId() {
        return tenantId;
    }

    /**
     * Returns the optional node/worker id.
     *
     * @return node id, or {@code null}
     */
    public Long nodeId() {
        return nodeId;
    }

    /**
     * Returns the optional business date.
     *
     * @return business date, or {@code null}
     */
    public LocalDate businessDate() {
        return businessDate;
    }

    /**
     * Returns the optional business date-time.
     *
     * @return business date-time, or {@code null}
     */
    public LocalDateTime businessDateTime() {
        return businessDateTime;
    }

    /**
     * Returns the immutable attribute map.
     *
     * @return immutable attributes
     */
    public Map<String, Object> attributes() {
        return attributes;
    }

    /**
     * Mutable builder for {@link IdContext}.
     *
     * @since 1.0.0
     */
    public static final class Builder {

        private String businessType;
        private String prefix;
        private String tenantId;
        private Long nodeId;
        private LocalDate businessDate;
        private LocalDateTime businessDateTime;
        private Map<String, Object> attributes;

        private Builder() {
        }

        /**
         * Sets the business type.
         *
         * @param businessType business type
         * @return this builder
         */
        public Builder businessType(String businessType) {
            this.businessType = businessType;
            return this;
        }

        /**
         * Sets the prefix override.
         *
         * @param prefix prefix
         * @return this builder
         */
        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        /**
         * Sets the tenant id.
         *
         * @param tenantId tenant id
         * @return this builder
         */
        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        /**
         * Sets the node/worker id.
         *
         * @param nodeId node id
         * @return this builder
         */
        public Builder nodeId(Long nodeId) {
            this.nodeId = nodeId;
            return this;
        }

        /**
         * Sets the business date.
         *
         * @param businessDate business date
         * @return this builder
         */
        public Builder businessDate(LocalDate businessDate) {
            this.businessDate = businessDate;
            return this;
        }

        /**
         * Sets the business date-time; takes precedence over a business date.
         *
         * @param businessDateTime business date-time
         * @return this builder
         */
        public Builder businessDateTime(LocalDateTime businessDateTime) {
            this.businessDateTime = businessDateTime;
            return this;
        }

        /**
         * Replaces the attributes with a defensive copy.
         *
         * @param attributes attributes, or {@code null} for an empty map
         * @return this builder
         */
        public Builder attributes(Map<String, Object> attributes) {
            this.attributes = attributes == null ? null : new LinkedHashMap<>(attributes);
            return this;
        }

        /**
         * Builds an immutable context.
         *
         * @return immutable context
         */
        public IdContext build() {
            return new IdContext(this);
        }
    }
}
