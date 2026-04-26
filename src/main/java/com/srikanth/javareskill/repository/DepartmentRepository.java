package com.srikanth.javareskill.repository;

import com.srikanth.javareskill.domain.Department;

import java.util.List;

/**
 * Department-specific repository.
 *
 * <p>Extends {@link GenericRepository}{@code <Department, String>} — deliberately
 * using plain {@code String} as the ID type to contrast with {@code EmployeeId},
 * demonstrating that the generic interface works with any ID type.</p>
 */
public interface DepartmentRepository extends GenericRepository<Department, String> {

    /**
     * Returns all departments located at the given location (case-insensitive).
     *
     * @param location must not be {@code null}
     * @return unmodifiable list; empty if none match
     */
    List<Department> findByLocation(String location);

    /**
     * Looks up a department by its exact name (case-insensitive).
     *
     * @param name must not be {@code null}
     * @return the matching department, or {@code null} if absent
     */
    java.util.Optional<Department> findByName(String name);
}

