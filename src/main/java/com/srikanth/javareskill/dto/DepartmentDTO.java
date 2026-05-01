package com.srikanth.javareskill.dto;

import com.srikanth.javareskill.domain.Department;

import java.util.Objects;

/**
 * Immutable data-transfer object for department data.
 *
 * <p>Implemented as a Java {@code record} — the compiler generates
 * {@code equals}, {@code hashCode}, {@code toString}, and accessor methods
 * automatically.</p>
 *
 * @param id       department identifier
 * @param name     department name
 * @param location office location
 */
public record DepartmentDTO(
        String id,
        String name,
        String location
) {

    /**
     * Compact constructor — validates every component is non-null.
     */
    public DepartmentDTO {
        Objects.requireNonNull(id,       "id must not be null");
        Objects.requireNonNull(name,     "name must not be null");
        Objects.requireNonNull(location, "location must not be null");
    }

    /**
     * Factory method that converts a domain {@link Department} into a {@code DepartmentDTO}.
     *
     * @param department the domain entity; must not be {@code null}
     * @return a new DTO mirroring the entity's fields
     */
    public static DepartmentDTO fromEntity(Department department) {
        Objects.requireNonNull(department, "department must not be null");
        return new DepartmentDTO(
                department.getId(),
                department.getName(),
                department.getLocation()
        );
    }
}

