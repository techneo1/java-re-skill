package com.srikanth.javareskill;

import com.srikanth.javareskill.domain.Department;
import com.srikanth.javareskill.domain.Employee;
import com.srikanth.javareskill.domain.PayrollRecord;
import com.srikanth.javareskill.domain.enums.EmployeeStatus;
import com.srikanth.javareskill.domain.enums.Role;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entry point for the java-re-skill project.
 *
 * <p>Demonstrates creation of the core domain entities:
 * {@link Department}, {@link Employee}, and {@link PayrollRecord}.</p>
 */
public class App {

    public static void main(String[] args) {

        // --- Department ---------------------------------------------------
        Department engineering = Department.builder()
                .id("D001")
                .name("Engineering")
                .location("New York")
                .build();

        System.out.println("Department  : " + engineering);

        // --- Employee -----------------------------------------------------
        Employee alice = Employee.builder()
                .id("E001")
                .name("Alice Smith")
                .email("alice.smith@example.com")
                .departmentId(engineering.getId())
                .role(Role.ENGINEER)
                .salary(new BigDecimal("95000.00"))
                .status(EmployeeStatus.ACTIVE)
                .joiningDate(LocalDate.of(2021, 3, 15))
                .build();

        System.out.println("Employee    : " + alice);

        // --- PayrollRecord ------------------------------------------------
        PayrollRecord aprilPayroll = PayrollRecord.builder()
                .id("PR001")
                .employeeId(alice.getId())
                .grossSalary(alice.getSalary())
                .taxAmount(new BigDecimal("19000.00"))   // ~20 % tax
                .payrollMonth(LocalDate.of(2026, 4, 1))
                .processedTimestamp(LocalDateTime.now())
                .build();

        System.out.printf("Payroll     : gross=%.2f  tax=%.2f  net=%.2f%n",
                aprilPayroll.getGrossSalary(),
                aprilPayroll.getTaxAmount(),
                aprilPayroll.getNetSalary());

        System.out.println("\njava-re-skill — domain layer ready!");
    }
}

