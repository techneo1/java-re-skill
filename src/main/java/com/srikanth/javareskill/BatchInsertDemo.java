package com.srikanth.javareskill;

import com.srikanth.javareskill.domain.Employee;
import com.srikanth.javareskill.domain.PayrollRecord;
import com.srikanth.javareskill.domain.enums.EmployeeStatus;
import com.srikanth.javareskill.domain.enums.Role;
import com.srikanth.javareskill.payroll.PayrollService;
import com.srikanth.javareskill.payroll.impl.PayrollServiceImpl;
import com.srikanth.javareskill.repository.jdbc.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Demonstrates batch insert performance benefits for payroll processing.
 *
 * <p>This application shows the dramatic performance improvement when using
 * JDBC batch operations instead of individual inserts.</p>
 *
 * <h2>Demonstrations</h2>
 * <ol>
 *   <li><b>Individual Inserts</b> - One SQL statement per record (SLOW)</li>
 *   <li><b>Batch Insert</b> - All records in one batch (FAST)</li>
 *   <li><b>Chunked Batch</b> - Large datasets in manageable chunks (OPTIMAL)</li>
 *   <li><b>Transactional Batch</b> - Batch with rollback capability (SAFE & FAST)</li>
 * </ol>
 *
 * <h2>Expected Performance</h2>
 * <pre>
 * Records    Individual    Batch      Speedup
 * ───────────────────────────────────────────
 * 10         ~10ms        ~2ms       5x
 * 100        ~100ms       ~10ms      10x
 * 1,000      ~1,000ms     ~50ms      20x
 * 10,000     ~10,000ms    ~300ms     33x
 * </pre>
 */
public class BatchInsertDemo {

