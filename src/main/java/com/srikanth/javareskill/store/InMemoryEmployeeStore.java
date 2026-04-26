package com.srikanth.javareskill.store;

import com.srikanth.javareskill.domain.Employee;

import java.util.*;

/**
 * Thread-unsafe, in-memory implementation of {@link EmployeeStore}.
 *
 * <p>Internally maintains:
 * <ul>
 *   <li>A {@link HashMap} keyed by employee ID for O(1) look-ups.</li>
 *   <li>An {@link ArrayList} that preserves insertion order for listing.</li>
 * </ul>
 * Both collections are kept in sync on every mutating operation.</p>
 */
public class InMemoryEmployeeStore implements EmployeeStore {

    /** Primary lookup structure: ID → Employee */
    private final Map<String, Employee> idIndex = new HashMap<>();

    /** Ordered list of employees (insertion order). */
    private final List<Employee> employeeList = new ArrayList<>();

    // -------------------------------------------------------------------------
    // EmployeeStore implementation
    // -------------------------------------------------------------------------

    @Override
    public void add(Employee employee) {
        Objects.requireNonNull(employee, "employee must not be null");
        if (idIndex.containsKey(employee.getId())) {
            throw new IllegalArgumentException(
                    "Employee with ID '" + employee.getId() + "' already exists");
        }
        idIndex.put(employee.getId(), employee);
        employeeList.add(employee);
    }

    @Override
    public Optional<Employee> findById(String id) {
        Objects.requireNonNull(id, "id must not be null");
        return Optional.ofNullable(idIndex.get(id));
    }

    @Override
    public List<Employee> findAll() {
        return Collections.unmodifiableList(employeeList);
    }

    @Override
    public void update(Employee employee) {
        Objects.requireNonNull(employee, "employee must not be null");
        if (!idIndex.containsKey(employee.getId())) {
            throw new EmployeeNotFoundException(employee.getId());
        }
        // Replace in map
        idIndex.put(employee.getId(), employee);
        // Replace in list (preserve position)
        int index = findListIndex(employee.getId());
        employeeList.set(index, employee);
    }

    @Override
    public void remove(String id) {
        Objects.requireNonNull(id, "id must not be null");
        Employee removed = idIndex.remove(id);
        if (removed == null) {
            throw new EmployeeNotFoundException(id);
        }
        employeeList.remove(removed);
    }

    @Override
    public int size() {
        return idIndex.size();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private int findListIndex(String id) {
        for (int i = 0; i < employeeList.size(); i++) {
            if (employeeList.get(i).getId().equals(id)) {
                return i;
            }
        }
        // Should never reach here if idIndex and employeeList are in sync
        throw new IllegalStateException("Store is in inconsistent state for ID: " + id);
    }
}

