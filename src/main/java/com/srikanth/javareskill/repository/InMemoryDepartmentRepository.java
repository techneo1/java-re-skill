package com.srikanth.javareskill.repository;

import com.srikanth.javareskill.domain.Department;
import com.srikanth.javareskill.exception.DepartmentNotFoundException;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * In-memory implementation of {@link DepartmentRepository}.
 *
 * <p>Extends {@link InMemoryRepository}{@code <Department, String>} — note the
 * plain {@code String} ID type, contrasting with {@link InMemoryEmployeeRepository}
 * which uses the custom {@code EmployeeId} key type.</p>
 *
 * <h2>Generic wiring</h2>
 * <ul>
 *   <li><b>keyExtractor</b>   – {@code Department::getId} (method reference, no wrapping needed)</li>
 *   <li><b>notFoundFactory</b> – produces a {@link DepartmentNotFoundException}</li>
 * </ul>
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
