package com.srikanth.javareskill;

import com.srikanth.javareskill.domain.Department;
import com.srikanth.javareskill.domain.Employee;
import com.srikanth.javareskill.domain.enums.EmployeeStatus;
import com.srikanth.javareskill.domain.enums.Role;
import com.srikanth.javareskill.repository.jdbc.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * Demonstrates manual transaction management with commit and rollback.
 *
 * <p>This application showcases:</p>
 * <ul>
 *   <li><b>Atomic operations</b> – multiple operations succeed or fail together</li>
 *   <li><b>Manual commit</b> – explicit commit after successful operations</li>
 *   <li><b>Manual rollback</b> – undo changes when errors occur</li>
 *   <li><b>Savepoints</b> – partial rollback within a transaction</li>
 *   <li><b>Isolation levels</b> – controlling concurrent access</li>
 * </ul>
 *
 * <h2>ACID Properties Demonstrated</h2>
 * <ul>
 *   <li><b>Atomicity</b>: All-or-nothing execution</li>
 *   <li><b>Consistency</b>: Database constraints enforced</li>
 *   <li><b>Isolation</b>: Transactions don't interfere with each other</li>
 *   <li><b>Durability</b>: Committed data is permanent</li>
 * </ul>
 */
public class TransactionDemoApp {

    public static void main(String[] args) {
        try {
            // Initialize database
            System.out.println("=== Transaction Management Demo ===\n");
            ConnectionManager.initializeDefault();
            SchemaInitializer.createTables();
            System.out.println("✓ Database initialized\n");

            // Run demonstrations
            demonstrateSuccessfulTransaction();
            demonstrateRollbackOnError();
            demonstrateSavepoints();
            demonstrateAtomicity();

            System.out.println("\n=== All Transaction Demos Complete ===");

        } catch (Exception e) {
            System.err.println("Demo failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            ConnectionManager.shutdown();
            System.out.println("✓ Database shutdown complete");
        }
    }

    /**
     * Demonstrates a successful transaction with manual commit.
     */
    private static void demonstrateSuccessfulTransaction() throws SQLException {
        System.out.println("--- Demo 1: Successful Transaction (Commit) ---");

        TransactionManager tx = new TransactionManager();
        JdbcDepartmentRepository deptRepo = new JdbcDepartmentRepository();
        TransactionalJdbcEmployeeRepository empRepo = new TransactionalJdbcEmployeeRepository();

        try {
            tx.begin();
            Connection conn = tx.getConnection();

            // Create department
            Department dept = Department.builder()
                    .id("D001")
                    .name("Engineering")
                    .location("San Francisco")
                    .build();
            
            // Insert department (within transaction)
            try (var pstmt = conn.prepareStatement(
                    "INSERT INTO departments (id, name, location) VALUES (?, ?, ?)")) {
                pstmt.setString(1, dept.getId());
                pstmt.setString(2, dept.getName());
                pstmt.setString(3, dept.getLocation());
                pstmt.executeUpdate();
            }

            System.out.println("  → Department inserted (not yet committed)");

            // Create employee
            Employee emp = Employee.builder()
                    .id("E001")
                    .name("Alice Johnson")
                    .email("alice@example.com")
                    .departmentId("D001")
                    .role(Role.ENGINEER)
                    .salary(new BigDecimal("100000"))
                    .status(EmployeeStatus.ACTIVE)
                    .joiningDate(LocalDate.now())
                    .build();

            empRepo.save(emp, conn);
            System.out.println("  → Employee inserted (not yet committed)");

            // Commit transaction
            tx.commit();
            System.out.println("  ✓ Transaction committed successfully");

            // Verify data persisted
            System.out.println("  ✓ Data is now permanently stored");

        } catch (Exception e) {
            tx.rollback();
            System.out.println("  ✗ Transaction rolled back due to: " + e.getMessage());
            throw e;
        } finally {
            tx.close();
        }

        System.out.println();
    }

    /**
     * Demonstrates automatic rollback when an error occurs.
     */
    private static void demonstrateRollbackOnError() throws SQLException {
        System.out.println("--- Demo 2: Automatic Rollback on Error ---");

        // Clean up from previous demo
        SchemaInitializer.dropTables();
        SchemaInitializer.createTables();

        TransactionManager tx = new TransactionManager();
        JdbcDepartmentRepository deptRepo = new JdbcDepartmentRepository();
        TransactionalJdbcEmployeeRepository empRepo = new TransactionalJdbcEmployeeRepository();

        try {
            tx.begin();
            Connection conn = tx.getConnection();

            // Insert first department (will succeed)
            Department dept1 = Department.builder()
                    .id("D001")
                    .name("Engineering")
                    .location("SF")
                    .build();

            try (var pstmt = conn.prepareStatement(
                    "INSERT INTO departments (id, name, location) VALUES (?, ?, ?)")) {
                pstmt.setString(1, dept1.getId());
                pstmt.setString(2, dept1.getName());
                pstmt.setString(3, dept1.getLocation());
                pstmt.executeUpdate();
            }

            System.out.println("  → First department inserted");

            // Try to insert employee with non-existent department (will fail)
            Employee emp = Employee.builder()
                    .id("E001")
                    .name("Bob Smith")
                    .email("bob@example.com")
                    .departmentId("D999")  // Foreign key violation!
                    .role(Role.ENGINEER)
                    .salary(new BigDecimal("95000"))
                    .status(EmployeeStatus.ACTIVE)
                    .joiningDate(LocalDate.now())
                    .build();

            System.out.println("  → Attempting to insert employee with invalid department...");
            empRepo.save(emp, conn);  // This will throw SQLException

            // This line will never execute
            tx.commit();

        } catch (SQLException e) {
            tx.rollback();
            System.out.println("  ✓ Transaction rolled back automatically");
            System.out.println("  ✓ First department insert was also rolled back");
            System.out.println("  → Reason: " + e.getMessage());
        } finally {
            tx.close();
        }

        // Verify nothing was committed
        int deptCount = new JdbcDepartmentRepository().count();
        System.out.println("  ✓ Verification: Department count = " + deptCount + " (expected 0)");

        System.out.println();
    }

    /**
     * Demonstrates savepoints for partial rollback.
     */
    private static void demonstrateSavepoints() throws SQLException {
        System.out.println("--- Demo 3: Savepoints (Partial Rollback) ---");

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
            System.out.println("  → Department D001 inserted");

            // Create savepoint after first department
            tx.setSavepoint("after_first_dept");
            System.out.println("  → Savepoint 'after_first_dept' created");

            // Insert second department
            try (var pstmt = conn.prepareStatement(
                    "INSERT INTO departments (id, name, location) VALUES (?, ?, ?)")) {
                pstmt.setString(1, "D002");
                pstmt.setString(2, "HR");
                pstmt.setString(3, "NY");
                pstmt.executeUpdate();
            }
            System.out.println("  → Department D002 inserted");

            // Simulate an error - rollback to savepoint
            System.out.println("  → Simulating error, rolling back to savepoint...");
            tx.rollbackToSavepoint("after_first_dept");
            System.out.println("  ✓ Rolled back to savepoint");
            System.out.println("  ✓ D001 is kept, D002 is discarded");

            // Insert different second department
            try (var pstmt = conn.prepareStatement(
                    "INSERT INTO departments (id, name, location) VALUES (?, ?, ?)")) {
                pstmt.setString(1, "D003");
                pstmt.setString(2, "Sales");
                pstmt.setString(3, "LA");
                pstmt.executeUpdate();
            }
            System.out.println("  → Department D003 inserted instead");

            // Commit the transaction
            tx.commit();
            System.out.println("  ✓ Transaction committed");

            // Verify final state
            int count = deptRepo.count();
            System.out.println("  ✓ Final department count: " + count + " (D001 and D003)");

        } catch (Exception e) {
            tx.rollback();
            System.out.println("  ✗ Transaction rolled back: " + e.getMessage());
            throw e;
        } finally {
            tx.close();
        }

        System.out.println();
    }

