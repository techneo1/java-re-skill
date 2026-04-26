package com.srikanth.javareskill.service;

import java.math.BigDecimal;

/**
 * Contract for validating employee-related inputs before they are persisted.
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li><b>Email uniqueness</b> – ensures no two active employees share the same
 *       e-mail address.</li>
 *   <li><b>Salary range</b> – ensures a proposed salary falls within the
 *       configured minimum / maximum bounds.</li>
 *   <li><b>Department existence</b> – ensures a referenced department ID is
 *       present in the repository.</li>
 * </ul>
 *
 * <p>All methods throw a domain-specific {@link com.srikanth.javareskill.exception.HrException}
 * sub-type on failure, so callers need only wrap these calls in a single
 * {@code catch (HrException e)} block if they wish to handle all validation
 * errors uniformly.</p>
 */
public interface ValidationService {

    /**
     * Asserts that the given {@code email} is not already registered to any
     * existing employee.
     *
     * <p>When {@code excludeEmployeeId} is non-{@code null} the employee with
     * that ID is skipped during the uniqueness check — useful for update
     * operations where the employee is allowed to keep their own e-mail.</p>
     *
     * @param email              the e-mail address to check; must not be {@code null}
     * @param excludeEmployeeId  ID of an employee to exclude from the check, or
     *                           {@code null} to check all employees
     * @throws com.srikanth.javareskill.exception.DuplicateEmailException if the
     *         e-mail is already in use by another employee
     * @throws IllegalArgumentException if {@code email} is {@code null} or blank
     */
    void validateEmailUniqueness(String email, String excludeEmployeeId);

    /**
     * Convenience overload that checks e-mail uniqueness against <em>all</em>
     * employees (no exclusion).
     *
     * @param email the e-mail address to check; must not be {@code null}
     * @throws com.srikanth.javareskill.exception.DuplicateEmailException if the
     *         e-mail is already in use
     */
    default void validateEmailUniqueness(String email) {
        validateEmailUniqueness(email, null);
    }

    /**
     * Asserts that the given {@code salary} is within the allowed range
     * [{@code minSalary}, {@code maxSalary}].
     *
     * @param salary     the salary value to validate; must not be {@code null}
     * @param minSalary  the inclusive lower bound; must not be {@code null}
     * @param maxSalary  the inclusive upper bound; must not be {@code null}
     * @throws com.srikanth.javareskill.exception.InvalidSalaryException if
     *         {@code salary} is outside the allowed range or is negative
     * @throws IllegalArgumentException if any argument is {@code null}, or if
     *         {@code minSalary} &gt; {@code maxSalary}
     */
    void validateSalaryRange(BigDecimal salary, BigDecimal minSalary, BigDecimal maxSalary);

    /**
     * Asserts that a department with the given {@code departmentId} exists in
     * the repository.
     *
     * @param departmentId the department ID to verify; must not be {@code null}
     * @throws com.srikanth.javareskill.exception.DepartmentNotFoundException if
     *         no department with that ID can be found
     * @throws IllegalArgumentException if {@code departmentId} is {@code null}
     *         or blank
     */
    void validateDepartmentExists(String departmentId);
}

