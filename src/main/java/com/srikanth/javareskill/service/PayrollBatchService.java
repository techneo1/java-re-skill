package com.srikanth.javareskill.service;

import com.srikanth.javareskill.domain.Employee;
import com.srikanth.javareskill.domain.PayrollRecord;
import com.srikanth.javareskill.payroll.PayrollService;
import com.srikanth.javareskill.payroll.impl.PayrollServiceImpl;
import com.srikanth.javareskill.repository.jdbc.JdbcPayrollRecordRepository;
import com.srikanth.javareskill.repository.jdbc.TransactionManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * High-performance payroll batch processing service.
 *
 * <p>This service demonstrates efficient batch processing of payroll using
 * JDBC batch operations combined with transaction management for optimal
 * performance and data integrity.</p>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li><b>Batch Processing</b>: Process hundreds/thousands of employees efficiently</li>
 *   <li><b>ACID Transactions</b>: All payroll committed atomically or rolled back</li>
 *   <li><b>Performance</b>: 10-100x faster than individual inserts</li>
 *   <li><b>Validation</b>: Pre-flight checks before batch processing</li>
 *   <li><b>Error Handling</b>: Complete rollback on any failure</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * PayrollBatchService service = new PayrollBatchService();
 * List<Employee> employees = getAllActiveEmployees();
 * 
 * PayrollBatchResult result = service.processPayrollBatch(
 *     employees, 
 *     LocalDate.of(2026, 5, 1)
 * );
 * 
 * System.out.println("Processed: " + result.getSuccessCount());
 * System.out.println("Time: " + result.getDurationMs() + " ms");
 * }</pre>
 */
public class PayrollBatchService {

    private final PayrollService payrollService;
    private final JdbcPayrollRecordRepository payrollRepo;

    /**
     * Creates a PayrollBatchService with default dependencies.
     */
    public PayrollBatchService() {
        this.payrollService = new PayrollServiceImpl();
        this.payrollRepo = new JdbcPayrollRecordRepository();
    }

    /**
     * Creates a PayrollBatchService with injected dependencies (for testing).
     *
     * @param payrollService payroll calculation service
     * @param payrollRepo payroll record repository
     */
    public PayrollBatchService(PayrollService payrollService, 
                                JdbcPayrollRecordRepository payrollRepo) {
        this.payrollService = Objects.requireNonNull(payrollService, "payrollService");
        this.payrollRepo = Objects.requireNonNull(payrollRepo, "payrollRepo");
    }

    /**
     * Processes payroll for all employees in a single atomic batch operation.
     *
     * <p>This method:</p>
     * <ol>
     *   <li>Validates all employees</li>
     *   <li>Calculates payroll for each employee</li>
     *   <li>Batch inserts all records (10-100x faster)</li>
     *   <li>Commits atomically or rolls back on any error</li>
     * </ol>
     *
     * <p><b>Important:</b> If ANY employee fails validation or processing,
     * the ENTIRE batch is rolled back to prevent partial payroll.</p>
     *
     * @param employees list of employees to pay
     * @param payrollMonth payroll month
     * @return result object with statistics
     * @throws PayrollBatchException if processing fails
     */
    public PayrollBatchResult processPayrollBatch(List<Employee> employees, LocalDate payrollMonth) 
            throws PayrollBatchException {
        
        Objects.requireNonNull(employees, "employees must not be null");
        Objects.requireNonNull(payrollMonth, "payrollMonth must not be null");

        long startTime = System.currentTimeMillis();
        TransactionManager tx = new TransactionManager();

        try {
            tx.begin();
            Connection conn = tx.getConnection();

            // Step 1: Validate all employees
            validateEmployees(employees);

            // Step 2: Calculate payroll for all employees
            List<PayrollRecord> records = new ArrayList<>();
            for (Employee emp : employees) {
                PayrollRecord record = payrollService.process(emp, payrollMonth);
                records.add(record);
            }

            // Step 3: Batch insert all records (FAST!)
            int[] results = payrollRepo.batchInsert(records, conn);

            // Step 4: Commit transaction
            tx.commit();

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            return new PayrollBatchResult(
                results.length,
                0,
                duration,
                true,
                null
            );

        } catch (Exception e) {
            try {
                tx.rollback();
            } catch (SQLException rollbackEx) {
                e.addSuppressed(rollbackEx);
            }

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            throw new PayrollBatchException(
                "Payroll batch processing failed: " + e.getMessage(),
                e,
                new PayrollBatchResult(0, employees.size(), duration, false, e.getMessage())
            );

        } finally {
            tx.close();
        }
    }

