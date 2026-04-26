package com.srikanth.javareskill;

import com.srikanth.javareskill.domain.Department;
import com.srikanth.javareskill.domain.Employee;
import com.srikanth.javareskill.domain.EmployeeId;
import com.srikanth.javareskill.domain.enums.EmployeeStatus;
import com.srikanth.javareskill.domain.enums.Role;
import com.srikanth.javareskill.repository.inmemory.InMemoryDepartmentRepository;
import com.srikanth.javareskill.repository.inmemory.InMemoryEmployeeRepository;
import com.srikanth.javareskill.service.DepartmentService;
import com.srikanth.javareskill.service.EmployeeService;
import com.srikanth.javareskill.service.impl.DepartmentServiceImpl;
import com.srikanth.javareskill.service.impl.EmployeeServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entry point for the java-re-skill project.
 *
 * <p>Demonstrates the layered architecture:</p>
 * <pre>
 *   domain  →  repository (in-memory)  →  service
 * </pre>
 */
public class App {

    public static void main(String[] args) {

        // ── Wire up the layers ─────────────────────────────────────────────
        DepartmentService deptService = new DepartmentServiceImpl(new InMemoryDepartmentRepository());
        EmployeeService   empService  = new EmployeeServiceImpl(new InMemoryEmployeeRepository());

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

        System.out.println("\njava-re-skill — layered architecture ready!");
    }
}
