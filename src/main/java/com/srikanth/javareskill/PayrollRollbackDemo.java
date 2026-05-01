package com.srikanth.javareskill;

import com.srikanth.javareskill.domain.Department;
import com.srikanth.javareskill.domain.Employee;
import com.srikanth.javareskill.domain.PayrollRecord;
import com.srikanth.javareskill.domain.enums.EmployeeStatus;
import com.srikanth.javareskill.domain.enums.Role;
import com.srikanth.javareskill.payroll.PayrollService;
import com.srikanth.javareskill.payroll.impl.PayrollServiceImpl;
import com.srikanth.javareskill.repository.jdbc.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Demonstrates transaction rollback on failure during payroll processing.
 *
 * <p>This demonstration shows a realistic scenario where:</p>
 * <ul>
 *   <li>Multiple employees need to be paid in a single payroll run</li>
 *   <li>A failure occurs mid-process (e.g., insufficient funds, validation error)</li>
 *   <li>The entire transaction rolls back to prevent partial payments</li>
 *   <li>Database consistency is maintained (all-or-nothing)</li>
 * </ul>
 *
 * <h2>Scenarios Demonstrated</h2>
 * <ol>
 *   <li><b>Successful Payroll</b> - All employees paid, transaction committed</li>
 *   <li><b>Insufficient Funds</b> - Mid-process failure, complete rollback</li>
 *   <li><b>Invalid Employee</b> - Validation failure, complete rollback</li>
 *   <li><b>Account Limit Exceeded</b> - Business rule violation, complete rollback</li>
 * </ol>
 */
public class PayrollRollbackDemo {

    // Simulated company bank account balance
    private static final BigDecimal COMPANY_BALANCE = new BigDecimal("500000.00");
    private static BigDecimal currentBalance = COMPANY_BALANCE;

