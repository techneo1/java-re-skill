package com.srikanth.javareskill.dto.response;

import com.srikanth.javareskill.dto.DepartmentDTO;
import com.srikanth.javareskill.dto.EmployeeDTO;

import java.util.List;
import java.util.Objects;

/**
 * Response for {@code GET /departments/{id}}.
 *
 * <p>Extends the plain {@link DepartmentDTO} with a list of the employees
 * that belong to the department — a common pattern to avoid an extra
 * round-trip from the client.</p>
 *
 * @param department  the department data
 * @param employees   employees belonging to this department (may be empty, never null)
 */
public record DepartmentWithEmployeesResponse(
        DepartmentDTO department,
        List<EmployeeDTO> employees
) {
    /** Compact constructor — guards against null lists. */
    public DepartmentWithEmployeesResponse {
        Objects.requireNonNull(department, "department must not be null");
        employees = employees == null ? List.of() : List.copyOf(employees);
    }
}