    public static void main(String[] args) {
        try {
            System.out.println("╔══════════════════════════════════════════════════════════╗");
            System.out.println("║        Batch Insert Performance Demonstration           ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝\n");

            // Initialize database
            ConnectionManager.initializeDefault();
            SchemaInitializer.createTables();
            createPayrollTable();
            setupTestData();

            System.out.println("✓ Database initialized");
            System.out.println("✓ Test data created (1000 employees)\n");

            // Run demonstrations
            demonstrateIndividualInserts();
            
            System.out.println("\n" + "=".repeat(60) + "\n");
            demonstrateBatchInsert();
            
            System.out.println("\n" + "=".repeat(60) + "\n");
            demonstrateChunkedBatch();
            
            System.out.println("\n" + "=".repeat(60) + "\n");
            demonstrateTransactionalBatch();

            System.out.println("\n" + "=".repeat(60));
            System.out.println("\n✅ All Batch Insert Demonstrations Complete!");
            printSummary();

        } catch (Exception e) {
            System.err.println("\n❌ Demo failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            ConnectionManager.shutdown();
        }
    }

    /**
     * Demo 1: Individual inserts (SLOW - baseline for comparison).
     */
    private static void demonstrateIndividualInserts() throws SQLException {
        System.out.println("🐌 DEMO 1: Individual Inserts (Baseline)");
        System.out.println("─".repeat(60));

        clearPayrollTable();

        PayrollService payrollService = new PayrollServiceImpl();
        JdbcPayrollRecordRepository payrollRepo = new JdbcPayrollRecordRepository();

        // Get first 100 employees
        List<Employee> employees = getEmployees(100);
        List<PayrollRecord> records = new ArrayList<>();

        // Calculate all payroll records first
        for (Employee emp : employees) {
            PayrollRecord record = payrollService.process(emp, LocalDate.now());
            records.add(record);
        }

        System.out.println("Processing " + records.size() + " payroll records...");
        System.out.println("Method: Individual INSERT statements\n");

        // Time individual inserts
        long startTime = System.currentTimeMillis();

        for (PayrollRecord record : records) {
            payrollRepo.insert(record);  // One SQL per record
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("✓ Completed: " + records.size() + " records inserted");
        System.out.println("⏱  Time taken: " + duration + " ms");
        System.out.println("📊 Average: " + String.format("%.2f", duration / (double) records.size()) + " ms per record");
        System.out.println("🔢 Database calls: " + records.size() + " (one per record)");
        
        // Verify
        int count = payrollRepo.count();
        System.out.println("✓ Verification: " + count + " records in database");
    }

    /**
     * Demo 2: Batch insert (FAST - 10-100x faster).
     */
    private static void demonstrateBatchInsert() throws SQLException {
        System.out.println("🚀 DEMO 2: Batch Insert (High Performance)");
        System.out.println("─".repeat(60));

        clearPayrollTable();

        PayrollService payrollService = new PayrollServiceImpl();
        JdbcPayrollRecordRepository payrollRepo = new JdbcPayrollRecordRepository();

        // Get first 100 employees
        List<Employee> employees = getEmployees(100);
        List<PayrollRecord> records = new ArrayList<>();

        // Calculate all payroll records
        for (Employee emp : employees) {
            PayrollRecord record = payrollService.process(emp, LocalDate.now());
            records.add(record);
        }

        System.out.println("Processing " + records.size() + " payroll records...");
        System.out.println("Method: Batch INSERT (single operation)\n");

        // Time batch insert
        long startTime = System.currentTimeMillis();

        int[] results = payrollRepo.batchInsert(records);  // One batch operation

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("✓ Completed: " + results.length + " records inserted");
        System.out.println("⏱  Time taken: " + duration + " ms");
        System.out.println("📊 Average: " + String.format("%.2f", duration / (double) records.size()) + " ms per record");
        System.out.println("🔢 Database calls: 1 (single batch)");
        System.out.println("⚡ Performance: ~10-20x faster than individual inserts");

        // Verify
        int count = payrollRepo.count();
        System.out.println("✓ Verification: " + count + " records in database");
    }

    /**
     * Demo 3: Chunked batch insert (OPTIMAL for large datasets).
     */
    private static void demonstrateChunkedBatch() throws SQLException {
        System.out.println("🎯 DEMO 3: Chunked Batch Insert (Optimal for Large Data)");
        System.out.println("─".repeat(60));

        clearPayrollTable();

        PayrollService payrollService = new PayrollServiceImpl();
        JdbcPayrollRecordRepository payrollRepo = new JdbcPayrollRecordRepository();

        // Get ALL 1000 employees
        List<Employee> employees = getEmployees(1000);
        List<PayrollRecord> records = new ArrayList<>();

        System.out.println("Processing " + employees.size() + " payroll records...");
        System.out.println("Method: Chunked batch (100 records per chunk)\n");

        // Calculate all payroll records
        for (Employee emp : employees) {
            PayrollRecord record = payrollService.process(emp, LocalDate.now());
            records.add(record);
        }

        // Time chunked batch insert
        long startTime = System.currentTimeMillis();

        int totalInserted = payrollRepo.batchInsertInChunks(records, 100);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("✓ Completed: " + totalInserted + " records inserted");
        System.out.println("⏱  Time taken: " + duration + " ms");
        System.out.println("📊 Average: " + String.format("%.2f", duration / (double) records.size()) + " ms per record");
        System.out.println("🔢 Database calls: " + (records.size() / 100) + " batches");
        System.out.println("⚡ Performance: Optimal for large datasets (10,000+ records)");
        System.out.println("💡 Benefit: Prevents memory issues and long transactions");

        // Verify
        int count = payrollRepo.count();
        System.out.println("✓ Verification: " + count + " records in database");
    }

    /**
     * Demo 4: Transactional batch insert with rollback capability.
     */
    private static void demonstrateTransactionalBatch() throws SQLException {
        System.out.println("🔒 DEMO 4: Transactional Batch Insert (Safe & Fast)");
        System.out.println("─".repeat(60));

        clearPayrollTable();

        PayrollService payrollService = new PayrollServiceImpl();
        JdbcPayrollRecordRepository payrollRepo = new JdbcPayrollRecordRepository();
        TransactionManager tx = new TransactionManager();

        // Get 200 employees
        List<Employee> employees = getEmployees(200);
        List<PayrollRecord> records = new ArrayList<>();

        for (Employee emp : employees) {
            PayrollRecord record = payrollService.process(emp, LocalDate.now());
            records.add(record);
        }

        System.out.println("Processing " + records.size() + " payroll records...");
        System.out.println("Method: Batch INSERT within transaction\n");

        try {
            // Time transactional batch insert
            long startTime = System.currentTimeMillis();

            tx.begin();
            Connection conn = tx.getConnection();

            int[] results = payrollRepo.batchInsert(records, conn);

            tx.commit();

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            System.out.println("✓ Completed: " + results.length + " records inserted");
            System.out.println("⏱  Time taken: " + duration + " ms");
            System.out.println("📊 Average: " + String.format("%.2f", duration / (double) records.size()) + " ms per record");
            System.out.println("🔒 Transaction: COMMITTED (all records persisted)");
            System.out.println("⚡ Performance: Fast batch + ACID guarantees");
            System.out.println("✅ Rollback ready: Any failure would undo ALL inserts");

        } catch (Exception e) {
            tx.rollback();
            System.out.println("❌ Transaction ROLLED BACK: " + e.getMessage());
        } finally {
            tx.close();
        }

        // Verify
        int count = payrollRepo.count();
        System.out.println("✓ Verification: " + count + " records in database");
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

    private static void setupTestData() throws SQLException {
        TransactionManager tx = new TransactionManager();
        try {
            tx.begin();
            Connection conn = tx.getConnection();

            // Insert department
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "INSERT INTO departments (id, name, location) VALUES (?, ?, ?)")) {
                pstmt.setString(1, "D001");
                pstmt.setString(2, "Engineering");
                pstmt.setString(3, "SF");
                pstmt.executeUpdate();
            }

            // Insert 1000 employees for performance testing
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "INSERT INTO employees (id, name, email, department_id, role, salary, status, joining_date) " +
                    "VALUES (?, ?, ?, 'D001', ?, ?, 'ACTIVE', ?)")) {

                Role[] roles = {Role.ENGINEER, Role.SENIOR_ENGINEER, Role.MANAGER, Role.ANALYST};

                for (int i = 1; i <= 1000; i++) {
                    pstmt.setString(1, String.format("E%04d", i));
                    pstmt.setString(2, "Employee " + i);
                    pstmt.setString(3, "emp" + i + "@company.com");
                    pstmt.setString(4, roles[i % 4].name());
                    pstmt.setBigDecimal(5, new java.math.BigDecimal(80000 + (i % 5) * 10000));
                    pstmt.setDate(6, java.sql.Date.valueOf(LocalDate.now().minusYears(2)));
                    pstmt.addBatch();

                    // Execute batch every 100 records
                    if (i % 100 == 0) {
                        pstmt.executeBatch();
                    }
                }

                pstmt.executeBatch();  // Execute remaining
            }

            tx.commit();

        } finally {
            tx.close();
        }
    }

    private static List<Employee> getEmployees(int limit) throws SQLException {
        List<Employee> employees = new ArrayList<>();

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT * FROM employees WHERE status = 'ACTIVE' ORDER BY id LIMIT ?")) {

            pstmt.setInt(1, limit);

            try (var rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    employees.add(Employee.builder()
                            .id(rs.getString("id"))
                            .name(rs.getString("name"))
                            .email(rs.getString("email"))
                            .departmentId(rs.getString("department_id"))
                            .role(Role.valueOf(rs.getString("role")))
                            .salary(rs.getBigDecimal("salary"))
                            .status(EmployeeStatus.valueOf(rs.getString("status")))
                            .joiningDate(rs.getDate("joining_date").toLocalDate())
                            .build());
                }
            }
        }

        return employees;
    }

    private static void clearPayrollTable() throws SQLException {
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("DELETE FROM payroll_records")) {
            pstmt.executeUpdate();
        }
    }

    private static void printSummary() {
        System.out.println("\n📊 Performance Summary");
        System.out.println("─".repeat(60));
        System.out.println();
        System.out.println("Method               Records    Expected Time    Speedup");
        System.out.println("──────────────────────────────────────────────────────────");
        System.out.println("Individual Inserts   100        ~100 ms          1x (baseline)");
        System.out.println("Batch Insert         100        ~10 ms           10x faster");
        System.out.println("Chunked Batch        1,000      ~50 ms           20x faster");
        System.out.println("Transactional Batch  200        ~20 ms           10x faster");
        System.out.println();
        System.out.println("Key Takeaways:");
        System.out.println("  ✓ Batch operations are 10-100x faster");
        System.out.println("  ✓ Use chunked batches for large datasets (10,000+ records)");
        System.out.println("  ✓ Combine with transactions for ACID guarantees");
        System.out.println("  ✓ Reduced network overhead = Better performance");
        System.out.println();
    }
}

