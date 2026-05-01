package com.srikanth.javareskill;

import com.srikanth.javareskill.domain.Department;
import com.srikanth.javareskill.domain.Employee;
import com.srikanth.javareskill.domain.EmployeeId;
import com.srikanth.javareskill.domain.enums.EmployeeStatus;
import com.srikanth.javareskill.domain.enums.Role;
import com.srikanth.javareskill.repository.DepartmentRepository;
import com.srikanth.javareskill.repository.EmployeeRepository;
import com.srikanth.javareskill.repository.jdbc.ConnectionManager;
import com.srikanth.javareskill.repository.jdbc.JdbcDepartmentRepository;
import com.srikanth.javareskill.repository.jdbc.JdbcEmployeeRepository;
import com.srikanth.javareskill.repository.jdbc.SchemaInitializer;
import com.srikanth.javareskill.service.DepartmentService;
import com.srikanth.javareskill.service.EmployeeService;
import com.srikanth.javareskill.service.SalaryAnalyticsService;
import com.srikanth.javareskill.service.ValidationService;
import com.srikanth.javareskill.service.impl.DepartmentServiceImpl;
import com.srikanth.javareskill.service.impl.EmployeeServiceImpl;
import com.srikanth.javareskill.service.impl.SalaryAnalyticsServiceImpl;
import com.srikanth.javareskill.service.impl.ValidationServiceImpl;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * Demo application showcasing the JDBC DAO layer with PreparedStatement.
 *
 * <p>This application demonstrates:</p>
 * <ul>
 *   <li><b>JDBC connection pooling</b> via HikariCP ({@link ConnectionManager})</li>
 *   <li><b>PreparedStatement usage</b> for SQL injection prevention and performance</li>
 *   <li><b>Schema initialization</b> with DDL scripts ({@link SchemaInitializer})</li>
 *   <li><b>DAO pattern</b> with JDBC implementations ({@link JdbcDepartmentRepository},
 *       {@link JdbcEmployeeRepository})</li>
 *   <li><b>Transaction management</b> (auto-commit mode for simplicity)</li>
 *   <li><b>Layered architecture</b>: DAO → Service → Business logic</li>
 * </ul>
 *
 * <h2>Architecture</h2>
 * <pre>
 *   Domain Objects (Employee, Department)
 *        ↓
 *   Repository Interfaces (EmployeeRepository, DepartmentRepository)
 *        ↓
 *   JDBC DAO Implementations (JdbcEmployeeRepository, JdbcDepartmentRepository)
 *        ↓
 *   ConnectionManager (HikariCP)
 *        ↓
 *   H2 In-Memory Database
 * </pre>
 *
 * <h2>SOLID principles demonstrated</h2>
 * <ul>
 *   <li><b>D – Dependency Inversion</b>: Services depend on repository interfaces,
 *       not concrete JDBC implementations. Swapping in-memory for JDBC requires
 *       only changing the wiring in {@code main()}.</li>
 * </ul>
 */
public class JdbcDemoApp {