    /**
     * Processes payroll in chunks for very large employee lists.
     *
     * <p>Use this method when processing 10,000+ employees to avoid memory
     * issues and overly long transactions.</p>
     *
     * @param employees all employees to pay
     * @param payrollMonth payroll month
     * @param chunkSize number of employees per chunk (recommended: 500-1000)
     * @return combined result for all chunks
     * @throws PayrollBatchException if any chunk fails
     */
    public PayrollBatchResult processPayrollInChunks(List<Employee> employees, 
                                                      LocalDate payrollMonth,
                                                      int chunkSize) 
            throws PayrollBatchException {
        
        Objects.requireNonNull(employees, "employees must not be null");
        Objects.requireNonNull(payrollMonth, "payrollMonth must not be null");
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize must be > 0");
        }

        long startTime = System.currentTimeMillis();
        int totalSuccess = 0;
        int size = employees.size();

        for (int i = 0; i < size; i += chunkSize) {
            int end = Math.min(i + chunkSize, size);
            List<Employee> chunk = employees.subList(i, end);

            PayrollBatchResult chunkResult = processPayrollBatch(chunk, payrollMonth);
            totalSuccess += chunkResult.getSuccessCount();
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        return new PayrollBatchResult(totalSuccess, 0, duration, true, null);
    }

    // -------------------------------------------------------------------------
    // Validation
    // -------------------------------------------------------------------------

    private void validateEmployees(List<Employee> employees) throws ValidationException {
        for (Employee emp : employees) {
            if (emp.getStatus() != com.srikanth.javareskill.domain.enums.EmployeeStatus.ACTIVE) {
                throw new ValidationException("Cannot process payroll for inactive employee: " + emp.getName());
            }

            if (emp.getSalary() == null || emp.getSalary().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                throw new ValidationException("Invalid salary for employee: " + emp.getName());
            }
        }
    }

    // -------------------------------------------------------------------------
    // Result Classes
    // -------------------------------------------------------------------------

    /**
     * Result object containing statistics from a batch payroll operation.
     */
    public static class PayrollBatchResult {
        private final int successCount;
        private final int failureCount;
        private final long durationMs;
        private final boolean committed;
        private final String errorMessage;

        public PayrollBatchResult(int successCount, int failureCount, long durationMs,
                                   boolean committed, String errorMessage) {
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.durationMs = durationMs;
            this.committed = committed;
            this.errorMessage = errorMessage;
        }

        public int getSuccessCount() { return successCount; }
        public int getFailureCount() { return failureCount; }
        public long getDurationMs() { return durationMs; }
        public boolean isCommitted() { return committed; }
        public String getErrorMessage() { return errorMessage; }

        @Override
        public String toString() {
            return String.format(
                "PayrollBatchResult{success=%d, failures=%d, duration=%dms, committed=%s}",
                successCount, failureCount, durationMs, committed
            );
        }
    }

    /**
     * Exception thrown when payroll batch processing fails.
     */
    public static class PayrollBatchException extends Exception {
        private final PayrollBatchResult result;

        public PayrollBatchException(String message, Throwable cause, PayrollBatchResult result) {
            super(message, cause);
            this.result = result;
        }

        public PayrollBatchResult getResult() {
            return result;
        }
    }

    /**
     * Exception thrown during employee validation.
     */
    public static class ValidationException extends Exception {
        public ValidationException(String message) {
            super(message);
        }
    }
}

