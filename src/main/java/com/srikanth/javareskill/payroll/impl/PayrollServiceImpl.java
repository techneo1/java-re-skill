package com.srikanth.javareskill.payroll.impl;

import com.srikanth.javareskill.domain.Employee;
import com.srikanth.javareskill.domain.PayrollRecord;
import com.srikanth.javareskill.payroll.PayrollService;
import com.srikanth.javareskill.payroll.strategy.TaxStrategy;
import com.srikanth.javareskill.payroll.strategy.TaxStrategyFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Default implementation of {@link PayrollService}.
 *
 * <p>Delegates all tax-calculation concerns to an injected {@link TaxStrategy},
 * keeping this class free from tax-specific logic (Open/Closed Principle).</p>
 */
public final class PayrollServiceImpl implements PayrollService {

    // -------------------------------------------------------------------------
    // PayrollService implementation
    // -------------------------------------------------------------------------

    @Override
    public PayrollRecord process(Employee employee, LocalDate payrollMonth, TaxStrategy taxStrategy) {
        Objects.requireNonNull(employee,     "employee must not be null");
        Objects.requireNonNull(payrollMonth, "payrollMonth must not be null");
        Objects.requireNonNull(taxStrategy,  "taxStrategy must not be null");

        var grossSalary = employee.getSalary();
        var taxAmount   = taxStrategy.calculateTax(grossSalary);

        return PayrollRecord.builder()
                .id(UUID.randomUUID().toString())
                .employeeId(employee.getId())
                .grossSalary(grossSalary)
                .taxAmount(taxAmount)
                .payrollMonth(payrollMonth.withDayOfMonth(1))
                .processedTimestamp(LocalDateTime.now())
                .build();
    }

    @Override
    public PayrollRecord process(Employee employee, LocalDate payrollMonth) {
        Objects.requireNonNull(employee, "employee must not be null");
        TaxStrategy strategy = TaxStrategyFactory.forRole(employee.getRole());
        return process(employee, payrollMonth, strategy);
    }

    @Override
    public List<PayrollRecord> processAll(List<Employee> employees, LocalDate payrollMonth) {
        Objects.requireNonNull(employees,    "employees must not be null");
        Objects.requireNonNull(payrollMonth, "payrollMonth must not be null");

        List<PayrollRecord> records = employees.stream()
                .map(emp -> process(emp, payrollMonth))
                .toList();
        return List.copyOf(records);
    }
}