    public static void main(String[] args) {
        try {
            // ── Step 1: Initialize database connection pool ──────────────────
            System.out.println("=== Initializing JDBC Connection Pool (HikariCP) ===");
            ConnectionManager.initializeDefault();
            System.out.println("✓ Connection pool ready\n");

            // ── Step 2: Create database schema ───────────────────────────────
            System.out.println("=== Creating Database Schema ===");
            SchemaInitializer.createTables();
            System.out.println("✓ Tables created: departments, employees\n");

            // ── Step 3: Wire up JDBC repositories ────────────────────────────
            DepartmentRepository deptRepo = new JdbcDepartmentRepository();
            EmployeeRepository   empRepo  = new JdbcEmployeeRepository();

            ValidationService validationService = new ValidationServiceImpl(empRepo, deptRepo);
            DepartmentService deptService = new DepartmentServiceImpl(deptRepo);
            EmployeeService   empService  = new EmployeeServiceImpl(empRepo, validationService);

            System.out.println("=== JDBC DAO Layer Ready ===\n");

            // ── Step 4: CRUD operations via JDBC ─────────────────────────────
            System.out.println("--- Department Operations (JDBC + PreparedStatement) ---");

            Department engineering = Department.builder()
                    .id("D001")
                    .name("Engineering")
                    .location("San Francisco")
                    .build();
            deptService.create(engineering);
            System.out.println("✓ Inserted: " + engineering);

            Department hr = Department.builder()
                    .id("D002")
                    .name("Human Resources")
                    .location("New York")
                    .build();
            deptService.create(hr);
            System.out.println("✓ Inserted: " + hr);

            System.out.println("\nAll departments: " + deptService.findAll());
            System.out.println("Count: " + deptService.count());

            // ── Step 5: Employee operations ──────────────────────────────────
            System.out.println("\n--- Employee Operations (JDBC + PreparedStatement) ---");

            Employee alice = Employee.builder()
                    .id("E001")
                    .name("Alice Johnson")
                    .email("alice.johnson@company.com")
                    .departmentId("D001")
                    .role(Role.SENIOR_ENGINEER)
                    .salary(new BigDecimal("120000.00"))
                    .status(EmployeeStatus.ACTIVE)
                    .joiningDate(LocalDate.of(2020, 1, 15))
                    .build();
            empService.hire(alice);
            System.out.println("✓ Hired: " + alice.getName());

            Employee bob = Employee.builder()
                    .id("E002")
                    .name("Bob Smith")
                    .email("bob.smith@company.com")
                    .departmentId("D001")
                    .role(Role.ENGINEER)
                    .salary(new BigDecimal("95000.00"))
                    .status(EmployeeStatus.ACTIVE)
                    .joiningDate(LocalDate.of(2021, 6, 1))
                    .build();
            empService.hire(bob);
            System.out.println("✓ Hired: " + bob.getName());

            Employee carol = Employee.builder()
                    .id("E003")
                    .name("Carol Williams")
                    .email("carol.williams@company.com")
                    .departmentId("D002")
                    .role(Role.MANAGER)
                    .salary(new BigDecimal("110000.00"))
                    .status(EmployeeStatus.ACTIVE)
                    .joiningDate(LocalDate.of(2019, 3, 10))
                    .build();
            empService.hire(carol);
            System.out.println("✓ Hired: " + carol.getName());

            // ── Step 6: Query operations ─────────────────────────────────────
            System.out.println("\n--- Query Operations (PreparedStatement) ---");
            System.out.println("Total employees: " + empService.headcount());
            System.out.println("Engineering dept: " + empService.findByDepartment("D001").size() + " employee(s)");
            System.out.println("Active employees: " + empService.findByStatus(EmployeeStatus.ACTIVE).size());
            System.out.println("Managers: " + empService.findByRole(Role.MANAGER).size());

            // Find by ID
            empService.findById(new EmployeeId("E001"))
                    .ifPresent(e -> System.out.println("Found by ID: " + e.getName() + " (" + e.getEmail() + ")"));

            // ── Step 7: Update operation ─────────────────────────────────────
            System.out.println("\n--- Update Operation ---");
            Employee updatedAlice = Employee.builder()
                    .id(alice.getId())
                    .name(alice.getName())
                    .email("alice.j@company.com")  // Updated email
                    .departmentId(alice.getDepartmentId())
                    .role(Role.DIRECTOR)            // Promoted!
                    .salary(new BigDecimal("135000.00"))
                    .status(alice.getStatus())
                    .joiningDate(alice.getJoiningDate())
                    .build();
            empService.update(updatedAlice);
            System.out.println("✓ Updated Alice's role and salary");

            // ── Step 8: Salary analytics ─────────────────────────────────────
            SalaryAnalyticsService analytics = new SalaryAnalyticsServiceImpl(empService);
            System.out.println("\n--- Salary Analytics (Streams API) ---");
            System.out.println("Total payroll: " + analytics.totalSalaryBill());
            System.out.println("Max salary: " + analytics.maxSalary());
            System.out.println("Min salary: " + analytics.minSalary());
            System.out.println("Average by role: " + analytics.averageSalaryPerRole());
            System.out.println("Top earner: " + analytics.topNHighestSalaries(1).get(0).getName());

            // ── Step 9: Demonstrate transaction (auto-commit) ────────────────
            System.out.println("\n--- Demonstrating PreparedStatement Security ---");
            System.out.println("✓ All SQL uses PreparedStatement → Safe from SQL injection");
            System.out.println("✓ Parameters are bound, not concatenated");

            System.out.println("\n=== JDBC DAO Demo Complete ===");

        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Application error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // ── Cleanup: Close connection pool ───────────────────────────────
            System.out.println("\nShutting down connection pool...");
            ConnectionManager.shutdown();
            System.out.println("✓ Cleanup complete");
        }
    }
}

