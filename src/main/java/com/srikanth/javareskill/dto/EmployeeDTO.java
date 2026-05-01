package com.srikanth.javareskill.dto;

import com.srikanth.javareskill.domain.Employee;
import com.srikanth.javareskill.domain.enums.EmployeeStatus;
import com.srikanth.javareskill.domain.enums.Role;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Immutable data-transfer object for employee data.
 *
 * <p>Implemented as a Java {@code record} (JDK 16+), which auto-generates:
 * <ul>
 *   <li>A canonical constructor</li>
 *   <li>Accessor methods matching each component name (e.g. {@code id()}, {@code name()})</li>
 *   <li>{@code equals()}, {@code hashCode()}, and {@code toString()}</li>
 * </ul>
 *
 * <p>The compact constructor validates that all fields are non-null.</p>
 *
 * <h2>Records vs traditional classes</h2>
 * <p>The domain {@link Employee} class uses a Builder pattern with getters — appropriate
 * for a rich domain model with validation rules.  This record is a lightweight projection
 * designed for transferring data across layers (e.g. service → controller) without
 * carrying the full domain weight.</p>
 *
 * @param id           employee identifier
 * @param name         full name
 * @param email        e-mail address
 * @param departmentId department the employee belongs to
 * @param role         job role
 * @param salary       monthly salary
 * @param status       active / inactive
 * @param joiningDate  date the employee joined
 */
public record EmployeeDTO(
        String id,
        String name,
        String email,
        String departmentId,
        Role role,
        BigDecimal salary,
        EmployeeStatus status,
        LocalDate joiningDate
) {

    /**
     * Compact constructor — validates every component is non-null.
     */
    public EmployeeDTO {
        Objects.requireNonNull(id,           "id must not be null");
        Objects.requireNonNull(name,         "name must not be null");
        Objects.requireNonNull(email,        "email must not be null");
        Objects.requireNonNull(departmentId, "departmentId must not be null");
        Objects.requireNonNull(role,         "role must not be null");
        Objects.requireNonNull(salary,       "salary must not be null");
        Objects.requireNonNull(status,       "status must not be null");
        Objects.requireNonNull(joiningDate,  "joiningDate must not be null");
    }

    /**
     * Factory method that converts a domain {@link Employee} into an {@code EmployeeDTO}.
     *
     * @param employee the domain entity; must not be {@code null}
     * @return a new DTO mirroring the entity's fields
     */
    public static EmployeeDTO fromEntity(Employee employee) {
        Objects.requireNonNull(employee, "employee must not be null");
        return new EmployeeDTO(
                employee.getId(),
                employee.getName(),
                employee.getEmail(),
                employee.getDepartmentId(),
                employee.getRole(),
                employee.getSalary(),
                employee.getStatus(),
                employee.getJoiningDate()
        );
    }
}

