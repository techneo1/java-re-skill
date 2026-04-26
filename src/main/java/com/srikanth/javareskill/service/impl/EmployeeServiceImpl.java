package com.srikanth.javareskill.service.impl;

import com.srikanth.javareskill.domain.Employee;
import com.srikanth.javareskill.domain.EmployeeId;
import com.srikanth.javareskill.domain.enums.EmployeeStatus;
import com.srikanth.javareskill.domain.enums.Role;
import com.srikanth.javareskill.exception.DuplicateEmailException;
import com.srikanth.javareskill.repository.EmployeeRepository;
import com.srikanth.javareskill.service.EmployeeService;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Default implementation of {@link EmployeeService}.
 *
 * <p>Delegates persistence to an {@link EmployeeRepository} and enforces
 * business rules (e.g. unique e-mail) before each write operation.</p>
 */
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository repository;

    public EmployeeServiceImpl(EmployeeRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public void hire(Employee employee) {
        Objects.requireNonNull(employee, "employee must not be null");

        // Business rule: e-mail must be unique across all employees
        repository.findAll().stream()
                .filter(e -> e.getEmail().equalsIgnoreCase(employee.getEmail()))
                .findFirst()
                .ifPresent(existing -> {
                    throw new DuplicateEmailException(employee.getEmail(), existing.getId());
                });

        repository.save(employee);
    }

    @Override
    public Optional<Employee> findById(EmployeeId id) {
        return repository.findById(id);
    }

    @Override
    public List<Employee> findAll() {
        return repository.findAll();
    }

    @Override
    public void update(Employee employee) {
        repository.update(employee);
    }

    @Override
    public void terminate(EmployeeId id) {
        repository.deleteById(id);
    }

    @Override
    public List<Employee> findByDepartment(String departmentId) {
        return repository.findByDepartmentId(departmentId);
    }

    @Override
    public List<Employee> findByStatus(EmployeeStatus status) {
        return repository.findByStatus(status);
    }

    @Override
    public List<Employee> findByRole(Role role) {
        return repository.findByRole(role);
    }

    @Override
    public int headcount() {
        return repository.count();
    }
}

