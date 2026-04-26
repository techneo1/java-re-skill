package com.srikanth.javareskill.service.impl;

import com.srikanth.javareskill.exception.DepartmentNotFoundException;
import com.srikanth.javareskill.exception.DuplicateEmailException;
import com.srikanth.javareskill.exception.InvalidSalaryException;
import com.srikanth.javareskill.repository.DepartmentRepository;
import com.srikanth.javareskill.repository.EmployeeRepository;
import com.srikanth.javareskill.service.ValidationService;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Default implementation of {@link ValidationService}.
 *
 * <p>All three validation rules are implemented against the in-memory
 * repositories injected at construction time.  No Spring / CDI annotations
 * are used intentionally — the class is wired manually to keep the project
 * framework-free.</p>
 *
 * <h2>Thread safety</h2>
 * <p>This class is stateless beyond its immutable repository references.
 * Thread safety depends entirely on the thread safety of the supplied
 * repository implementations.</p>
 */
public final class ValidationServiceImpl implements ValidationService {

    private final EmployeeRepository   employeeRepository;
    private final DepartmentRepository departmentRepository;

    /**
     * Constructs a {@code ValidationServiceImpl} with the required repositories.
     *
     * @param employeeRepository   used for e-mail uniqueness checks; must not be {@code null}
     * @param departmentRepository used for department existence checks; must not be {@code null}
     */
    public ValidationServiceImpl(EmployeeRepository   employeeRepository,
                                  DepartmentRepository departmentRepository) {
        this.employeeRepository   = Objects.requireNonNull(employeeRepository,   "employeeRepository");
        this.departmentRepository = Objects.requireNonNull(departmentRepository, "departmentRepository");
    }

    // -------------------------------------------------------------------------
    // ValidationService implementation
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Scans all employees for a case-insensitive e-mail match, optionally
     * skipping the employee identified by {@code excludeEmployeeId}.</p>
     */
    @Override
    public void validateEmailUniqueness(String email, String excludeEmployeeId) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email must not be null or blank");
        }

        employeeRepository.findAll().stream()
                .filter(e -> !e.getId().equals(excludeEmployeeId))
                .filter(e -> e.getEmail().equalsIgnoreCase(email))
                .findFirst()
                .ifPresent(existing -> {
                    throw new DuplicateEmailException(email, existing.getId());
                });
    }

    /**
     * {@inheritDoc}
     *
     * <p>Validates that {@code salary} is non-negative and lies within
     * [{@code minSalary}, {@code maxSalary}] (both bounds inclusive).</p>
     */
    @Override
    public void validateSalaryRange(BigDecimal salary, BigDecimal minSalary, BigDecimal maxSalary) {
        Objects.requireNonNull(salary,    "salary must not be null");
        Objects.requireNonNull(minSalary, "minSalary must not be null");
        Objects.requireNonNull(maxSalary, "maxSalary must not be null");

        if (minSalary.compareTo(maxSalary) > 0) {
            throw new IllegalArgumentException(
                    "minSalary (" + minSalary + ") must not exceed maxSalary (" + maxSalary + ")");
        }

        if (salary.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidSalaryException(salary, "salary must not be negative");
        }

        if (salary.compareTo(minSalary) < 0) {
            throw new InvalidSalaryException(salary,
                    "salary " + salary + " is below the minimum allowed value of " + minSalary);
        }

        if (salary.compareTo(maxSalary) > 0) {
            throw new InvalidSalaryException(salary,
                    "salary " + salary + " exceeds the maximum allowed value of " + maxSalary);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to {@link DepartmentRepository#existsById(Object)}; throws
     * {@link DepartmentNotFoundException} when the department is absent.</p>
     */
    @Override
    public void validateDepartmentExists(String departmentId) {
        if (departmentId == null || departmentId.isBlank()) {
            throw new IllegalArgumentException("departmentId must not be null or blank");
        }

        if (!departmentRepository.existsById(departmentId)) {
            throw new DepartmentNotFoundException(departmentId);
        }
    }
}

