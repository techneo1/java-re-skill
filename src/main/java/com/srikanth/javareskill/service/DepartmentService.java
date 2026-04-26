package com.srikanth.javareskill.service;

import com.srikanth.javareskill.domain.Department;

import java.util.List;
import java.util.Optional;

/**
 * Service contract for department management.
 *
 * <p>Sits between the application layer and {@link com.srikanth.javareskill.repository.DepartmentRepository}.
 * Enforces business rules such as duplicate name validation.</p>
 */
public interface DepartmentService {

    /**
     * Creates a new department.
     *
     * @param department must not be {@code null}
     * @throws IllegalArgumentException if a department with the same ID already exists
     */
    void create(Department department);

    /**
     * Looks up a department by its ID.
     *
     * @return an {@link Optional} with the department, or empty if not found
     */
    Optional<Department> findById(String id);

    /** Returns all departments in insertion order. */
    List<Department> findAll();

    /**
     * Updates an existing department record.
     *
     * @throws com.srikanth.javareskill.exception.DepartmentNotFoundException if not found
     */
    void update(Department department);

    /**
     * Removes a department.
     *
     * @throws com.srikanth.javareskill.exception.DepartmentNotFoundException if not found
     */
    void delete(String id);

    /** Returns all departments located at the given location (case-insensitive). */
    List<Department> findByLocation(String location);

    /** Looks up a department by its name (case-insensitive). */
    Optional<Department> findByName(String name);

    /** Returns the total number of departments. */
    int count();
}

