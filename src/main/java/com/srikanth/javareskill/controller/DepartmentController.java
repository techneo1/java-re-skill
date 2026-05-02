package com.srikanth.javareskill.controller;

import com.srikanth.javareskill.domain.Department;
import com.srikanth.javareskill.dto.EmployeeDTO;
import com.srikanth.javareskill.dto.request.CreateDepartmentRequest;
import com.srikanth.javareskill.dto.response.CreatedResponse;
import com.srikanth.javareskill.dto.response.DepartmentWithEmployeesResponse;
import com.srikanth.javareskill.exception.DepartmentNotFoundException;
import com.srikanth.javareskill.mapper.DepartmentMapper;
import com.srikanth.javareskill.mapper.EmployeeMapper;
import com.srikanth.javareskill.service.DepartmentService;
import com.srikanth.javareskill.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * REST controller for Department operations.
 *
 * <h2>Endpoints</h2>
 * <pre>
 * POST  /departments      – Create a new department          → 201
 * GET   /departments/{id} – Fetch department with employees  → 200 / 404
 * </pre>
 *
 * <h2>GET /departments/{id} design</h2>
 * <p>Returns a composite {@link DepartmentWithEmployeesResponse} instead of
 * a plain {@code DepartmentDTO} so callers avoid an additional round-trip to
 * list the department's employees.  The employee list is fetched via
 * {@code EmployeeService.findByDepartment(id)} and assembled by
 * {@link DepartmentMapper#toWithEmployees}.</p>
 */
@RestController
@RequestMapping("/departments")
public class DepartmentController {

    private final DepartmentService departmentService;
    private final EmployeeService   employeeService;

    public DepartmentController(DepartmentService departmentService,
                                EmployeeService   employeeService) {
        this.departmentService = departmentService;
        this.employeeService   = employeeService;
    }

    // =========================================================================
    // POST /departments
    // =========================================================================

    /**
     * Creates a new department.
     *
     * <p>Returns HTTP 201 with a {@code Location} header and a
     * {@link CreatedResponse} body carrying the generated ID.</p>
     *
     * @param request validated request body
     * @return 201 Created
     */
    @PostMapping
    public ResponseEntity<CreatedResponse> createDepartment(
            @Valid @RequestBody CreateDepartmentRequest request) {

        Department department = DepartmentMapper.toDomain(request);
        departmentService.create(department);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(department.getId())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(CreatedResponse.ofDepartment(department.getId()));
    }

    // =========================================================================
    // GET /departments/{id}
    // =========================================================================

    /**
     * Returns a department along with all employees belonging to it.
     *
     * @param id department identifier (path variable)
     * @return 200 OK with {@link DepartmentWithEmployeesResponse}, or 404 if not found
     * @throws DepartmentNotFoundException if no department exists with that ID (→ 404)
     */
    @GetMapping("/{id}")
    public ResponseEntity<DepartmentWithEmployeesResponse> getDepartment(
            @PathVariable String id) {

        Department department = departmentService
                .findById(id)
                .orElseThrow(() -> new DepartmentNotFoundException(id));

        List<EmployeeDTO> employees = EmployeeMapper.toDtoList(
                employeeService.findByDepartment(id));

        return ResponseEntity.ok(
                DepartmentMapper.toWithEmployees(department, employees));
    }
}

