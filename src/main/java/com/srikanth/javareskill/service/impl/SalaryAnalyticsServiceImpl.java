package com.srikanth.javareskill.service.impl;

import com.srikanth.javareskill.domain.Employee;
import com.srikanth.javareskill.domain.enums.EmployeeStatus;
import com.srikanth.javareskill.domain.enums.Role;
import com.srikanth.javareskill.service.EmployeeService;
import com.srikanth.javareskill.service.SalaryAnalyticsService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Stream-based implementation of {@link SalaryAnalyticsService}.
 *
 * <p>Every public method obtains its employee snapshot from
 * {@link EmployeeService#findAll()} and processes it with the Java Streams
 * API — no mutable state, no loops.</p>
 *
 * <h2>Java Streams features demonstrated</h2>
 * <ul>
 *   <li>{@code Collectors.groupingBy}               – {@link #groupByDepartment()}</li>
 *   <li>{@code sorted(Comparator.reversed())}       – {@link #topNHighestSalaries(int)}</li>
 *   <li>{@code limit(n)}                            – {@link #topNHighestSalaries(int)}</li>
 *   <li>{@code Collectors.groupingBy + averaging}   – {@link #averageSalaryPerRole()}</li>
 *   <li>{@code Collectors.partitioningBy}           – {@link #partitionByStatus()}</li>
 *   <li>{@code map + reduce}                        – {@link #totalSalaryBill()}</li>
 *   <li>{@code max / min(Comparator)}               – {@link #maxSalary()}, {@link #minSalary()}</li>
 * </ul>
 *
 * <h2>SOLID principles applied</h2>
 * <ul>
 *   <li><b>S – Single Responsibility</b>: This class does only salary analytics;
 *       employee lifecycle management stays in {@link EmployeeServiceImpl}.</li>
 *   <li><b>D – Dependency Inversion</b>: Depends on the {@link EmployeeService}
 *       abstraction, not a concrete class.</li>
 * </ul>
 */
public final class SalaryAnalyticsServiceImpl implements SalaryAnalyticsService {

    private static final int SALARY_SCALE    = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final EmployeeService employeeService;

    /**
     * @param employeeService the source of employee data; must not be {@code null}
     */
    public SalaryAnalyticsServiceImpl(EmployeeService employeeService) {
        this.employeeService = Objects.requireNonNull(employeeService,
                "employeeService must not be null");
    }

    // -------------------------------------------------------------------------
    // SalaryAnalyticsService implementation
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Uses {@code Collectors.groupingBy(Employee::getDepartmentId)} to
     * partition the employee list by department in a single pass.</p>
     */
    @Override
    public Map<String, List<Employee>> groupByDepartment() {
        return employeeService.findAll().stream()
                .collect(Collectors.groupingBy(
                        Employee::getDepartmentId,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                Collections::unmodifiableList)));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Pipeline: {@code stream → sorted(salary DESC) → limit(n) → toList}.</p>
     */
    @Override
    public List<Employee> topNHighestSalaries(int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("n must be > 0, got: " + n);
        }
        return employeeService.findAll().stream()
                .sorted(Comparator.comparing(Employee::getSalary).reversed())
                .limit(n)
                .toList();                      // Java 16+ unmodifiable list
    }

    /**
     * {@inheritDoc}
     *
     * <p>Groups employees by {@link Role}, then computes the arithmetic mean
     * of each group's salaries using {@code BigDecimal} arithmetic for
     * precision.  The average is rounded to 2 decimal places
     * ({@link RoundingMode#HALF_UP}).</p>
     */
    @Override
    public Map<Role, BigDecimal> averageSalaryPerRole() {
        return employeeService.findAll().stream()
                .collect(Collectors.groupingBy(
                        Employee::getRole,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                SalaryAnalyticsServiceImpl::averageSalary)));
    }

    /** {@inheritDoc} */
    @Override
    public BigDecimal maxSalary() {
        return employeeService.findAll().stream()
                .map(Employee::getSalary)
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);
    }

    /** {@inheritDoc} */
    @Override
    public BigDecimal minSalary() {
        return employeeService.findAll().stream()
                .map(Employee::getSalary)
                .min(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);
    }

    /** {@inheritDoc} */
    @Override
    public BigDecimal totalSalaryBill() {
        return employeeService.findAll().stream()
                .map(Employee::getSalary)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Uses {@code Collectors.partitioningBy} with the predicate
     * {@code employee.getStatus() == EmployeeStatus.ACTIVE}.  The collector
     * always produces a two-entry map ({@code true} / {@code false}) even
     * when one partition is empty.</p>
     */
    @Override
    public Map<Boolean, List<Employee>> partitionByStatus() {
        return employeeService.findAll().stream()
                .collect(Collectors.partitioningBy(
                        e -> e.getStatus() == EmployeeStatus.ACTIVE,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                Collections::unmodifiableList)));
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Computes the arithmetic mean of the salaries in the given list.
     *
     * @param employees non-empty list (guaranteed by upstream groupingBy)
     * @return average salary rounded to {@value #SALARY_SCALE} decimal places
     */
    private static BigDecimal averageSalary(List<Employee> employees) {
        BigDecimal sum = employees.stream()
                .map(Employee::getSalary)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(employees.size()), SALARY_SCALE, ROUNDING);
    }
}