    /**
     * Demonstrates atomicity - all operations succeed or all fail.
     */
    private static void demonstrateAtomicity() throws SQLException {
        System.out.println("--- Demo 4: Atomicity (All-or-Nothing) ---");

        // Clean up
        SchemaInitializer.dropTables();
        SchemaInitializer.createTables();

        TransactionManager tx = new TransactionManager();

        try {
            tx.begin();
            Connection conn = tx.getConnection();

            // Batch insert multiple departments
            System.out.println("  → Inserting multiple departments atomically...");
            
            String[] deptIds = {"D001", "D002", "D003", "D004", "D005"};
            String[] deptNames = {"Engineering", "HR", "Sales", "Marketing", "Finance"};
            
            try (var pstmt = conn.prepareStatement(
                    "INSERT INTO departments (id, name, location) VALUES (?, ?, ?)")) {
                
                for (int i = 0; i < deptIds.length; i++) {
                    pstmt.setString(1, deptIds[i]);
                    pstmt.setString(2, deptNames[i]);
                    pstmt.setString(3, "HQ");
                    pstmt.addBatch();
                }
                
                int[] results = pstmt.executeBatch();
                System.out.println("  → " + results.length + " departments inserted");
            }

            // Verify within transaction
            try (var stmt = conn.createStatement();
                 var rs = stmt.executeQuery("SELECT COUNT(*) FROM departments")) {
                if (rs.next()) {
                    System.out.println("  → Count before commit: " + rs.getInt(1));
                }
            }

            tx.commit();
            System.out.println("  ✓ All departments committed atomically");

            // Verify after commit
            int finalCount = new JdbcDepartmentRepository().count();
            System.out.println("  ✓ Final count: " + finalCount);

        } catch (Exception e) {
            tx.rollback();
            System.out.println("  ✗ All changes rolled back: " + e.getMessage());
            throw e;
        } finally {
            tx.close();
        }

        System.out.println();
    }
}

