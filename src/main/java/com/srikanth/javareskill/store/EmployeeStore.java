package com.srikanth.javareskill.store;

import com.srikanth.javareskill.domain.Employee;

import java.util.List;
import java.util.Optional;

/**
 * Contract for an in-memory Employee store.
 */
public interface EmployeeStore {

    /**
     * Adds a new employee to the store.
     *
     * @param employee the employee to add
     * @throws IllegalArgumentException if an employee with the same ID already exists
     */
    void add(Employee employee);

    /**
     * Looks up an employee by their unique ID.
     *
     * @param id the employee ID
     * @return an {@link Optional} containing the employee, or empty if not found
     */
    Optional<Employee> findById(String id);

    /**
     * Returns an unmodifiable snapshot of all employees in insertion order.
     *
     * @return list of all employees
     */
    List<Employee> findAll();

    /**
     * Updates an existing employee record (matched by ID).
     *
     * @param employee the updated employee
     * @throws EmployeeNotFoundException if no employee with that ID exists
     */
    void update(Employee employee);

    /**
     * Removes an employee from the store by ID.
     *
     * @param id the employee ID
     * @throws EmployeeNotFoundException if no employee with that ID exists
     */
    void remove(String id);

    /**
     * Returns the total number of employees in the store.
     *
     * @return store size
     */
    int size();
}

