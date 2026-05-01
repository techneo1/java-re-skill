package com.srikanth.javareskill.repository.jdbc;

import com.srikanth.javareskill.domain.Department;
import com.srikanth.javareskill.domain.Employee;
import com.srikanth.javareskill.domain.EmployeeId;
import com.srikanth.javareskill.domain.enums.EmployeeStatus;
import com.srikanth.javareskill.domain.enums.Role;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test for JDBC repository implementations.
 *
 * <p>Tests the complete JDBC DAO layer including connection management,
 * schema initialization, and CRUD operations using PreparedStatement.</p>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JdbcRepositoryIntegrationTest {

    private static JdbcDepartmentRepository deptRepo;
    private static JdbcEmployeeRepository empRepo;

    @BeforeAll
    static void initDatabase() throws SQLException {
        // Initialize connection pool with in-memory H2 database
        ConnectionManager.initializeDefault();
        SchemaInitializer.createTables();

        deptRepo = new JdbcDepartmentRepository();
        empRepo = new JdbcEmployeeRepository();
    }

    @AfterAll
    static void cleanup() {
        ConnectionManager.shutdown();
    }

    @BeforeEach
    void clearTables() throws SQLException {
        // Clear data before each test (but keep schema)
        SchemaInitializer.dropTables();
        SchemaInitializer.createTables();
    }

    // -------------------------------------------------------------------------
    // Department tests
    // -------------------------------------------------------------------------

    @Test
    @Order(1)
    void testDepartmentSaveAndFindById() {
        Department dept = Department.builder()
                .id("D001")
                .name("Engineering")
                .location("San Francisco")
                .build();

        deptRepo.save(dept);

        var found = deptRepo.findById("D001");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Engineering");
        assertThat(found.get().getLocation()).isEqualTo("San Francisco");
    }

    @Test
    @Order(2)
    void testDepartmentFindAll() {
        deptRepo.save(Department.builder().id("D001").name("Eng").location("SF").build());
        deptRepo.save(Department.builder().id("D002").name("HR").location("NY").build());

        List<Department> all = deptRepo.findAll();
        assertThat(all).hasSize(2);
    }

    @Test
    @Order(3)
    void testDepartmentUpdate() {
        Department dept = Department.builder()
                .id("D001")
                .name("Engineering")
                .location("SF")
                .build();
        deptRepo.save(dept);

        Department updated = Department.builder()
                .id("D001")
                .name("Software Engineering")
                .location("San Francisco")
                .build();
        deptRepo.update(updated);

        var found = deptRepo.findById("D001");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Software Engineering");
    }

    @Test
    @Order(4)
    void testDepartmentDeleteById() {
        deptRepo.save(Department.builder().id("D001").name("Eng").location("SF").build());
        assertThat(deptRepo.existsById("D001")).isTrue();

        deptRepo.deleteById("D001");
        assertThat(deptRepo.existsById("D001")).isFalse();
    }

    @Test
    @Order(5)
    void testDepartmentCount() {
        assertThat(deptRepo.count()).isEqualTo(0);

        deptRepo.save(Department.builder().id("D001").name("Eng").location("SF").build());
        deptRepo.save(Department.builder().id("D002").name("HR").location("NY").build());

        assertThat(deptRepo.count()).isEqualTo(2);
    }

    @Test
    @Order(6)
    void testDepartmentFindByLocation() {
        deptRepo.save(Department.builder().id("D001").name("Eng").location("SF").build());
        deptRepo.save(Department.builder().id("D002").name("HR").location("NY").build());
        deptRepo.save(Department.builder().id("D003").name("Sales").location("SF").build());

        List<Department> sfDepts = deptRepo.findByLocation("SF");
        assertThat(sfDepts).hasSize(2);
    }

    @Test
    @Order(7)
    void testDepartmentFindByName() {
        deptRepo.save(Department.builder().id("D001").name("Engineering").location("SF").build());

        var found = deptRepo.findByName("Engineering");
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo("D001");

        var notFound = deptRepo.findByName("Marketing");
        assertThat(notFound).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Employee tests
    // -------------------------------------------------------------------------

    @Test
    @Order(10)
    void testEmployeeSaveAndFindById() {
        // First create department (foreign key constraint)
        deptRepo.save(Department.builder().id("D001").name("Eng").location("SF").build());

        Employee emp = Employee.builder()
                .id("E001")
                .name("Alice Johnson")
                .email("alice@example.com")
                .departmentId("D001")
                .role(Role.ENGINEER)
                .salary(new BigDecimal("100000.00"))
                .status(EmployeeStatus.ACTIVE)
                .joiningDate(LocalDate.of(2020, 1, 1))
                .build();

        empRepo.save(emp);

        var found = empRepo.findById(new EmployeeId("E001"));
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Alice Johnson");
        assertThat(found.get().getEmail()).isEqualTo("alice@example.com");
        assertThat(found.get().getRole()).isEqualTo(Role.ENGINEER);
    }

    @Test
    @Order(11)
    void testEmployeeFindAll() {
        deptRepo.save(Department.builder().id("D001").name("Eng").location("SF").build());

        empRepo.save(createEmployee("E001", "Alice", "alice@example.com"));
        empRepo.save(createEmployee("E002", "Bob", "bob@example.com"));

        List<Employee> all = empRepo.findAll();
        assertThat(all).hasSize(2);
    }

    @Test
    @Order(12)
    void testEmployeeUpdate() {
        deptRepo.save(Department.builder().id("D001").name("Eng").location("SF").build());

        Employee emp = createEmployee("E001", "Alice", "alice@example.com");
        empRepo.save(emp);

        Employee updated = Employee.builder()
                .id("E001")
                .name("Alice Johnson-Smith")
                .email("alice.smith@example.com")
                .departmentId("D001")
                .role(Role.SENIOR_ENGINEER)
                .salary(new BigDecimal("120000.00"))
                .status(EmployeeStatus.ACTIVE)
                .joiningDate(emp.getJoiningDate())
                .build();

        empRepo.update(updated);

        var found = empRepo.findById(new EmployeeId("E001"));
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Alice Johnson-Smith");
        assertThat(found.get().getRole()).isEqualTo(Role.SENIOR_ENGINEER);
    }

    @Test
    @Order(13)
    void testEmployeeDeleteById() {
        deptRepo.save(Department.builder().id("D001").name("Eng").location("SF").build());

        empRepo.save(createEmployee("E001", "Alice", "alice@example.com"));
        assertThat(empRepo.existsById(new EmployeeId("E001"))).isTrue();

        empRepo.deleteById(new EmployeeId("E001"));
        assertThat(empRepo.existsById(new EmployeeId("E001"))).isFalse();
    }

    @Test
    @Order(14)
    void testEmployeeFindByDepartmentId() {
        deptRepo.save(Department.builder().id("D001").name("Eng").location("SF").build());
        deptRepo.save(Department.builder().id("D002").name("HR").location("NY").build());

        empRepo.save(createEmployeeInDept("E001", "Alice", "alice@example.com", "D001"));
        empRepo.save(createEmployeeInDept("E002", "Bob", "bob@example.com", "D001"));
        empRepo.save(createEmployeeInDept("E003", "Carol", "carol@example.com", "D002"));

        List<Employee> engEmps = empRepo.findByDepartmentId("D001");
        assertThat(engEmps).hasSize(2);
    }

    @Test
    @Order(15)
    void testEmployeeFindByStatus() {
        deptRepo.save(Department.builder().id("D001").name("Eng").location("SF").build());

        empRepo.save(createEmployeeWithStatus("E001", "Alice", EmployeeStatus.ACTIVE));
        empRepo.save(createEmployeeWithStatus("E002", "Bob", EmployeeStatus.ACTIVE));
        empRepo.save(createEmployeeWithStatus("E003", "Carol", EmployeeStatus.INACTIVE));

        List<Employee> active = empRepo.findByStatus(EmployeeStatus.ACTIVE);
        assertThat(active).hasSize(2);

        List<Employee> inactive = empRepo.findByStatus(EmployeeStatus.INACTIVE);
        assertThat(inactive).hasSize(1);
    }

    @Test
    @Order(16)
    void testEmployeeFindByRole() {
        deptRepo.save(Department.builder().id("D001").name("Eng").location("SF").build());

        empRepo.save(createEmployeeWithRole("E001", "Alice", Role.ENGINEER));
        empRepo.save(createEmployeeWithRole("E002", "Bob", Role.ENGINEER));
        empRepo.save(createEmployeeWithRole("E003", "Carol", Role.MANAGER));

        List<Employee> engineers = empRepo.findByRole(Role.ENGINEER);
        assertThat(engineers).hasSize(2);

        List<Employee> managers = empRepo.findByRole(Role.MANAGER);
        assertThat(managers).hasSize(1);
    }

    // -------------------------------------------------------------------------
    // Helper methods
    // -------------------------------------------------------------------------

    private Employee createEmployee(String id, String name, String email) {
        return Employee.builder()
                .id(id)
                .name(name)
                .email(email)
                .departmentId("D001")
                .role(Role.ENGINEER)
                .salary(new BigDecimal("100000.00"))
                .status(EmployeeStatus.ACTIVE)
                .joiningDate(LocalDate.of(2020, 1, 1))
                .build();
    }

    private Employee createEmployeeInDept(String id, String name, String email, String deptId) {
        return Employee.builder()
                .id(id)
                .name(name)
                .email(email)
                .departmentId(deptId)
                .role(Role.ENGINEER)
                .salary(new BigDecimal("100000.00"))
                .status(EmployeeStatus.ACTIVE)
                .joiningDate(LocalDate.of(2020, 1, 1))
                .build();
    }

    private Employee createEmployeeWithStatus(String id, String name, EmployeeStatus status) {
        return Employee.builder()
                .id(id)
                .name(name)
                .email(name.toLowerCase() + "@example.com")
                .departmentId("D001")
                .role(Role.ENGINEER)
                .salary(new BigDecimal("100000.00"))
                .status(status)
                .joiningDate(LocalDate.of(2020, 1, 1))
                .build();
    }

    private Employee createEmployeeWithRole(String id, String name, Role role) {
        return Employee.builder()
                .id(id)
                .name(name)
                .email(name.toLowerCase() + "@example.com")
                .departmentId("D001")
                .role(role)
                .salary(new BigDecimal("100000.00"))
                .status(EmployeeStatus.ACTIVE)
                .joiningDate(LocalDate.of(2020, 1, 1))
                .build();
    }
}

