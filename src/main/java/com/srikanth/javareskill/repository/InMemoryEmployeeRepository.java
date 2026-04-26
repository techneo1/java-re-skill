package com.srikanth.javareskill.repository;

import com.srikanth.javareskill.domain.Employee;
import com.srikanth.javareskill.domain.enums.EmployeeStatus;
import com.srikanth.javareskill.domain.enums.Role;
import com.srikanth.javareskill.exception.EmployeeNotFoundException;
import com.srikanth.javareskill.store.EmployeeId;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * In-memory implementation of {@link EmployeeRepository}.
 *
 * <p>Extends {@link InMemoryRepository}{@code <Employee, EmployeeId>}, inheriting
 * all generic CRUD behaviour, and adds the three HR-specific query methods.</p>
 *
 * <h2>Generic wiring</h2>
 * <ul>
 *   <li><b>keyExtractor</b>   – {@code e -> new EmployeeId(e.getId())} converts the
 *       raw string ID stored on the entity into the typed {@link EmployeeId} key.</li>
 *   <li><b>notFoundFactory</b> – produces an {@link EmployeeNotFoundException} with
 *       the missing key's value so callers receive a meaningful, typed exception.</li>
 * </ul>
 */
public class InMemoryEmployeeRepository
        extends InMemoryRepository<Employee, EmployeeId>
        implements EmployeeRepository {

    public InMemoryEmployeeRepository() {
        super(
            /* keyExtractor    */ e  -> new EmployeeId(e.getId()),
            /* notFoundFactory */ id -> new EmployeeNotFoundException(id.getValue())
        );
    }

    @Override
    public List<Employee> findByDepartmentId(String departmentId) {
        Objects.requireNonNull(departmentId, "departmentId must not be null");
        return indexView().values().stream()
                .filter(e -> departmentId.equals(e.getDepartmentId()))
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public List<Employee> findByStatus(EmployeeStatus status) {
        Objects.requireNonNull(status, "status must not be null");
        return indexView().values().stream()
                .filter(e -> status == e.getStatus())
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public List<Employee> findByRole(Role role) {
        Objects.requireNonNull(role, "role must not be null");
        return indexView().values().stream()
                .filter(e -> role == e.getRole())
                .collect(Collectors.toUnmodifiableList());
    }
}
