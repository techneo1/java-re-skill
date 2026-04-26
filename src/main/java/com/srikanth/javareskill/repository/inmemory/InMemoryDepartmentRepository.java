package com.srikanth.javareskill.repository.inmemory;

import com.srikanth.javareskill.domain.Department;
import com.srikanth.javareskill.exception.DepartmentNotFoundException;
import com.srikanth.javareskill.repository.DepartmentRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * In-memory implementation of {@link DepartmentRepository}.
 *
 * <p>Uses plain {@code String} as the ID type (contrast with
 * {@link InMemoryEmployeeRepository} which uses the typed {@code EmployeeId} key).</p>
 */
public class InMemoryDepartmentRepository
        extends InMemoryRepository<Department, String>
        implements DepartmentRepository {

    public InMemoryDepartmentRepository() {
        super(
            /* keyExtractor    */ Department::getId,
            /* notFoundFactory */ id -> new DepartmentNotFoundException(id)
        );
    }

    @Override
    public List<Department> findByLocation(String location) {
        Objects.requireNonNull(location, "location must not be null");
        return indexView().values().stream()
                .filter(d -> location.equalsIgnoreCase(d.getLocation()))
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public Optional<Department> findByName(String name) {
        Objects.requireNonNull(name, "name must not be null");
        return indexView().values().stream()
                .filter(d -> name.equalsIgnoreCase(d.getName()))
                .findFirst();
    }
}

