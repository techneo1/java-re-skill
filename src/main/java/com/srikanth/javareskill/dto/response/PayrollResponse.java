package com.srikanth.javareskill.dto.response;

import com.srikanth.javareskill.dto.PayrollSummaryDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Response for {@code POST /payroll/process} and {@code GET /payroll}.
 *
 * <p>Wraps a list of {@link PayrollSummaryDTO} records with aggregate
 * statistics so the caller can inspect totals without iterating
 * the entire list.</p>
 *
 * @param month          the payroll month (first day of the month)
 * @param records        individual payroll records
 * @param totalProcessed number of employees successfully processed
 * @param totalGross     sum of all gross salaries
 * @param totalTax       sum of all tax deductions
 * @param totalNet       sum of all net salaries
 */
public record PayrollResponse(
        LocalDate month,
        List<PayrollSummaryDTO> records,
        int totalProcessed,
        BigDecimal totalGross,
        BigDecimal totalTax,
        BigDecimal totalNet
) {
    /** Compact constructor — guards against nulls and defensive-copies the list. */
    public PayrollResponse {
        Objects.requireNonNull(month,        "month must not be null");
        Objects.requireNonNull(totalGross,   "totalGross must not be null");
        Objects.requireNonNull(totalTax,     "totalTax must not be null");
        Objects.requireNonNull(totalNet,     "totalNet must not be null");
        records = records == null ? List.of() : List.copyOf(records);
    }

    /**
     * Convenience factory: builds a {@code PayrollResponse} by computing
     * aggregates from the supplied record list.
     *
     * @param month   payroll month
     * @param records individual records (may be empty)
     * @return fully populated response
     */
    public static PayrollResponse of(LocalDate month, List<PayrollSummaryDTO> records) {
        Objects.requireNonNull(month,   "month must not be null");
        Objects.requireNonNull(records, "records must not be null");

        BigDecimal totalGross = records.stream()
                .map(PayrollSummaryDTO::grossSalary)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalTax = records.stream()
                .map(PayrollSummaryDTO::taxAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalNet = records.stream()
                .map(PayrollSummaryDTO::netSalary)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new PayrollResponse(month, records, records.size(), totalGross, totalTax, totalNet);
    }
}

