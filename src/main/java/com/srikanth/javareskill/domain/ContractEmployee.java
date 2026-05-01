package com.srikanth.javareskill.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

/**
 * A contract (temporary / freelance) employee with a fixed term.
 *
 * <p>{@code final} — this is a leaf in the sealed hierarchy and cannot
 * be extended further.</p>
 *
 * <h2>Type-specific fields</h2>
 * <ul>
 *   <li>{@code contractEndDate} – the date on which the contract expires.</li>
 *   <li>{@code hourlyRate}      – billing rate per hour (used for annual
 *       compensation calculation).</li>
 *   <li>{@code contractedHoursPerYear} – total contracted hours in the year.</li>
 * </ul>
 */
public final class ContractEmployee extends EmployeeType {

    private final LocalDate contractEndDate;
    private final BigDecimal hourlyRate;
    private final int contractedHoursPerYear;

    /**
     * @param employee                the underlying employee; must not be {@code null}
     * @param contractEndDate         when the contract expires; must not be {@code null}
     * @param hourlyRate              billing rate per hour; must not be {@code null}, must be ≥ 0
     * @param contractedHoursPerYear  total hours contracted for the year; must be &gt; 0
     */
    public ContractEmployee(Employee employee,
                             LocalDate contractEndDate,
                             BigDecimal hourlyRate,
                             int contractedHoursPerYear) {
        super(employee);
        this.contractEndDate = Objects.requireNonNull(contractEndDate,
                "contractEndDate must not be null");
        this.hourlyRate = Objects.requireNonNull(hourlyRate,
                "hourlyRate must not be null");
        this.contractedHoursPerYear = contractedHoursPerYear;

        if (hourlyRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("hourlyRate must not be negative");
        }
        if (contractedHoursPerYear <= 0) {
            throw new IllegalArgumentException(
                    "contractedHoursPerYear must be > 0, got: " + contractedHoursPerYear);
        }
    }

    public LocalDate getContractEndDate()    { return contractEndDate; }
    public BigDecimal getHourlyRate()        { return hourlyRate; }
    public int getContractedHoursPerYear()   { return contractedHoursPerYear; }

    /**
     * Returns whether the contract has expired relative to the given date.
     *
     * @param asOf the reference date; must not be {@code null}
     * @return {@code true} if {@code contractEndDate} is before {@code asOf}
     */
    public boolean isExpired(LocalDate asOf) {
        Objects.requireNonNull(asOf, "asOf must not be null");
        return contractEndDate.isBefore(asOf);
    }

    /**
     * Annual compensation = hourlyRate × contractedHoursPerYear.
     *
     * <p>Result is rounded to 2 decimal places (HALF_UP).</p>
     */
    @Override
    public BigDecimal annualCompensation() {
        return hourlyRate.multiply(BigDecimal.valueOf(contractedHoursPerYear))
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String typeName() {
        return "Contract";
    }

    @Override
    public String toString() {
        return "ContractEmployee{employee=" + getEmployee()
                + ", contractEndDate=" + contractEndDate
                + ", hourlyRate=" + hourlyRate
                + ", contractedHoursPerYear=" + contractedHoursPerYear + "}";
    }
}

