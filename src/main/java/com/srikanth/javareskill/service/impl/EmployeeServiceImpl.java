package com.srikanth.javareskill.service.impl;

import com.srikanth.javareskill.domain.Employee;
import com.srikanth.javareskill.domain.EmployeeId;
import com.srikanth.javareskill.domain.enums.EmployeeStatus;
import com.srikanth.javareskill.domain.enums.Role;
import com.srikanth.javareskill.exception.DuplicateEmailException;
import com.srikanth.javareskill.repository.EmployeeRepository;
import com.srikanth.javareskill.service.EmployeeService;
import com.srikanth.javareskill.service.ValidationService;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Default implementation of {@link EmployeeService}.
 *
 * <p>Delegates persistence to an {@link EmployeeRepository} and enforces
 * business rules (e.g. unique e-mail) before each write operation.</p>
 *
 * <h2>SOLID principles applied</h2>
 * <ul>
 *   <li><b>S – Single Responsibility</b>: This class is responsible only for
 *       employee lifecycle management (hire, find, update, terminate). Validation
 *       logic is separated into {@link ValidationService} — a distinct concern.
 *   </li>
 *   <li><b>D – Dependency Inversion</b>: Both {@link EmployeeRepository} and
 *       {@link ValidationService} are injected as interfaces at construction time.
 *       This class depends on abstractions, not concrete implementations.
 *   </li>
 * </ul>
 */
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository repository;

    /**
     * Optional validation service injected for business-rule checks.
     * May be {@code null} when the single-arg constructor is used (backward-compat).
     */
    private final ValidationService validationService;

    /**
     * Backward-compatible constructor; performs inline e-mail uniqueness check.
     *
     * <p>Prefer {@link #EmployeeServiceImpl(EmployeeRepository, ValidationService)}
     * for full Dependency Inversion compliance.</p>
     */
    public EmployeeServiceImpl(EmployeeRepository repository) {
        this(repository, null);
    }

    /**
     * Full constructor that accepts a {@link ValidationService} for
     * proper Dependency Inversion (DIP).
     *
     * @param repository        employee repository; must not be {@code null}
     * @param validationService validation delegate; may be {@code null} to fall
     *                          back to inline uniqueness check
     */
    public EmployeeServiceImpl(EmployeeRepository repository, ValidationService validationService) {
        this.repository        = Objects.requireNonNull(repository, "repository must not be null");
        this.validationService = validationService; // nullable – backward-compat fallback
    }

    @Override
    public void hire(Employee employee) {
        Objects.requireNonNull(employee, "employee must not be null");

        // SRP: delegate e-mail uniqueness check to ValidationService when available.
        // DIP: depend on the ValidationService abstraction, not a concrete class.
        if (validationService != null) {
            validationService.validateEmailUniqueness(employee.getEmail());
        } else {
            // Inline fallback for backward compatibility (single-arg constructor path).
            repository.findAll().stream()
                    .filter(e -> e.getEmail().equalsIgnoreCase(employee.getEmail()))
                    .findFirst()
                    .ifPresent(existing -> {
                        throw new DuplicateEmailException(employee.getEmail(), existing.getId());
                    });
        }

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

