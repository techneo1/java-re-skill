package com.srikanth.javareskill.dto;

import com.srikanth.javareskill.domain.PayrollRecord;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Immutable data-transfer object summarising a payroll processing result.
 *
 * <p>Implemented as a Java {@code record}.  Unlike the domain
 * {@link PayrollRecord} (which derives {@code netSalary} internally), this
 * DTO carries the pre-computed net salary as a first-class component — ideal
 * for serialisation or display without re-calculation.</p>
 *
 * @param id                 payroll record identifier
 * @param employeeId         the employee this record belongs to
 * @param grossSalary        gross (pre-tax) salary
 * @param taxAmount          tax deducted
 * @param netSalary          net (post-tax) salary
 * @param payrollMonth       the month this payroll covers (first day of month)
 * @param processedTimestamp when the payroll was processed
 */
public record PayrollSummaryDTO(
        String id,
        String employeeId,
        BigDecimal grossSalary,
        BigDecimal taxAmount,
        BigDecimal netSalary,
        LocalDate payrollMonth,
        LocalDateTime processedTimestamp
) {

    /**
     * Compact constructor — validates every component is non-null and
     * enforces the invariant {@code netSalary == grossSalary − taxAmount}.
     */
    public PayrollSummaryDTO {
        Objects.requireNonNull(id,                 "id must not be null");
        Objects.requireNonNull(employeeId,         "employeeId must not be null");
        Objects.requireNonNull(grossSalary,        "grossSalary must not be null");
        Objects.requireNonNull(taxAmount,          "taxAmount must not be null");
        Objects.requireNonNull(netSalary,          "netSalary must not be null");
        Objects.requireNonNull(payrollMonth,       "payrollMonth must not be null");
        Objects.requireNonNull(processedTimestamp, "processedTimestamp must not be null");

        if (netSalary.compareTo(grossSalary.subtract(taxAmount)) != 0) {
            throw new IllegalArgumentException(
                    "netSalary must equal grossSalary - taxAmount; got net=" + netSalary
                            + ", gross=" + grossSalary + ", tax=" + taxAmount);
        }
    }

    /**
     * Factory method that converts a domain {@link PayrollRecord} into a
     * {@code PayrollSummaryDTO}.
     *
     * @param record the domain entity; must not be {@code null}
     * @return a new DTO mirroring the entity's fields
     */
    public static PayrollSummaryDTO fromEntity(PayrollRecord record) {
        Objects.requireNonNull(record, "record must not be null");
        return new PayrollSummaryDTO(
                record.getId(),
                record.getEmployeeId(),
                record.getGrossSalary(),
                record.getTaxAmount(),
                record.getNetSalary(),
                record.getPayrollMonth(),
                record.getProcessedTimestamp()
        );
    }
}