    public static void main(String[] args) {
        try {
            // Initialize database
            System.out.println("╔══════════════════════════════════════════════════════════╗");
            System.out.println("║   Payroll Processing - Rollback on Failure Demo         ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝\n");
            
            ConnectionManager.initializeDefault();
            SchemaInitializer.createTables();
            createPayrollTable();
            
            System.out.println("✓ Database initialized");
            System.out.println("✓ Company balance: $" + currentBalance + "\n");

            // Setup test data
            setupTestData();

            // Run demonstrations
            System.out.println("\n" + "=".repeat(60) + "\n");
            demonstrateSuccessfulPayroll();
            
            System.out.println("\n" + "=".repeat(60) + "\n");
            demonstrateInsufficientFunds();
            
            System.out.println("\n" + "=".repeat(60) + "\n");
            demonstrateInvalidEmployeeStatus();
            
            System.out.println("\n" + "=".repeat(60) + "\n");
            demonstrateBusinessRuleViolation();

            System.out.println("\n" + "=".repeat(60));
            System.out.println("\n✅ All Payroll Rollback Demonstrations Complete!");
            System.out.println("\nKey Takeaway: Transactions ensure ATOMICITY");
            System.out.println("→ Either ALL employees get paid, or NONE do");
            System.out.println("→ No partial payments = Database consistency maintained\n");

        } catch (Exception e) {
            System.err.println("\n❌ Demo failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            ConnectionManager.shutdown();
        }
    }

    /**
     * Scenario 1: Successful payroll processing.
     * All employees paid, transaction committed successfully.
     */
    private static void demonstrateSuccessfulPayroll() throws SQLException {
        System.out.println("📋 SCENARIO 1: Successful Payroll Processing");
        System.out.println("─".repeat(60));
        
        resetDatabase();
        currentBalance = COMPANY_BALANCE;
        
        TransactionManager tx = new TransactionManager();
        PayrollService payrollService = new PayrollServiceImpl();
        
        try {
            tx.begin();
            Connection conn = tx.getConnection();
            
            // Get active employees
            List<Employee> employees = getActiveEmployees(conn);
            System.out.println("Processing payroll for " + employees.size() + " employees...\n");
            
            BigDecimal totalPayroll = BigDecimal.ZERO;
            
            for (Employee emp : employees) {
                // Calculate payroll
                PayrollRecord record = payrollService.process(emp, LocalDate.now());
                
                // Check company has sufficient funds
                if (currentBalance.compareTo(record.getNetSalary()) < 0) {
                    throw new InsufficientFundsException(
                        "Insufficient funds for employee: " + emp.getName());
                }
                
                // Deduct from company account
                currentBalance = currentBalance.subtract(record.getNetSalary());
                
                // Insert payroll record
                insertPayrollRecord(conn, record);
                
                // Mark employee as paid
                markEmployeeAsPaid(conn, emp.getId());
                
                totalPayroll = totalPayroll.add(record.getNetSalary());
                
                System.out.printf("  ✓ %s: $%s (Balance: $%s)%n", 
                    emp.getName(), 
                    record.getNetSalary(), 
                    currentBalance);
            }
            
            // Commit transaction
            tx.commit();
            
            System.out.println("\n✅ PAYROLL COMMITTED");
            System.out.println("  → Total paid: $" + totalPayroll);
            System.out.println("  → Remaining balance: $" + currentBalance);
            System.out.println("  → All " + employees.size() + " employees successfully paid");
            
            // Verify
            int paidCount = countPayrollRecords(null);
            System.out.println("  → Verification: " + paidCount + " payroll records in database");
            
        } catch (Exception e) {
            tx.rollback();
            System.out.println("\n❌ PAYROLL ROLLED BACK: " + e.getMessage());
        } finally {
            tx.close();
        }
    }

    /**
     * Scenario 2: Insufficient funds during payroll processing.
     * Transaction rolls back to prevent partial payments.
     */
    private static void demonstrateInsufficientFunds() throws SQLException {
        System.out.println("💰 SCENARIO 2: Insufficient Funds - Mid-Process Failure");
        System.out.println("─".repeat(60));
        
        resetDatabase();
        // Set artificially low balance to trigger failure
        currentBalance = new BigDecimal("150000.00");
        
        System.out.println("Starting balance: $" + currentBalance + " (INSUFFICIENT)");
        System.out.println("Processing payroll for 5 employees...\n");
        
        TransactionManager tx = new TransactionManager();
        PayrollService payrollService = new PayrollServiceImpl();
        
        int processedCount = 0;
        String failedEmployee = null;
        
        try {
            tx.begin();
            Connection conn = tx.getConnection();
            
            List<Employee> employees = getActiveEmployees(conn);
            BigDecimal totalAttempted = BigDecimal.ZERO;
            
            for (Employee emp : employees) {
                PayrollRecord record = payrollService.process(emp, LocalDate.now());
                
                // Check funds BEFORE payment
                if (currentBalance.compareTo(record.getNetSalary()) < 0) {
                    failedEmployee = emp.getName();
                    throw new InsufficientFundsException(
                        String.format("Cannot pay %s: Need $%s but only have $%s remaining", 
                            emp.getName(), record.getNetSalary(), currentBalance));
                }
                
                currentBalance = currentBalance.subtract(record.getNetSalary());
                insertPayrollRecord(conn, record);
                markEmployeeAsPaid(conn, emp.getId());
                
                processedCount++;
                totalAttempted = totalAttempted.add(record.getNetSalary());
                
                System.out.printf("  ✓ %s: $%s (Temp balance: $%s)%n", 
                    emp.getName(), 
                    record.getNetSalary(), 
                    currentBalance);
            }
            
            tx.commit();
            
        } catch (InsufficientFundsException e) {
            // Rollback transaction
            tx.rollback();
            currentBalance = new BigDecimal("150000.00"); // Restore original
            
            System.out.println("\n❌ INSUFFICIENT FUNDS!");
            System.out.println("  → Failed at employee: " + failedEmployee);
            System.out.println("  → Successfully processed: " + processedCount + " employees");
            System.out.println("  → But transaction ROLLED BACK");
            System.out.println("\n🔄 ROLLBACK RESULT:");
            System.out.println("  → Company balance restored: $" + currentBalance);
            
            // Verify no payments were made
            int paidCount = countPayrollRecords(null);
            System.out.println("  → Payroll records in database: " + paidCount);
            System.out.println("  → ✅ NO PARTIAL PAYMENTS - Atomicity preserved!");
            
        } catch (Exception e) {
            tx.rollback();
            System.out.println("\n❌ UNEXPECTED ERROR: " + e.getMessage());
        } finally {
            tx.close();
        }
    }

    /**
     * Scenario 3: Invalid employee status during payroll processing.
     * Shows validation failure causing complete rollback.
     */
    private static void demonstrateInvalidEmployeeStatus() throws SQLException {
        System.out.println("⚠️  SCENARIO 3: Invalid Employee Status - Validation Failure");
        System.out.println("─".repeat(60));
        
        resetDatabase();
        currentBalance = COMPANY_BALANCE;
        
        // Add an inactive employee to trigger failure
        addInactiveEmployee();
        
        System.out.println("Processing payroll (includes 1 INACTIVE employee)...\n");
        
        TransactionManager tx = new TransactionManager();
        PayrollService payrollService = new PayrollServiceImpl();
        
        int processedCount = 0;
        String failedEmployee = null;
        
        try {
            tx.begin();
            Connection conn = tx.getConnection();
            
            // Get ALL employees (including inactive)
            List<Employee> employees = getAllEmployees(conn);
            
            for (Employee emp : employees) {
                // Validate employee status
                if (emp.getStatus() != EmployeeStatus.ACTIVE) {
                    failedEmployee = emp.getName();
                    throw new InvalidEmployeeException(
                        "Cannot process payroll for INACTIVE employee: " + emp.getName());
                }
                
                PayrollRecord record = payrollService.process(emp, LocalDate.now());
                currentBalance = currentBalance.subtract(record.getNetSalary());
                insertPayrollRecord(conn, record);
                markEmployeeAsPaid(conn, emp.getId());
                
                processedCount++;
                System.out.printf("  ✓ %s: $%s%n", emp.getName(), record.getNetSalary());
            }
            
            tx.commit();
            
        } catch (InvalidEmployeeException e) {
            tx.rollback();
            currentBalance = COMPANY_BALANCE; // Restore
            
            System.out.println("\n❌ VALIDATION FAILED!");
            System.out.println("  → Failed at employee: " + failedEmployee + " (INACTIVE)");
            System.out.println("  → Successfully processed: " + processedCount + " employees");
            System.out.println("  → But transaction ROLLED BACK");
            System.out.println("\n🔄 ROLLBACK RESULT:");
            
            int paidCount = countPayrollRecords(null);
            System.out.println("  → Payroll records in database: " + paidCount);
            System.out.println("  → Company balance restored: $" + currentBalance);
            System.out.println("  → ✅ NO ONE PAID - Invalid employee prevented entire payroll!");
            
        } catch (Exception e) {
            tx.rollback();
            System.out.println("\n❌ UNEXPECTED ERROR: " + e.getMessage());
        } finally {
            tx.close();
        }
    }

    /**
     * Scenario 4: Business rule violation (e.g., daily payment limit exceeded).
     * Shows business logic enforcement causing rollback.
     */
    private static void demonstrateBusinessRuleViolation() throws SQLException {
        System.out.println("🚫 SCENARIO 4: Business Rule Violation - Payment Limit Exceeded");
        System.out.println("─".repeat(60));
        
        resetDatabase();
        currentBalance = COMPANY_BALANCE;
        
        final BigDecimal DAILY_PAYMENT_LIMIT = new BigDecimal("400000.00");
        
        System.out.println("Daily payment limit: $" + DAILY_PAYMENT_LIMIT);
        System.out.println("Processing payroll for 5 employees...\n");
        
        TransactionManager tx = new TransactionManager();
        PayrollService payrollService = new PayrollServiceImpl();
        
        int processedCount = 0;
        
        try {
            tx.begin();
            Connection conn = tx.getConnection();
            
            List<Employee> employees = getActiveEmployees(conn);
            BigDecimal totalPayroll = BigDecimal.ZERO;
            
            for (Employee emp : employees) {
                PayrollRecord record = payrollService.process(emp, LocalDate.now());
                
                // Check business rule: daily payment limit
                BigDecimal projectedTotal = totalPayroll.add(record.getNetSalary());
                if (projectedTotal.compareTo(DAILY_PAYMENT_LIMIT) > 0) {
                    throw new BusinessRuleException(
                        String.format("Daily payment limit exceeded! Limit: $%s, Attempted: $%s",
                            DAILY_PAYMENT_LIMIT, projectedTotal));
                }
                
                currentBalance = currentBalance.subtract(record.getNetSalary());
                insertPayrollRecord(conn, record);
                markEmployeeAsPaid(conn, emp.getId());
                
                totalPayroll = totalPayroll.add(record.getNetSalary());
                processedCount++;
                
                System.out.printf("  ✓ %s: $%s (Total: $%s)%n", 
                    emp.getName(), 
                    record.getNetSalary(), 
                    totalPayroll);
            }
            
            tx.commit();
            
        } catch (BusinessRuleException e) {
            tx.rollback();
            currentBalance = COMPANY_BALANCE; // Restore
            
            System.out.println("\n❌ BUSINESS RULE VIOLATED!");
            System.out.println("  → Reason: " + e.getMessage());
            System.out.println("  → Successfully processed: " + processedCount + " employees");
            System.out.println("  → But transaction ROLLED BACK");
            System.out.println("\n🔄 ROLLBACK RESULT:");
            
            int paidCount = countPayrollRecords(null);
            System.out.println("  → Payroll records in database: " + paidCount);
            System.out.println("  → Company balance restored: $" + currentBalance);
            System.out.println("  → ✅ Business rule enforced - No payments made!");
            
        } catch (Exception e) {
            tx.rollback();
            System.out.println("\n❌ UNEXPECTED ERROR: " + e.getMessage());
        } finally {
            tx.close();
        }
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
                pstmt.setString(3, "San Francisco");
                pstmt.executeUpdate();
            }
            
            // Insert 5 active employees with varying salaries
            String[][] employees = {
                {"E001", "Alice Johnson", "alice@company.com", "120000"},
                {"E002", "Bob Smith", "bob@company.com", "95000"},
                {"E003", "Carol Williams", "carol@company.com", "110000"},
                {"E004", "David Brown", "david@company.com", "85000"},
                {"E005", "Eve Davis", "eve@company.com", "105000"}
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
            
            tx.commit();
            System.out.println("✓ Test data created: 5 employees");
            
        } finally {
            tx.close();
        }
    }

