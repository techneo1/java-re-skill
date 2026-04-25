package com.srikanth.javareskill.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a single payroll processing record for an employee.
 *
 * <p>Immutable after construction; use {@link Builder} to create instances.</p>
 *
 * <p>Invariant: {@code netSalary == grossSalary - taxAmount}</p>
 */
public final class PayrollRecord {

    private final String id;
    private final String employeeId;
    private final BigDecimal grossSalary;
    private final BigDecimal taxAmount;
    private final BigDecimal netSalary;
    private final LocalDate payrollMonth;        // year-month represented as first day of month
    private final LocalDateTime processedTimestamp;

    private PayrollRecord(Builder builder) {
        this.id                  = Objects.requireNonNull(builder.id,                 "id must not be null");
        this.employeeId          = Objects.requireNonNull(builder.employeeId,          "employeeId must not be null");
        this.grossSalary         = Objects.requireNonNull(builder.grossSalary,         "grossSalary must not be null");
        this.taxAmount           = Objects.requireNonNull(builder.taxAmount,           "taxAmount must not be null");
        this.payrollMonth        = Objects.requireNonNull(builder.payrollMonth,        "payrollMonth must not be null");
        this.processedTimestamp  = Objects.requireNonNull(builder.processedTimestamp,  "processedTimestamp must not be null");

        if (grossSalary.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("grossSalary must not be negative");
        }
        if (taxAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("taxAmount must not be negative");
        }
        if (taxAmount.compareTo(grossSalary) > 0) {
            throw new IllegalArgumentException("taxAmount cannot exceed grossSalary");
        }

        // netSalary is always derived — never caller-supplied to avoid inconsistency
        this.netSalary = grossSalary.subtract(taxAmount);
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public String getId()                            { return id; }
    public String getEmployeeId()                    { return employeeId; }
    public BigDecimal getGrossSalary()               { return grossSalary; }
    public BigDecimal getTaxAmount()                 { return taxAmount; }
    public BigDecimal getNetSalary()                 { return netSalary; }
    public LocalDate getPayrollMonth()               { return payrollMonth; }
    public LocalDateTime getProcessedTimestamp()     { return processedTimestamp; }

    // -------------------------------------------------------------------------
    // equals / hashCode / toString
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PayrollRecord)) return false;
        PayrollRecord p = (PayrollRecord) o;
        return Objects.equals(id, p.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "PayrollRecord{" +
                "id='" + id + '\'' +
                ", employeeId='" + employeeId + '\'' +
                ", grossSalary=" + grossSalary +
                ", taxAmount=" + taxAmount +
                ", netSalary=" + netSalary +
                ", payrollMonth=" + payrollMonth +
                ", processedTimestamp=" + processedTimestamp +
                '}';
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String id;
        private String employeeId;
        private BigDecimal grossSalary;
        private BigDecimal taxAmount;
        private LocalDate payrollMonth;
        private LocalDateTime processedTimestamp;

        private Builder() {}

        public Builder id(String id)                                       { this.id = id; return this; }
        public Builder employeeId(String employeeId)                       { this.employeeId = employeeId; return this; }
        public Builder grossSalary(BigDecimal grossSalary)                 { this.grossSalary = grossSalary; return this; }
        public Builder taxAmount(BigDecimal taxAmount)                     { this.taxAmount = taxAmount; return this; }
        public Builder payrollMonth(LocalDate payrollMonth)                { this.payrollMonth = payrollMonth; return this; }
        public Builder processedTimestamp(LocalDateTime processedTimestamp){ this.processedTimestamp = processedTimestamp; return this; }

        public PayrollRecord build() {
            return new PayrollRecord(this);
        }
    }
}

