package com.srikanth.javareskill.mapper;

import com.srikanth.javareskill.domain.Department;
import com.srikanth.javareskill.dto.DepartmentDTO;
import com.srikanth.javareskill.dto.EmployeeDTO;
import com.srikanth.javareskill.dto.request.CreateDepartmentRequest;
import com.srikanth.javareskill.dto.response.DepartmentWithEmployeesResponse;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Pure-static mapper between the {@link Department} domain entity and
 * the REST-layer DTOs.
 *
 * <p>See {@link EmployeeMapper} for the design rationale behind hand-written mappers.</p>
 */
public final class DepartmentMapper {

    private DepartmentMapper() { /* utility class */ }

    // ── Request → Domain ─────────────────────────────────────────────────────

    /**
     * Converts a {@link CreateDepartmentRequest} to a {@link Department}, generating a UUID.
     *
     * @param request validated request; must not be {@code null}
     * @return new domain entity ready to be persisted
     */
    public static Department toDomain(CreateDepartmentRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        return Department.builder()
                .id(UUID.randomUUID().toString())
                .name(request.getName())
                .location(request.getLocation())
                .build();
    }

    // ── Domain → Response DTO ─────────────────────────────────────────────────

    /**
     * Converts a {@link Department} to a {@link DepartmentDTO}.
     *
     * @param department must not be {@code null}
     * @return response DTO
     */
    public static DepartmentDTO toDto(Department department) {
        return DepartmentDTO.fromEntity(department);   // delegates to existing factory
    }

    /**
     * Builds a {@link DepartmentWithEmployeesResponse} from a department and the
     * pre-fetched list of its employees.
     *
     * <p>The caller is responsible for fetching the employee list via
     * {@code EmployeeService.findByDepartment(id)} before invoking this method.</p>
     *
     * @param department must not be {@code null}
     * @param employees  employees that belong to the department; may be empty
     * @return composite response
     */
    public static DepartmentWithEmployeesResponse toWithEmployees(
            Department department, List<EmployeeDTO> employees) {
        Objects.requireNonNull(department, "department must not be null");
        Objects.requireNonNull(employees,  "employees must not be null");
        return new DepartmentWithEmployeesResponse(toDto(department), employees);
    }
}

