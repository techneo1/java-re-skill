package com.srikanth.javareskill;

import com.srikanth.javareskill.domain.Department;
import com.srikanth.javareskill.domain.Employee;
import com.srikanth.javareskill.domain.EmployeeId;
import com.srikanth.javareskill.domain.enums.EmployeeStatus;
import com.srikanth.javareskill.domain.enums.Role;
import com.srikanth.javareskill.domain.PayrollRecord;
import com.srikanth.javareskill.payroll.PayrollService;
import com.srikanth.javareskill.payroll.impl.PayrollServiceImpl;
import com.srikanth.javareskill.repository.inmemory.InMemoryDepartmentRepository;
import com.srikanth.javareskill.repository.inmemory.InMemoryEmployeeRepository;
import com.srikanth.javareskill.service.DepartmentService;
import com.srikanth.javareskill.service.EmployeeService;
import com.srikanth.javareskill.service.SalaryAnalyticsService;
import com.srikanth.javareskill.service.ValidationService;
import com.srikanth.javareskill.service.impl.DepartmentServiceImpl;
import com.srikanth.javareskill.service.impl.EmployeeServiceImpl;
import com.srikanth.javareskill.service.impl.SalaryAnalyticsServiceImpl;
import com.srikanth.javareskill.service.impl.ValidationServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Entry point for the java-re-skill project.
 *
 * <p>Demonstrates the layered architecture:</p>
 * <pre>
 *   domain  →  repository (in-memory)  →  service
 * </pre>
 *
 * <h2>SOLID wiring illustrated here</h2>
 * <ul>
 *   <li><b>D – Dependency Inversion</b>: All service variables are declared as
 *       interface types ({@code EmployeeService}, {@code ValidationService}).
 *       Concrete classes only appear in the {@code new} expressions below — the
 *       rest of the code is free of implementation details.</li>
 * </ul>
 */
public class App {

    public static void main(String[] args) {

        // ── Wire up the layers (Dependency Inversion: program to interfaces) ──
        InMemoryDepartmentRepository deptRepo = new InMemoryDepartmentRepository();
        InMemoryEmployeeRepository   empRepo  = new InMemoryEmployeeRepository();

        // DIP: ValidationService is an abstraction; EmployeeServiceImpl depends on it.
        ValidationService validationService = new ValidationServiceImpl(empRepo, deptRepo);
        DepartmentService deptService = new DepartmentServiceImpl(deptRepo);
        EmployeeService   empService  = new EmployeeServiceImpl(empRepo, validationService);

        // ── Department layer ──────────────────────────────────────────────
        Department engineering = Department.builder()
                .id("D001").name("Engineering").location("New York").build();
        deptService.create(engineering);
        System.out.println("Departments : " + deptService.findAll());

        // ── Employee layer ────────────────────────────────────────────────
        Employee alice = Employee.builder()
                .id("E001").name("Alice Smith").email("alice.smith@example.com")
                .departmentId(engineering.getId()).role(Role.ENGINEER)
                .salary(new BigDecimal("95000.00")).status(EmployeeStatus.ACTIVE)
                .joiningDate(LocalDate.of(2021, 3, 15)).build();

        Employee bob = Employee.builder()
                .id("E002").name("Bob Jones").email("bob.jones@example.com")
                .departmentId(engineering.getId()).role(Role.SENIOR_ENGINEER)
                .salary(new BigDecimal("115000.00")).status(EmployeeStatus.ACTIVE)
                .joiningDate(LocalDate.of(2019, 7, 1)).build();

        empService.hire(alice);
        empService.hire(bob);

        System.out.println("Headcount   : " + empService.headcount());
        System.out.println("In Eng dept : " + empService.findByDepartment("D001").size() + " employee(s)");
        System.out.println("Active      : " + empService.findByStatus(EmployeeStatus.ACTIVE).size() + " employee(s)");

        // Look up by ID (value object in domain layer)
        empService.findById(new EmployeeId("E001"))
                .ifPresent(e -> System.out.println("Found       : " + e.getName()));

        // ── Salary Analytics (Streams API) ─────────────────────────────────
        SalaryAnalyticsService analytics = new SalaryAnalyticsServiceImpl(empService);

        System.out.println("\n--- Salary Analytics ---");
        System.out.println("Total bill  : " + analytics.totalSalaryBill());
        System.out.println("Max salary  : " + analytics.maxSalary());
        System.out.println("Min salary  : " + analytics.minSalary());
        System.out.println("Top 1       : " + analytics.topNHighestSalaries(1).get(0).getName());
        System.out.println("Avg by role : " + analytics.averageSalaryPerRole());
        System.out.println("By dept     : " + analytics.groupByDepartment().keySet());
        System.out.println("Active/Inactive partition sizes: "
                + analytics.partitionByStatus().get(true).size() + " / "
                + analytics.partitionByStatus().get(false).size());

        // ── Payroll Processing (Strategy Pattern) ──────────────────────────
        PayrollService payrollService = new PayrollServiceImpl();
        LocalDate payMonth = LocalDate.of(2026, 5, 1);

        System.out.println("\n--- Payroll Processing ---");
        List<PayrollRecord> records = payrollService.processAll(empService.findAll(), payMonth);
        records.forEach(r -> System.out.printf("  %s → gross=%s, tax=%s, net=%s%n",
                r.getEmployeeId(), r.getGrossSalary(), r.getTaxAmount(), r.getNetSalary()));

        System.out.println("\njava-re-skill — layered architecture ready!");
    }
}
