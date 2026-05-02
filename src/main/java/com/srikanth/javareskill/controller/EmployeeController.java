package com.srikanth.javareskill.controller;

import com.srikanth.javareskill.domain.Employee;
import com.srikanth.javareskill.domain.EmployeeId;
import com.srikanth.javareskill.domain.enums.EmployeeStatus;
import com.srikanth.javareskill.dto.EmployeeDTO;
import com.srikanth.javareskill.dto.request.CreateEmployeeRequest;
import com.srikanth.javareskill.dto.request.UpdateEmployeeStatusRequest;
import com.srikanth.javareskill.dto.response.CreatedResponse;
import com.srikanth.javareskill.exception.EmployeeNotFoundException;
import com.srikanth.javareskill.mapper.EmployeeMapper;
import com.srikanth.javareskill.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * REST controller for Employee lifecycle operations.
 *
 * <h2>Endpoints</h2>
 * <pre>
 * POST   /employees                       – Create a new employee          → 201
 * GET    /employees/{id}                  – Fetch employee by ID           → 200 / 404
 * GET    /employees?departmentId=&status= – List all with optional filters → 200
 * PUT    /employees/{id}/status           – Update employment status       → 200
 * </pre>
 *
 * <h2>Filtering strategy</h2>
 * <p>{@code GET /employees} accepts optional query parameters.  Filtering is
 * applied with Java streams so that multiple predicates compose cleanly without
 * additional repository methods:</p>
 * <ol>
 *   <li>Fetch the full employee list from the service once.</li>
 *   <li>Conditionally chain {@code .filter()} for each supplied parameter.</li>
 *   <li>Map to DTOs and collect to a list.</li>
 * </ol>
 *
 * <h2>Validation</h2>
 * <p>{@code @Valid} on request body parameters triggers Bean Validation before
 * any service call.  Violations surface as HTTP 400 via
 * {@code GlobalExceptionHandler.handleValidation()}.</p>
 */
@RestController
@RequestMapping("/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    // =========================================================================
    // POST /employees
    // =========================================================================

    /**
     * Creates a new employee and returns HTTP 201 with:
     * <ul>
     *   <li>A {@code Location} header pointing to the new resource URI.</li>
     *   <li>A {@link CreatedResponse} body carrying the generated ID.</li>
     * </ul>
     *
     * @param request validated request body
     * @return 201 Created
     */
    @PostMapping
    public ResponseEntity<CreatedResponse> createEmployee(
            @Valid @RequestBody CreateEmployeeRequest request) {

        Employee employee = EmployeeMapper.toDomain(request);
        employeeService.hire(employee);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(employee.getId())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(CreatedResponse.ofEmployee(employee.getId()));
    }

    // =========================================================================
    // GET /employees/{id}
    // =========================================================================

    /**
     * Returns the employee with the given ID.
     *
     * @param id employee identifier (path variable)
     * @return 200 OK with {@link EmployeeDTO}, or 404 if not found
     * @throws EmployeeNotFoundException if no employee exists with that ID
     *         (handled globally → 404)
     */
    @GetMapping("/{id}")
    public ResponseEntity<EmployeeDTO> getEmployee(@PathVariable String id) {

        Employee employee = employeeService
                .findById(new EmployeeId(id))
                .orElseThrow(() -> new EmployeeNotFoundException(id));

        return ResponseEntity.ok(EmployeeMapper.toDto(employee));
    }

    // =========================================================================
    // GET /employees?departmentId=&status=
    // =========================================================================

    /**
     * Returns all employees, optionally filtered by department and/or status.
     *
     * <p>Both parameters are optional.  When both are supplied the filters
     * are AND-ed together (stream chaining).</p>
     *
     * <h3>Example requests</h3>
     * <pre>
     * GET /employees                              → all employees
     * GET /employees?departmentId=DEPT-01         → employees in DEPT-01
     * GET /employees?status=ACTIVE                → all active employees
     * GET /employees?departmentId=DEPT-01&amp;status=INACTIVE
     *                                             → inactive employees in DEPT-01
     * </pre>
     *
     * @param departmentId optional department filter
     * @param status       optional status filter ({@code ACTIVE} or {@code INACTIVE})
     * @return 200 OK with list of {@link EmployeeDTO} (may be empty)
     */
    @GetMapping
    public ResponseEntity<List<EmployeeDTO>> listEmployees(
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) EmployeeStatus status) {

        List<EmployeeDTO> result = employeeService.findAll()
                .stream()
                // ── filter by departmentId if supplied ──────────────────────
                .filter(e -> departmentId == null
                        || departmentId.equalsIgnoreCase(e.getDepartmentId()))
                // ── filter by status if supplied ────────────────────────────
                .filter(e -> status == null || status == e.getStatus())
                // ── project to DTO ──────────────────────────────────────────
                .map(EmployeeMapper::toDto)
                .toList();

        return ResponseEntity.ok(result);
    }

    // =========================================================================
    // PUT /employees/{id}/status
    // =========================================================================

    /**
     * Updates the employment status of an existing employee.
     *
     * <p>Because {@link Employee} is immutable, the handler:
     * <ol>
     *   <li>Fetches the current entity.</li>
     *   <li>Calls {@link EmployeeMapper#withStatus} to produce a new instance
     *       with only the status changed.</li>
     *   <li>Persists the new instance via {@code EmployeeService.update()}.</li>
     * </ol>
     *
     * @param id      employee identifier (path variable)
     * @param request validated body containing the new status
     * @return 200 OK with the updated {@link EmployeeDTO}
     * @throws EmployeeNotFoundException if no employee exists with that ID (→ 404)
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<EmployeeDTO> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateEmployeeStatusRequest request) {

        Employee existing = employeeService
                .findById(new EmployeeId(id))
                .orElseThrow(() -> new EmployeeNotFoundException(id));

        Employee updated = EmployeeMapper.withStatus(existing, request.getStatus());
        employeeService.update(updated);

        return ResponseEntity.ok(EmployeeMapper.toDto(updated));
    }
}

