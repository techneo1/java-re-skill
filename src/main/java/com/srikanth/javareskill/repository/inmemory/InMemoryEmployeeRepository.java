package com.srikanth.javareskill.repository.inmemory;

import com.srikanth.javareskill.domain.Employee;
import com.srikanth.javareskill.domain.EmployeeId;
import com.srikanth.javareskill.domain.enums.EmployeeStatus;
import com.srikanth.javareskill.domain.enums.Role;
import com.srikanth.javareskill.exception.EmployeeNotFoundException;
import com.srikanth.javareskill.repository.EmployeeRepository;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * In-memory implementation of {@link EmployeeRepository}.
 *
 * <p>Extends {@link InMemoryRepository}{@code <Employee, EmployeeId>}, inheriting
 * all generic CRUD behaviour, and adds HR-specific query methods.</p>
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

