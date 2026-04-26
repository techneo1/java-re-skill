package com.srikanth.javareskill.config;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Immutable, typed snapshot of the application configuration.
 *
 * <p>All fields are sourced from a {@code .properties} file loaded by
 * {@link ConfigLoader}.  Use {@link #builder()} to construct instances.</p>
 *
 * <h2>Config keys</h2>
 * <pre>
 * app.name            = string   (required)
 * app.max.employees   = int      (required, > 0)
 * app.default.salary  = decimal  (required, >= 0)
 * app.audit.enabled   = boolean  (optional, default false)
 * </pre>
 */
public final class AppConfig {

    private final String appName;
    private final int maxEmployees;
    private final BigDecimal defaultSalary;
    private final boolean auditEnabled;

    private AppConfig(Builder b) {
        this.appName       = Objects.requireNonNull(b.appName,       "appName");
        this.maxEmployees  = b.maxEmployees;
        this.defaultSalary = Objects.requireNonNull(b.defaultSalary, "defaultSalary");
        this.auditEnabled  = b.auditEnabled;

        if (maxEmployees <= 0) {
            throw new IllegalArgumentException("maxEmployees must be > 0, got: " + maxEmployees);
        }
        if (defaultSalary.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("defaultSalary must be >= 0");
        }
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public String     getAppName()       { return appName; }
    public int        getMaxEmployees()  { return maxEmployees; }
    public BigDecimal getDefaultSalary() { return defaultSalary; }
    public boolean    isAuditEnabled()   { return auditEnabled; }

    // -------------------------------------------------------------------------
    // toString
    // -------------------------------------------------------------------------

    @Override
    public String toString() {
        return "AppConfig{" +
                "appName='" + appName + '\'' +
                ", maxEmployees=" + maxEmployees +
                ", defaultSalary=" + defaultSalary +
                ", auditEnabled=" + auditEnabled +
                '}';
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String appName;
        private int maxEmployees;
        private BigDecimal defaultSalary;
        private boolean auditEnabled;

        private Builder() {}

        public Builder appName(String v)           { this.appName = v;       return this; }
        public Builder maxEmployees(int v)         { this.maxEmployees = v;  return this; }
        public Builder defaultSalary(BigDecimal v) { this.defaultSalary = v; return this; }
        public Builder auditEnabled(boolean v)     { this.auditEnabled = v;  return this; }

        public AppConfig build() { return new AppConfig(this); }
    }
}

