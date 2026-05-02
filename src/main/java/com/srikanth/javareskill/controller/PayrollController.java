package com.srikanth.javareskill.controller;

import com.srikanth.javareskill.domain.Employee;
import com.srikanth.javareskill.domain.PayrollRecord;
import com.srikanth.javareskill.domain.enums.EmployeeStatus;
import com.srikanth.javareskill.dto.response.PayrollResponse;
import com.srikanth.javareskill.mapper.PayrollMapper;
import com.srikanth.javareskill.payroll.PayrollService;
import com.srikanth.javareskill.repository.InMemoryPayrollRepository;
import com.srikanth.javareskill.service.EmployeeService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * REST controller for payroll processing and retrieval.
 *
 * <h2>Endpoints</h2>
 * <pre>
 * POST  /payroll/process?month=2026-02  – Process payroll for active employees → 200
 * GET   /payroll?month=2026-02          – Retrieve payroll records for a month → 200
 * </pre>
 *
 * <h2>Tax logic</h2>
 * <p>Tax calculation is delegated entirely to {@link PayrollService#process},
 * which applies role-based {@code TaxStrategy} implementations (FlatRate /
 * Progressive / Exempt) resolved via {@code TaxStrategyFactory}.
 * The controller is unaware of any tax rules — it only orchestrates the
 * service calls and persists the results.</p>
 *
 * <h2>Month parameter format</h2>
 * <p>Both endpoints accept the month as {@code yyyy-MM} (e.g. {@code 2026-02}).
 * The value is parsed into a {@link YearMonth} and then converted to the first
 * day of that month ({@link LocalDate}) before being passed to the service.</p>
 *
 * <h2>Idempotency note</h2>
 * <p>Processing the same month twice will produce duplicate records in the
 * in-memory store (no deduplication).  A production implementation would add
 * a uniqueness check on {@code (employeeId, payrollMonth)} before persisting.</p>
 */
@RestController
@RequestMapping("/payroll")
public class PayrollController {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final EmployeeService          employeeService;
    private final PayrollService           payrollService;
    private final InMemoryPayrollRepository payrollRepository;

    public PayrollController(EmployeeService          employeeService,
                             PayrollService           payrollService,
                             InMemoryPayrollRepository payrollRepository) {
        this.employeeService   = employeeService;
        this.payrollService    = payrollService;
        this.payrollRepository = payrollRepository;
    }

    // =========================================================================
    // POST /payroll/process?month=2026-02
    // =========================================================================

    /**
     * Processes payroll for all <em>active</em> employees for the given month.
     *
     * <p>Steps:</p>
     * <ol>
     *   <li>Parse the {@code month} query parameter (format: {@code yyyy-MM}).</li>
     *   <li>Fetch all {@code ACTIVE} employees via the service.</li>
     *   <li>For each employee, call {@link PayrollService#process} which applies
     *       the role-based tax strategy and produces a {@link PayrollRecord}.</li>
     *   <li>Persist all records to the in-memory payroll repository.</li>
     *   <li>Return a {@link PayrollResponse} with the records and aggregate totals.</li>
     * </ol>
     *
     * @param month payroll month in {@code yyyy-MM} format (e.g. {@code 2026-02})
     * @return 200 OK with {@link PayrollResponse} containing all processed records
     *         and totals (gross / tax / net)
     */
    @PostMapping("/process")
    public ResponseEntity<PayrollResponse> processPayroll(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") String month) {

        YearMonth  yearMonth   = parseMonth(month);
        LocalDate  payrollDate = yearMonth.atDay(1);

        // Fetch only ACTIVE employees — inactive employees are not paid
        List<Employee> activeEmployees = employeeService.findByStatus(EmployeeStatus.ACTIVE);

        // Process payroll for each employee using role-based tax strategy
        List<PayrollRecord> records = activeEmployees.stream()
                .map(emp -> payrollService.process(emp, payrollDate))
                .toList();

        // Persist all records for later retrieval via GET /payroll
        payrollRepository.saveAll(records);

        return ResponseEntity.ok(PayrollMapper.toResponse(payrollDate, records));
    }

    // =========================================================================
    // GET /payroll?month=2026-02
    // =========================================================================

    /**
     * Returns all payroll records that were previously processed for the given month.
     *
     * @param month payroll month in {@code yyyy-MM} format (e.g. {@code 2026-02})
     * @return 200 OK with {@link PayrollResponse} (records may be empty if payroll
     *         has not been processed yet for that month)
     */
    @GetMapping
    public ResponseEntity<PayrollResponse> getPayroll(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") String month) {

        YearMonth yearMonth   = parseMonth(month);
        LocalDate payrollDate = yearMonth.atDay(1);

        List<PayrollRecord> records = payrollRepository.findByMonth(yearMonth);

        return ResponseEntity.ok(PayrollMapper.toResponse(payrollDate, records));
    }

    // =========================================================================
    // Helper
    // =========================================================================

    /**
     * Parses a {@code yyyy-MM} string to a {@link YearMonth}.
     *
     * @param month the raw string from the query parameter
     * @return parsed {@link YearMonth}
     * @throws IllegalArgumentException if the format is invalid (→ 400 via global handler)
     */
    private static YearMonth parseMonth(String month) {
        try {
            return YearMonth.parse(month, MONTH_FMT);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Invalid month format '" + month + "'. Expected yyyy-MM (e.g. 2026-02)", e);
        }
    }
}

