package com.srikanth.javareskill.store;

import com.srikanth.javareskill.domain.Employee;
import com.srikanth.javareskill.exception.EmployeeNotFoundException;

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

    /** Primary lookup structure: EmployeeId (custom key) → Employee */
    private final Map<EmployeeId, Employee> idIndex = new HashMap<>();

    /** Ordered list of employees (insertion order). */
    private final List<Employee> employeeList = new ArrayList<>();

    // -------------------------------------------------------------------------
    // EmployeeStore implementation
    // -------------------------------------------------------------------------

    @Override
    public void add(Employee employee) {
        Objects.requireNonNull(employee, "employee must not be null");
        EmployeeId key = new EmployeeId(employee.getId());
        if (idIndex.containsKey(key)) {
            throw new IllegalArgumentException(
                    "Employee with ID '" + employee.getId() + "' already exists");
        }
        idIndex.put(key, employee);
        employeeList.add(employee);
    }

    @Override
    public Optional<Employee> findById(EmployeeId id) {
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
        EmployeeId key = new EmployeeId(employee.getId());
        if (!idIndex.containsKey(key)) {
            throw new EmployeeNotFoundException(employee.getId());
        }
        // Replace in map
        idIndex.put(key, employee);
        // Replace in list (preserve position)
        int index = findListIndex(key);
        employeeList.set(index, employee);
    }

    @Override
    public void remove(EmployeeId id) {
        Objects.requireNonNull(id, "id must not be null");
        Employee removed = idIndex.remove(id);
        if (removed == null) {
            throw new EmployeeNotFoundException(id.getValue());
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

    private int findListIndex(EmployeeId key) {
        return java.util.stream.IntStream.range(0, employeeList.size())
                .filter(i -> new EmployeeId(employeeList.get(i).getId()).equals(key))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Store is in inconsistent state for ID: " + key));
    }
}

