package com.srikanth.javareskill.service;

import com.srikanth.javareskill.domain.Employee;
import com.srikanth.javareskill.domain.enums.Role;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Analytics contract for salary-related queries over the employee population.
 *
 * <p>All methods operate on a snapshot of employees obtained from an
 * {@link EmployeeService}.  Implementations should make heavy use of the
 * Java Streams API ({@link java.util.stream.Stream}) to demonstrate
 * functional-style data processing.</p>
 *
 * <h2>Stream operations showcased</h2>
 * <ul>
 *   <li>{@code Collectors.groupingBy}   – group employees by department</li>
 *   <li>{@code sorted / limit}          – find top-N highest salaries</li>
 *   <li>{@code Collectors.toMap}        – compute average salary per role</li>
 *   <li>{@code mapToLong / summaryStatistics} – aggregate salary statistics</li>
 * </ul>
 */
public interface SalaryAnalyticsService {

    /**
     * Groups all employees by their department ID.
     *
     * @return an unmodifiable map where each key is a department ID and
     *         the value is the list of employees in that department;
     *         never {@code null}
     */
    Map<String, List<Employee>> groupByDepartment();

    /**
     * Returns the top {@code n} employees ordered by salary (highest first).
     *
     * <p>If fewer than {@code n} employees exist, all of them are returned.</p>
     *
     * @param n the maximum number of employees to return; must be &gt; 0
     * @return an unmodifiable list of up to {@code n} employees sorted by
     *         salary descending; never {@code null}
     * @throws IllegalArgumentException if {@code n} is &lt;= 0
     */
    List<Employee> topNHighestSalaries(int n);

    /**
     * Convenience shortcut for {@code topNHighestSalaries(5)}.
     */
    default List<Employee> top5HighestSalaries() {
        return topNHighestSalaries(5);
    }

    /**
     * Computes the average salary for each {@link Role}.
     *
     * <p>Roles that have no employees are omitted from the result map.</p>
     *
     * @return an unmodifiable map from {@link Role} to average salary
     *         (rounded to 2 decimal places, {@link java.math.RoundingMode#HALF_UP});
     *         never {@code null}
     */
    Map<Role, BigDecimal> averageSalaryPerRole();

    /**
     * Returns the single highest salary across all employees.
     *
     * @return the maximum salary, or {@link BigDecimal#ZERO} if there are
     *         no employees
     */
    BigDecimal maxSalary();

    /**
     * Returns the single lowest salary across all employees.
     *
     * @return the minimum salary, or {@link BigDecimal#ZERO} if there are
     *         no employees
     */
    BigDecimal minSalary();

    /**
     * Computes the total salary bill across all employees.
     *
     * @return the sum of all salaries; {@link BigDecimal#ZERO} if there are
     *         no employees
     */
    BigDecimal totalSalaryBill();
}

