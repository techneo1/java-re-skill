package com.srikanth.javareskill.repository;

import com.srikanth.javareskill.domain.Employee;
import com.srikanth.javareskill.domain.enums.EmployeeStatus;
import com.srikanth.javareskill.domain.enums.Role;
import com.srikanth.javareskill.store.EmployeeId;

import java.util.List;

/**
 * Employee-specific repository.
 *
 * <p>Extends {@link GenericRepository}{@code <Employee, EmployeeId>} with
 * HR-domain query methods that are not part of the generic contract.</p>
 *
 * <p>Demonstrates narrowing a generic interface to a concrete pair of type
 * arguments ({@code Employee} + {@code EmployeeId}), while adding
 * domain-specific finders.</p>
 */
public interface EmployeeRepository extends GenericRepository<Employee, EmployeeId> {

    /**
     * Returns all employees belonging to the given department.
     *
     * @param departmentId the department ID to filter by
     * @return unmodifiable list; empty if none match
     */
    List<Employee> findByDepartmentId(String departmentId);

    /**
     * Returns all employees whose {@link EmployeeStatus} matches.
     *
     * @param status must not be {@code null}
     * @return unmodifiable list; empty if none match
     */
    List<Employee> findByStatus(EmployeeStatus status);

    /**
     * Returns all employees whose {@link Role} matches.
     *
     * @param role must not be {@code null}
     * @return unmodifiable list; empty if none match
     */
    List<Employee> findByRole(Role role);
}