    private static void addInactiveEmployee() throws SQLException {
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                    "INSERT INTO employees (id, name, email, department_id, role, salary, status, joining_date) " +
                    "VALUES (?, ?, ?, 'D001', 'ENGINEER', ?, 'INACTIVE', ?)")) {
            pstmt.setString(1, "E006");
            pstmt.setString(2, "Frank Miller (INACTIVE)");
            pstmt.setString(3, "frank@company.com");
            pstmt.setBigDecimal(4, new BigDecimal("90000"));
            pstmt.setDate(5, java.sql.Date.valueOf(LocalDate.now().minusYears(2)));
            pstmt.executeUpdate();
        }
    }

    private static List<Employee> getActiveEmployees(Connection conn) throws SQLException {
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

    private static List<Employee> getAllEmployees(Connection conn) throws SQLException {
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

    private static Employee mapToEmployee(ResultSet rs) throws SQLException {
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

    private static void insertPayrollRecord(Connection conn, PayrollRecord record) throws SQLException {
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

    private static void markEmployeeAsPaid(Connection conn, String employeeId) throws SQLException {
        // In a real system, you might update a "last_paid_date" field
        // For demo purposes, we're just simulating this action
    }

    private static int countPayrollRecords(Connection conn) throws SQLException {
        boolean shouldClose = (conn == null);
        if (conn == null) {
            conn = ConnectionManager.getConnection();
        }
        
        try (PreparedStatement pstmt = conn.prepareStatement("SELECT COUNT(*) FROM payroll_records");
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } finally {
            if (shouldClose) {
                conn.close();
            }
        }
    }

    private static void resetDatabase() throws SQLException {
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement pstmt1 = conn.prepareStatement("DELETE FROM payroll_records");
             PreparedStatement pstmt2 = conn.prepareStatement("DELETE FROM employees");
             PreparedStatement pstmt3 = conn.prepareStatement("DELETE FROM departments")) {
            pstmt1.executeUpdate();
            pstmt2.executeUpdate();
            pstmt3.executeUpdate();
        }
        setupTestData();
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

