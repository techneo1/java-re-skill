package com.srikanth.javareskill.repository.jdbc;

import com.srikanth.javareskill.domain.Department;
import com.srikanth.javareskill.domain.Employee;
import com.srikanth.javareskill.domain.enums.EmployeeStatus;
import com.srikanth.javareskill.domain.enums.Role;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for transaction management with commit and rollback.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TransactionManagementTest {

    @BeforeAll
    static void initDatabase() throws SQLException {
        ConnectionManager.initializeDefault();
        SchemaInitializer.createTables();
    }

    @AfterAll
    static void cleanup() {
        ConnectionManager.shutdown();
    }

    @BeforeEach
    void clearTables() throws SQLException {
        SchemaInitializer.dropTables();
        SchemaInitializer.createTables();
    }

    // -------------------------------------------------------------------------
    // Commit Tests
    // -------------------------------------------------------------------------

    @Test
    @Order(1)
    void testSuccessfulCommit() throws SQLException {
        TransactionManager tx = new TransactionManager();
        JdbcDepartmentRepository deptRepo = new JdbcDepartmentRepository();

        try {
            tx.begin();
            Connection conn = tx.getConnection();

            // Insert department within transaction
            try (var pstmt = conn.prepareStatement(
                    "INSERT INTO departments (id, name, location) VALUES (?, ?, ?)")) {
                pstmt.setString(1, "D001");
                pstmt.setString(2, "Engineering");
                pstmt.setString(3, "SF");
                pstmt.executeUpdate();
            }

            // Commit
            tx.commit();

        } finally {
            tx.close();
        }

        // Verify data was persisted
        var dept = deptRepo.findById("D001");
        assertThat(dept).isPresent();
        assertThat(dept.get().getName()).isEqualTo("Engineering");
    }

    @Test
    @Order(2)
    void testMultipleOperationsCommit() throws SQLException {
        TransactionManager tx = new TransactionManager();
        TransactionalJdbcEmployeeRepository empRepo = new TransactionalJdbcEmployeeRepository();

        try {
            tx.begin();
            Connection conn = tx.getConnection();

            // Insert department
            try (var pstmt = conn.prepareStatement(
                    "INSERT INTO departments (id, name, location) VALUES (?, ?, ?)")) {
                pstmt.setString(1, "D001");
                pstmt.setString(2, "Engineering");
                pstmt.setString(3, "SF");
                pstmt.executeUpdate();
            }

            // Insert multiple employees
            for (int i = 1; i <= 3; i++) {
                Employee emp = createEmployee("E00" + i, "Employee " + i, "D001");
                empRepo.save(emp, conn);
            }

            tx.commit();

        } finally {
            tx.close();
        }

        // Verify all employees were saved
        assertThat(empRepo.count()).isEqualTo(3);
    }

    // -------------------------------------------------------------------------
    // Rollback Tests
    // -------------------------------------------------------------------------

    @Test
    @Order(3)
    void testExplicitRollback() throws SQLException {
        TransactionManager tx = new TransactionManager();
        JdbcDepartmentRepository deptRepo = new JdbcDepartmentRepository();

        try {
            tx.begin();
            Connection conn = tx.getConnection();

            // Insert department
            try (var pstmt = conn.prepareStatement(
                    "INSERT INTO departments (id, name, location) VALUES (?, ?, ?)")) {
                pstmt.setString(1, "D001");
                pstmt.setString(2, "Engineering");
                pstmt.setString(3, "SF");
                pstmt.executeUpdate();
            }

            // Explicitly rollback
            tx.rollback();

        } finally {
            tx.close();
        }

        // Verify data was NOT persisted
        assertThat(deptRepo.count()).isEqualTo(0);
    }

    @Test
    @Order(4)
    void testRollbackOnException() {
        TransactionManager tx = new TransactionManager();
        JdbcDepartmentRepository deptRepo = new JdbcDepartmentRepository();
        TransactionalJdbcEmployeeRepository empRepo = new TransactionalJdbcEmployeeRepository();

        try {
            tx.begin();
            Connection conn = tx.getConnection();

            // Insert department
            try (var pstmt = conn.prepareStatement(
                    "INSERT INTO departments (id, name, location) VALUES (?, ?, ?)")) {
                pstmt.setString(1, "D001");
                pstmt.setString(2, "Engineering");
                pstmt.setString(3, "SF");
                pstmt.executeUpdate();
            }

            // Try to insert employee with invalid department (foreign key violation)
            Employee emp = createEmployee("E001", "Alice", "D999");
            empRepo.save(emp, conn);  // Will throw SQLException

            tx.commit();  // Should not reach here
            fail("Expected SQLException");

        } catch (SQLException e) {
            try {
                tx.rollback();
            } catch (SQLException rollbackEx) {
                fail("Rollback failed", rollbackEx);
            }
        } finally {
            tx.close();
        }

        // Verify nothing was committed
        assertThat(deptRepo.count()).isEqualTo(0);
        assertThat(empRepo.count()).isEqualTo(0);
    }

    @Test
    @Order(5)
    void testAutoRollbackOnClose() throws SQLException {
        JdbcDepartmentRepository deptRepo = new JdbcDepartmentRepository();

        TransactionManager tx = new TransactionManager();
        tx.begin();
        Connection conn = tx.getConnection();

        // Insert department
        try (var pstmt = conn.prepareStatement(
                "INSERT INTO departments (id, name, location) VALUES (?, ?, ?)")) {
            pstmt.setString(1, "D001");
            pstmt.setString(2, "Engineering");
            pstmt.setString(3, "SF");
            pstmt.executeUpdate();
        }

        // Close without commit (should auto-rollback)
        tx.close();

        // Verify data was NOT persisted
        assertThat(deptRepo.count()).isEqualTo(0);
    }

    // -------------------------------------------------------------------------
    // Savepoint Tests
    // -------------------------------------------------------------------------

    @Test
    @Order(6)
    void testSavepointRollback() throws SQLException {
        TransactionManager tx = new TransactionManager();
        JdbcDepartmentRepository deptRepo = new JdbcDepartmentRepository();

        try {
            tx.begin();
            Connection conn = tx.getConnection();

            // Insert first department
            try (var pstmt = conn.prepareStatement(
                    "INSERT INTO departments (id, name, location) VALUES (?, ?, ?)")) {
                pstmt.setString(1, "D001");
                pstmt.setString(2, "Engineering");
                pstmt.setString(3, "SF");
                pstmt.executeUpdate();
            }

            // Create savepoint
            tx.setSavepoint("after_first");

            // Insert second department
            try (var pstmt = conn.prepareStatement(
                    "INSERT INTO departments (id, name, location) VALUES (?, ?, ?)")) {
                pstmt.setString(1, "D002");
                pstmt.setString(2, "HR");
                pstmt.setString(3, "NY");
                pstmt.executeUpdate();
            }

            // Rollback to savepoint (discard D002, keep D001)
            tx.rollbackToSavepoint("after_first");

            // Insert third department
            try (var pstmt = conn.prepareStatement(
                    "INSERT INTO departments (id, name, location) VALUES (?, ?, ?)")) {
                pstmt.setString(1, "D003");
                pstmt.setString(2, "Sales");
                pstmt.setString(3, "LA");
                pstmt.executeUpdate();
            }

            tx.commit();

        } finally {
            tx.close();
        }

        // Verify: D001 and D003 exist, D002 does not
        assertThat(deptRepo.count()).isEqualTo(2);
        assertThat(deptRepo.findById("D001")).isPresent();
        assertThat(deptRepo.findById("D002")).isEmpty();
        assertThat(deptRepo.findById("D003")).isPresent();
    }

    // -------------------------------------------------------------------------
    // Atomicity Tests
    // -------------------------------------------------------------------------

    @Test
    @Order(7)
    void testAtomicBatchInsert() throws SQLException {
        TransactionManager tx = new TransactionManager();
        JdbcDepartmentRepository deptRepo = new JdbcDepartmentRepository();

        try {
            tx.begin();
            Connection conn = tx.getConnection();

            // Batch insert departments
            try (var pstmt = conn.prepareStatement(
                    "INSERT INTO departments (id, name, location) VALUES (?, ?, ?)")) {
                
                for (int i = 1; i <= 5; i++) {
                    pstmt.setString(1, "D00" + i);
                    pstmt.setString(2, "Dept " + i);
                    pstmt.setString(3, "Location " + i);
                    pstmt.addBatch();
                }
                
                pstmt.executeBatch();
            }

            tx.commit();

        } finally {
            tx.close();
        }

        // Verify all 5 departments were inserted atomically
        assertThat(deptRepo.count()).isEqualTo(5);
    }

    @Test
    @Order(8)
    void testAtomicBatchInsertWithRollback() {
        TransactionManager tx = new TransactionManager();
        JdbcDepartmentRepository deptRepo = new JdbcDepartmentRepository();

        try {
            tx.begin();
            Connection conn = tx.getConnection();

            // Batch insert - one will fail (duplicate key)
            try (var pstmt = conn.prepareStatement(
                    "INSERT INTO departments (id, name, location) VALUES (?, ?, ?)")) {
                
                pstmt.setString(1, "D001");
                pstmt.setString(2, "Dept 1");
                pstmt.setString(3, "Loc 1");
                pstmt.addBatch();
                
                pstmt.setString(1, "D001");  // Duplicate!
                pstmt.setString(2, "Dept 2");
                pstmt.setString(3, "Loc 2");
                pstmt.addBatch();
                
                pstmt.executeBatch();
            }

            tx.commit();
            fail("Expected SQLException");

        } catch (SQLException e) {
            try {
                tx.rollback();
            } catch (SQLException rollbackEx) {
                fail("Rollback failed", rollbackEx);
            }
        } finally {
            tx.close();
        }

        // Verify nothing was committed (atomicity)
        assertThat(deptRepo.count()).isEqualTo(0);
    }

    // -------------------------------------------------------------------------
    // State Management Tests
    // -------------------------------------------------------------------------

    @Test
    @Order(9)
    void testCannotCommitWithoutTransaction() {
        TransactionManager tx = new TransactionManager();

        assertThatThrownBy(tx::commit)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No active transaction");
    }

    @Test
    @Order(10)
    void testCannotRollbackWithoutTransaction() {
        TransactionManager tx = new TransactionManager();

        assertThatThrownBy(tx::rollback)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No active transaction");
    }

    @Test
    @Order(11)
    void testCannotBeginTwice() throws SQLException {
        TransactionManager tx = new TransactionManager();

        try {
            tx.begin();
            
            assertThatThrownBy(tx::begin)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Transaction already active");

        } finally {
            tx.close();
        }
    }

    @Test
    @Order(12)
    void testTransactionStateTracking() throws SQLException {
        TransactionManager tx = new TransactionManager();

        assertThat(tx.isTransactionActive()).isFalse();

        tx.begin();
        assertThat(tx.isTransactionActive()).isTrue();

        tx.commit();
        assertThat(tx.isTransactionActive()).isFalse();

        tx.close();
        assertThat(tx.isTransactionActive()).isFalse();
    }

    // -------------------------------------------------------------------------
    // Isolation Level Tests
    // -------------------------------------------------------------------------

    @Test
    @Order(13)
    void testCustomIsolationLevel() throws SQLException {
        TransactionManager tx = new TransactionManager();

        try {
            tx.begin(Connection.TRANSACTION_SERIALIZABLE);
            Connection conn = tx.getConnection();

            assertThat(conn.getTransactionIsolation())
                    .isEqualTo(Connection.TRANSACTION_SERIALIZABLE);

            tx.commit();

        } finally {
            tx.close();
        }
    }

    // -------------------------------------------------------------------------
    // Helper Methods
    // -------------------------------------------------------------------------

    private Employee createEmployee(String id, String name, String deptId) {
        return Employee.builder()
                .id(id)
                .name(name)
                .email(name.toLowerCase().replace(" ", ".") + "@example.com")
                .departmentId(deptId)
                .role(Role.ENGINEER)
                .salary(new BigDecimal("100000"))
                .status(EmployeeStatus.ACTIVE)
                .joiningDate(LocalDate.now())
                .build();
    }
}

