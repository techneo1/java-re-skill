package com.srikanth.javareskill.mapper;

import com.srikanth.javareskill.domain.PayrollRecord;
import com.srikanth.javareskill.dto.PayrollSummaryDTO;
import com.srikanth.javareskill.dto.response.PayrollResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Pure-static mapper between {@link PayrollRecord} domain entities and the
 * REST-layer payroll DTOs.
 *
 * <p>See {@link EmployeeMapper} for the design rationale behind hand-written mappers.</p>
 */
public final class PayrollMapper {

    private PayrollMapper() { /* utility class */ }

    // ── Domain → DTO ─────────────────────────────────────────────────────────

    /**
     * Converts a single {@link PayrollRecord} to a {@link PayrollSummaryDTO}.
     *
     * @param record must not be {@code null}
     * @return summary DTO
     */
    public static PayrollSummaryDTO toDto(PayrollRecord record) {
        return PayrollSummaryDTO.fromEntity(record);   // delegates to existing factory
    }

    /**
     * Converts a list of {@link PayrollRecord} domain objects to a list of
     * {@link PayrollSummaryDTO}.
     *
     * @param records must not be {@code null}
     * @return unmodifiable list of DTOs
     */
    public static List<PayrollSummaryDTO> toDtoList(List<PayrollRecord> records) {
        Objects.requireNonNull(records, "records must not be null");
        return records.stream()
                .map(PayrollMapper::toDto)
                .toList();
    }

    // ── DTO → Response ───────────────────────────────────────────────────────

    /**
     * Builds a {@link PayrollResponse} from a list of {@link PayrollRecord} domain entities
     * and the payroll month, computing aggregate totals automatically.
     *
     * @param month   the payroll month (first day of month)
     * @param records domain records to aggregate; may be empty
     * @return fully populated payroll response
     */
    public static PayrollResponse toResponse(LocalDate month, List<PayrollRecord> records) {
        Objects.requireNonNull(month,   "month must not be null");
        Objects.requireNonNull(records, "records must not be null");
        List<PayrollSummaryDTO> dtos = toDtoList(records);
        return PayrollResponse.of(month, dtos);
    }
}

