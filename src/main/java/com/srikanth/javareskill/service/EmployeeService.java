package com.srikanth.javareskill.service;

import com.srikanth.javareskill.domain.Employee;
import com.srikanth.javareskill.domain.EmployeeId;
import com.srikanth.javareskill.domain.enums.EmployeeStatus;
import com.srikanth.javareskill.domain.enums.Role;

import java.util.List;
import java.util.Optional;

/**
 * Service contract for employee management.
 *
 * <p>Sits between the web/application layer and the repository layer.
 * Responsible for business validation (e.g. duplicate e-mail checks,
 * salary rules) before delegating to the repository.</p>
 */
public interface EmployeeService {

    /**
     * Registers a new employee.
     *
     * @param employee the employee to register; must not be {@code null}
     * @throws com.srikanth.javareskill.exception.DuplicateEmailException if the
     *         e-mail address is already used by another employee
     * @throws IllegalArgumentException if an employee with the same ID already exists
     */
    void hire(Employee employee);

    /**
     * Looks up an employee by ID.
     *
     * @param id employee ID; must not be {@code null}
     * @return an {@link Optional} with the employee, or empty if not found
     */
    Optional<Employee> findById(EmployeeId id);

    /**
     * Returns all employees in insertion order.
     */
    List<Employee> findAll();

    /**
     * Updates an existing employee record.
     *
     * @throws com.srikanth.javareskill.exception.EmployeeNotFoundException if not found
     */
    void update(Employee employee);

    /**
     * Terminates (removes) an employee.
     *
     * @throws com.srikanth.javareskill.exception.EmployeeNotFoundException if not found
     */
    void terminate(EmployeeId id);

    /** Returns all employees belonging to a specific department. */
    List<Employee> findByDepartment(String departmentId);

    /** Returns all employees with the given status. */
    List<Employee> findByStatus(EmployeeStatus status);

    /** Returns all employees with the given role. */
    List<Employee> findByRole(Role role);

    /** Returns the total number of employees. */
    int headcount();
}

