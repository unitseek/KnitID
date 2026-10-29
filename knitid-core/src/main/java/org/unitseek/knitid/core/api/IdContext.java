package org.unitseek.knitid.core.api;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable per-call context.
 */
public final class IdContext {

    private static final IdContext EMPTY = builder().build();

    private final String businessType;
    private final String prefix;
    private final String tenantId;
    private final Long nodeId;
    private final LocalDate businessDate;
    private final Map<String, Object> attributes;

    private IdContext(Builder builder) {
        this.businessType = builder.businessType;
        this.prefix = builder.prefix;
        this.tenantId = builder.tenantId;
        this.nodeId = builder.nodeId;
        this.businessDate = builder.businessDate;
        this.attributes = builder.attributes == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(builder.attributes));
    }

    public static IdContext empty() {
        return EMPTY;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String businessType() {
        return businessType;
    }

    public String prefix() {
        return prefix;
    }

    public String tenantId() {
        return tenantId;
    }

    public Long nodeId() {
        return nodeId;
    }

    public LocalDate businessDate() {
        return businessDate;
    }

    public Map<String, Object> attributes() {
        return attributes;
    }

    public static final class Builder {

        private String businessType;
        private String prefix;
        private String tenantId;
        private Long nodeId;
        private LocalDate businessDate;
        private Map<String, Object> attributes;

        private Builder() {
        }

        public Builder businessType(String businessType) {
            this.businessType = businessType;
            return this;
        }

        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder nodeId(Long nodeId) {
            this.nodeId = nodeId;
            return this;
        }

        public Builder businessDate(LocalDate businessDate) {
            this.businessDate = businessDate;
            return this;
        }

        public Builder attributes(Map<String, Object> attributes) {
            this.attributes = attributes == null ? null : new LinkedHashMap<>(attributes);
            return this;
        }

        public IdContext build() {
            return new IdContext(this);
        }
    }
}
