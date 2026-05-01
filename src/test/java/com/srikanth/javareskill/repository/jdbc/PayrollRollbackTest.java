package com.srikanth.javareskill.repository.jdbc;

import com.srikanth.javareskill.domain.Employee;
import com.srikanth.javareskill.domain.PayrollRecord;
import com.srikanth.javareskill.domain.enums.EmployeeStatus;
import com.srikanth.javareskill.domain.enums.Role;
import com.srikanth.javareskill.payroll.PayrollService;
import com.srikanth.javareskill.payroll.impl.PayrollServiceImpl;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests demonstrating rollback on failure during payroll processing.
 *
 * <p>These tests verify that when payroll processing fails for ANY employee,
 * the ENTIRE payroll batch is rolled back, ensuring no partial payments.</p>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PayrollRollbackTest {

    private static final BigDecimal COMPANY_BALANCE = new BigDecimal("500000.00");

    @BeforeAll
    static void initDatabase() throws SQLException {
        ConnectionManager.initializeDefault();
        SchemaInitializer.createTables();
        createPayrollTable();
    }

    @AfterAll
    static void cleanup() {
        ConnectionManager.shutdown();
    }

    @BeforeEach
    void setupTestData() throws SQLException {
        clearAllTables();
        insertTestData();
    }

    // -------------------------------------------------------------------------
    // Successful Payroll Tests
    // -------------------------------------------------------------------------

    @Test
    @Order(1)
    @DisplayName("Should commit all payroll records when processing succeeds")
    void testSuccessfulPayrollCommit() throws SQLException {
        TransactionManager tx = new TransactionManager();
        PayrollService payrollService = new PayrollServiceImpl();

        try {
            tx.begin();
            Connection conn = tx.getConnection();

            List<Employee> employees = getActiveEmployees(conn);
            assertThat(employees).hasSize(5);

            // Process payroll for all employees
            for (Employee emp : employees) {
                PayrollRecord record = payrollService.process(emp, LocalDate.now());
                insertPayrollRecord(conn, record);
            }

            tx.commit();

        } finally {
            tx.close();
        }

        // Verify all 5 payroll records were committed
        int count = countPayrollRecords();
        assertThat(count).isEqualTo(5);
    }

    // -------------------------------------------------------------------------
    // Rollback on Failure Tests
    // -------------------------------------------------------------------------

    @Test
    @Order(2)
    @DisplayName("Should rollback all payments when one employee fails")
    void testRollbackOnSingleEmployeeFailure() throws SQLException {
        TransactionManager tx = new TransactionManager();
        PayrollService payrollService = new PayrollServiceImpl();

        int processedCount = 0;

        try {
            tx.begin();
            Connection conn = tx.getConnection();

            List<Employee> employees = getActiveEmployees(conn);

            for (Employee emp : employees) {
                // Simulate failure on 3rd employee
                if (processedCount == 2) {
                    throw new RuntimeException("Simulated payment failure for: " + emp.getName());
                }

                PayrollRecord record = payrollService.process(emp, LocalDate.now());
                insertPayrollRecord(conn, record);
                processedCount++;
            }

            tx.commit();
            fail("Expected exception to be thrown");

        } catch (RuntimeException e) {
            tx.rollback();
            // Expected
        } finally {
            tx.close();
        }

        // Verify NO payroll records were committed (atomicity)
        int count = countPayrollRecords();
        assertThat(count).isEqualTo(0);
        assertThat(processedCount).isEqualTo(2); // 2 were processed but rolled back
    }

    @Test
    @Order(3)
    @DisplayName("Should rollback when insufficient funds detected")
    void testRollbackOnInsufficientFunds() throws SQLException {
        TransactionManager tx = new TransactionManager();
        PayrollService payrollService = new PayrollServiceImpl();

        BigDecimal balance = new BigDecimal("150000.00"); // Not enough for all employees
        int processedCount = 0;

        try {
            tx.begin();
            Connection conn = tx.getConnection();

            List<Employee> employees = getActiveEmployees(conn);
            BigDecimal totalPaid = BigDecimal.ZERO;

            for (Employee emp : employees) {
                PayrollRecord record = payrollService.process(emp, LocalDate.now());

                // Check if we have enough funds
                if (balance.compareTo(record.getNetSalary()) < 0) {
                    throw new InsufficientFundsException(
                        "Cannot pay " + emp.getName() + ": insufficient funds");
                }

                balance = balance.subtract(record.getNetSalary());
                totalPaid = totalPaid.add(record.getNetSalary());
                insertPayrollRecord(conn, record);
                processedCount++;
            }

            tx.commit();
            fail("Expected InsufficientFundsException");

        } catch (InsufficientFundsException e) {
            tx.rollback();
            // Expected - some employees couldn't be paid
        } finally {
            tx.close();
        }

        // Verify NO payments were committed
        int count = countPayrollRecords();
        assertThat(count).isEqualTo(0);
        assertThat(processedCount).isGreaterThan(0).isLessThan(5);
    }

    @Test
    @Order(4)
    @DisplayName("Should rollback when invalid employee status detected")
    void testRollbackOnInvalidEmployeeStatus() throws SQLException {
        // Add an inactive employee
        addInactiveEmployee();

        TransactionManager tx = new TransactionManager();
        PayrollService payrollService = new PayrollServiceImpl();

        int processedCount = 0;

        try {
            tx.begin();
            Connection conn = tx.getConnection();

            // Get ALL employees (including inactive)
            List<Employee> employees = getAllEmployees(conn);
            assertThat(employees).hasSize(6); // 5 active + 1 inactive

            for (Employee emp : employees) {
                // Validate employee status
                if (emp.getStatus() != EmployeeStatus.ACTIVE) {
                    throw new InvalidEmployeeException(
                        "Cannot process payroll for inactive employee: " + emp.getName());
                }

                PayrollRecord record = payrollService.process(emp, LocalDate.now());
                insertPayrollRecord(conn, record);
                processedCount++;
            }

            tx.commit();
            fail("Expected InvalidEmployeeException");

        } catch (InvalidEmployeeException e) {
            tx.rollback();
            // Expected - inactive employee prevented payroll
        } finally {
            tx.close();
        }

        // Verify NO payments were committed (even for active employees)
        int count = countPayrollRecords();
        assertThat(count).isEqualTo(0);
        assertThat(processedCount).isEqualTo(5); // 5 active were processed but rolled back
    }

    @Test
    @Order(5)
    @DisplayName("Should rollback when business rule violated (daily limit exceeded)")
    void testRollbackOnBusinessRuleViolation() throws SQLException {
        TransactionManager tx = new TransactionManager();
        PayrollService payrollService = new PayrollServiceImpl();

        final BigDecimal DAILY_LIMIT = new BigDecimal("400000.00");
        int processedCount = 0;

        try {
            tx.begin();
            Connection conn = tx.getConnection();

            List<Employee> employees = getActiveEmployees(conn);
            BigDecimal totalPayroll = BigDecimal.ZERO;

            for (Employee emp : employees) {
                PayrollRecord record = payrollService.process(emp, LocalDate.now());

                // Check business rule: daily payment limit
                BigDecimal projected = totalPayroll.add(record.getNetSalary());
                if (projected.compareTo(DAILY_LIMIT) > 0) {
                    throw new BusinessRuleException(
                        "Daily payment limit exceeded: " + projected + " > " + DAILY_LIMIT);
                }

                totalPayroll = totalPayroll.add(record.getNetSalary());
                insertPayrollRecord(conn, record);
                processedCount++;
            }

            tx.commit();
            fail("Expected BusinessRuleException");

        } catch (BusinessRuleException e) {
            tx.rollback();
            // Expected - total payroll exceeded daily limit
        } finally {
            tx.close();
        }

        // Verify NO payments were committed
        int count = countPayrollRecords();
        assertThat(count).isEqualTo(0);
        assertThat(processedCount).isGreaterThan(0); // Some were processed but rolled back
    }

    @Test
    @Order(6)
    @DisplayName("Should rollback when database constraint violated")
    void testRollbackOnDatabaseConstraintViolation() throws SQLException {
        TransactionManager tx = new TransactionManager();
        PayrollService payrollService = new PayrollServiceImpl();

        int processedCount = 0;

        try {
            tx.begin();
            Connection conn = tx.getConnection();

            List<Employee> employees = getActiveEmployees(conn);

            for (Employee emp : employees) {
                PayrollRecord record = payrollService.process(emp, LocalDate.now());
                insertPayrollRecord(conn, record);
                processedCount++;

                // Try to insert duplicate (same ID) - will violate PRIMARY KEY constraint
                if (processedCount == 3) {
                    insertPayrollRecord(conn, record); // Duplicate!
                }
            }

            tx.commit();
            fail("Expected SQLException for duplicate key");

        } catch (SQLException e) {
            tx.rollback();
            // Expected - duplicate primary key violation
            assertThat(e.getMessage()).containsIgnoringCase("primary key");
        } finally {
            tx.close();
        }

        // Verify NO payments were committed
        int count = countPayrollRecords();
        assertThat(count).isEqualTo(0);
    }

    @Test
    @Order(7)
    @DisplayName("Should maintain atomicity across multiple failures")
    void testAtomicityAcrossMultipleFailures() throws SQLException {
        // Test 1: Process and fail
        processPayrollWithFailure();
        assertThat(countPayrollRecords()).isEqualTo(0);

        // Test 2: Process and fail again
        processPayrollWithFailure();
        assertThat(countPayrollRecords()).isEqualTo(0);

        // Test 3: Process successfully
        processPayrollSuccessfully();
        assertThat(countPayrollRecords()).isEqualTo(5);
    }

    // -------------------------------------------------------------------------
    // Helper Methods
    // -------------------------------------------------------------------------

    private static void createPayrollTable() throws SQLException {
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("""
                CREATE TABLE IF NOT EXISTS payroll_records (
                    id              VARCHAR(50) PRIMARY KEY,
                    employee_id     VARCHAR(50) NOT NULL,
                    gross_salary    DECIMAL(15, 2) NOT NULL,
                    tax_amount      DECIMAL(15, 2) NOT NULL,
                    net_salary      DECIMAL(15, 2) NOT NULL,
                    payroll_month   DATE NOT NULL,
                    processed_at    TIMESTAMP NOT NULL
                )
            """)) {
            pstmt.executeUpdate();
        }
    }

    private void clearAllTables() throws SQLException {
        try (Connection conn = ConnectionManager.getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM payroll_records")) {
                pstmt.executeUpdate();
            }
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM employees")) {
                pstmt.executeUpdate();
            }
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM departments")) {
                pstmt.executeUpdate();
            }
        }
    }

    private void insertTestData() throws SQLException {
        try (Connection conn = ConnectionManager.getConnection()) {
            // Insert department
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "INSERT INTO departments (id, name, location) VALUES (?, ?, ?)")) {
                pstmt.setString(1, "D001");
                pstmt.setString(2, "Engineering");
                pstmt.setString(3, "SF");
                pstmt.executeUpdate();
            }

            // Insert 5 active employees
            String[][] employees = {
                {"E001", "Alice", "alice@test.com", "120000"},
                {"E002", "Bob", "bob@test.com", "95000"},
                {"E003", "Carol", "carol@test.com", "110000"},
                {"E004", "David", "david@test.com", "85000"},
                {"E005", "Eve", "eve@test.com", "105000"}
            };

            try (PreparedStatement pstmt = conn.prepareStatement(
                    "INSERT INTO employees (id, name, email, department_id, role, salary, status, joining_date) " +
                    "VALUES (?, ?, ?, 'D001', 'ENGINEER', ?, 'ACTIVE', ?)")) {
                for (String[] emp : employees) {
                    pstmt.setString(1, emp[0]);
                    pstmt.setString(2, emp[1]);
                    pstmt.setString(3, emp[2]);
                    pstmt.setBigDecimal(4, new BigDecimal(emp[3]));
                    pstmt.setDate(5, java.sql.Date.valueOf(LocalDate.now().minusYears(1)));
                    pstmt.executeUpdate();
                }
            }
        }
    }

    private void addInactiveEmployee() throws SQLException {
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                    "INSERT INTO employees (id, name, email, department_id, role, salary, status, joining_date) " +
                    "VALUES (?, ?, ?, 'D001', 'ENGINEER', ?, 'INACTIVE', ?)")) {
            pstmt.setString(1, "E006");
            pstmt.setString(2, "Frank");
            pstmt.setString(3, "frank@test.com");
            pstmt.setBigDecimal(4, new BigDecimal("90000"));
            pstmt.setDate(5, java.sql.Date.valueOf(LocalDate.now().minusYears(2)));
            pstmt.executeUpdate();
        }
    }

    private List<Employee> getActiveEmployees(Connection conn) throws SQLException {
        List<Employee> employees = new ArrayList<>();
        try (PreparedStatement pstmt = conn.prepareStatement(
                "SELECT * FROM employees WHERE status = 'ACTIVE' ORDER BY id");
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                employees.add(mapToEmployee(rs));
            }
        }
        return employees;
    }

    private List<Employee> getAllEmployees(Connection conn) throws SQLException {
        List<Employee> employees = new ArrayList<>();
        try (PreparedStatement pstmt = conn.prepareStatement(
                "SELECT * FROM employees ORDER BY id");
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                employees.add(mapToEmployee(rs));
            }
        }
        return employees;
    }

    private Employee mapToEmployee(ResultSet rs) throws SQLException {
        return Employee.builder()
                .id(rs.getString("id"))
                .name(rs.getString("name"))
                .email(rs.getString("email"))
                .departmentId(rs.getString("department_id"))
                .role(Role.valueOf(rs.getString("role")))
                .salary(rs.getBigDecimal("salary"))
                .status(EmployeeStatus.valueOf(rs.getString("status")))
                .joiningDate(rs.getDate("joining_date").toLocalDate())
                .build();
    }

    private void insertPayrollRecord(Connection conn, PayrollRecord record) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO payroll_records (id, employee_id, gross_salary, tax_amount, net_salary, payroll_month, processed_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            pstmt.setString(1, record.getId());
            pstmt.setString(2, record.getEmployeeId());
            pstmt.setBigDecimal(3, record.getGrossSalary());
            pstmt.setBigDecimal(4, record.getTaxAmount());
            pstmt.setBigDecimal(5, record.getNetSalary());
            pstmt.setDate(6, java.sql.Date.valueOf(record.getPayrollMonth()));
            pstmt.setTimestamp(7, java.sql.Timestamp.valueOf(record.getProcessedTimestamp()));
            pstmt.executeUpdate();
        }
    }

    private int countPayrollRecords() throws SQLException {
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT COUNT(*) FROM payroll_records");
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }

    private void processPayrollWithFailure() throws SQLException {
        TransactionManager tx = new TransactionManager();
        PayrollService payrollService = new PayrollServiceImpl();

        try {
            tx.begin();
            Connection conn = tx.getConnection();

            List<Employee> employees = getActiveEmployees(conn);
            for (int i = 0; i < employees.size(); i++) {
                if (i == 2) throw new RuntimeException("Simulated failure");
                PayrollRecord record = payrollService.process(employees.get(i), LocalDate.now());
                insertPayrollRecord(conn, record);
            }

            tx.commit();
        } catch (Exception e) {
            tx.rollback();
        } finally {
            tx.close();
        }
    }

    private void processPayrollSuccessfully() throws SQLException {
        TransactionManager tx = new TransactionManager();
        PayrollService payrollService = new PayrollServiceImpl();

        try {
            tx.begin();
            Connection conn = tx.getConnection();

            List<Employee> employees = getActiveEmployees(conn);
            for (Employee emp : employees) {
                PayrollRecord record = payrollService.process(emp, LocalDate.now());
                insertPayrollRecord(conn, record);
            }

            tx.commit();
        } finally {
            tx.close();
        }
    }

    // -------------------------------------------------------------------------
    // Custom Exceptions
    // -------------------------------------------------------------------------

    static class InsufficientFundsException extends Exception {
        public InsufficientFundsException(String message) {
            super(message);
        }
    }

    static class InvalidEmployeeException extends Exception {
        public InvalidEmployeeException(String message) {
            super(message);
        }
    }

    static class BusinessRuleException extends Exception {
        public BusinessRuleException(String message) {
            super(message);
        }
    }
}

