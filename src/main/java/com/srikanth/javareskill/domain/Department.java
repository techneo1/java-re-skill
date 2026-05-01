package com.srikanth.javareskill.domain;

import java.util.Objects;

/**
 * Represents a department within the organisation.
 *
 * <p>Immutable after construction; use {@link Builder} to create instances.</p>
 */
public final class Department {

    private final String id;
    private final String name;
    private final String location;

    private Department(Builder builder) {
        this.id       = Objects.requireNonNull(builder.id,       "id must not be null");
        this.name     = Objects.requireNonNull(builder.name,     "name must not be null");
        this.location = Objects.requireNonNull(builder.location, "location must not be null");
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public String getId()       { return id; }
    public String getName()     { return name; }
    public String getLocation() { return location; }

    // -------------------------------------------------------------------------
    // equals / hashCode / toString
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Department)) return false;
        Department d = (Department) o;
        return Objects.equals(id, d.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return """
                Department{id='%s', name='%s', location='%s'}\
                """.formatted(id, name, location);
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String id;
        private String name;
        private String location;

        private Builder() {}

        public Builder id(String id)             { this.id = id; return this; }
        public Builder name(String name)         { this.name = name; return this; }
        public Builder location(String location) { this.location = location; return this; }

        public Department build() {
            return new Department(this);
        }
    }
}

