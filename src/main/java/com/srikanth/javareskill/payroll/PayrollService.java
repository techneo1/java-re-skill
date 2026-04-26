package com.srikanth.javareskill.payroll;

import com.srikanth.javareskill.domain.Employee;
import com.srikanth.javareskill.domain.PayrollRecord;
import com.srikanth.javareskill.payroll.strategy.TaxStrategy;

import java.time.LocalDate;
import java.util.List;

/**
 * Service for payroll processing.
 *
 * <p>Consumers can supply a custom {@link TaxStrategy} per call, or rely on the
 * implementation's default (role-based) strategy resolution.</p>
 */
public interface PayrollService {

    /**
     * Processes payroll for a single employee using the provided tax strategy.
     *
     * @param employee    the employee to process; must not be {@code null}
     * @param payrollMonth first day of the month being processed; must not be {@code null}
     * @param taxStrategy the strategy to use for tax calculation; must not be {@code null}
     * @return the generated {@link PayrollRecord}
     */
    PayrollRecord process(Employee employee, LocalDate payrollMonth, TaxStrategy taxStrategy);

    /**
     * Processes payroll for a single employee using the default role-based strategy.
     *
     * @param employee    the employee to process; must not be {@code null}
     * @param payrollMonth first day of the month being processed; must not be {@code null}
     * @return the generated {@link PayrollRecord}
     */
    PayrollRecord process(Employee employee, LocalDate payrollMonth);

    /**
     * Processes payroll for all employees using role-based strategy resolution.
     *
     * @param employees    employees to process; must not be {@code null}
     * @param payrollMonth first day of the month being processed; must not be {@code null}
     * @return list of generated records, one per employee, in the same order as input
     */
    List<PayrollRecord> processAll(List<Employee> employees, LocalDate payrollMonth);
}

