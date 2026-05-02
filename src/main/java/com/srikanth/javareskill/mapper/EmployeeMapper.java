package com.srikanth.javareskill.mapper;

import com.srikanth.javareskill.domain.Employee;
import com.srikanth.javareskill.domain.enums.EmployeeStatus;
import com.srikanth.javareskill.dto.EmployeeDTO;
import com.srikanth.javareskill.dto.request.CreateEmployeeRequest;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Pure-static mapper between the {@link Employee} domain entity and
 * the various DTOs used by the REST layer.
 *
 * <h2>Why not MapStruct / ModelMapper?</h2>
 * <p>This project deliberately avoids annotation-processor or reflection-based
 * mapping libraries so that:</p>
 * <ul>
 *   <li>Mapping logic is explicit, readable, and easy to step through in a debugger.</li>
 *   <li>No extra compile-time annotation-processor configuration is needed.</li>
 *   <li>The generated code path is 100 % testable without a Spring context.</li>
 * </ul>
 *
 * <p>All methods are {@code static} — no state, no instantiation needed.</p>
 */
public final class EmployeeMapper {

    private EmployeeMapper() { /* utility class */ }

    // ── Request → Domain ─────────────────────────────────────────────────────

    /**
     * Converts a {@link CreateEmployeeRequest} to an {@link Employee} domain entity,
     * auto-generating a UUID for the employee ID.
     *
     * @param request validated request body; must not be {@code null}
     * @return a new {@link Employee} ready to be persisted
     */
    public static Employee toDomain(CreateEmployeeRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        return Employee.builder()
                .id(UUID.randomUUID().toString())
                .name(request.getName())
                .email(request.getEmail())
                .departmentId(request.getDepartmentId())
                .role(request.getRole())
                .salary(request.getSalary())
                .status(request.getStatus() != null ? request.getStatus() : EmployeeStatus.ACTIVE)
                .joiningDate(request.getJoiningDate() != null
                        ? request.getJoiningDate()
                        : java.time.LocalDate.now())
                .build();
    }

    // ── Domain → Response DTO ─────────────────────────────────────────────────

    /**
     * Converts a domain {@link Employee} to an {@link EmployeeDTO}.
     *
     * @param employee must not be {@code null}
     * @return response DTO
     */
    public static EmployeeDTO toDto(Employee employee) {
        return EmployeeDTO.fromEntity(employee);     // delegates to the existing fromEntity factory
    }

    /**
     * Converts a list of domain {@link Employee} objects to a list of {@link EmployeeDTO}.
     *
     * <p>Uses a stream so filtering / mapping can be composed at the call site.</p>
     *
     * @param employees must not be {@code null}
     * @return unmodifiable list of DTOs
     */
    public static List<EmployeeDTO> toDtoList(List<Employee> employees) {
        Objects.requireNonNull(employees, "employees must not be null");
        return employees.stream()
                .map(EmployeeMapper::toDto)
                .toList();
    }

    // ── Domain → Updated Domain ───────────────────────────────────────────────

    /**
     * Returns a new {@link Employee} that is identical to {@code existing} except
     * that the status has been changed to {@code newStatus}.
     *
     * <p>Because {@link Employee} is immutable, "updating" it means re-building
     * from scratch with all original fields preserved except the changed one.</p>
     *
     * @param existing  the current entity; must not be {@code null}
     * @param newStatus the new status to apply; must not be {@code null}
     * @return a new {@link Employee} instance with the updated status
     */
    public static Employee withStatus(Employee existing, com.srikanth.javareskill.domain.enums.EmployeeStatus newStatus) {
        Objects.requireNonNull(existing,   "existing must not be null");
        Objects.requireNonNull(newStatus,  "newStatus must not be null");
        return Employee.builder()
                .id(existing.getId())
                .name(existing.getName())
                .email(existing.getEmail())
                .departmentId(existing.getDepartmentId())
                .role(existing.getRole())
                .salary(existing.getSalary())
                .status(newStatus)
                .joiningDate(existing.getJoiningDate())
                .build();
    }
}

