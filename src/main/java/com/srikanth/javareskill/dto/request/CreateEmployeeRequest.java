package com.srikanth.javareskill.dto.request;

import com.srikanth.javareskill.domain.enums.EmployeeStatus;
import com.srikanth.javareskill.domain.enums.Role;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request body for {@code POST /employees}.
 *
 * <h2>Bean Validation rules</h2>
 * <p>All constraints are evaluated before any service logic runs.
 * Spring calls the validator automatically when the controller parameter
 * is annotated with {@code @Valid}; violations are collected and returned
 * as HTTP 400 by {@code GlobalExceptionHandler.handleValidation()}.</p>
 *
 * <table border="1">
 *   <tr><th>Field</th><th>Rule</th></tr>
 *   <tr><td>name</td>
 *       <td>Not blank · 2–100 chars · letters, spaces, hyphens, apostrophes only</td></tr>
 *   <tr><td>email</td>
 *       <td>Not blank · well-formed RFC-5322 address · max 255 chars</td></tr>
 *   <tr><td>departmentId</td>
 *       <td>Not blank · max 50 chars</td></tr>
 *   <tr><td>role</td>
 *       <td>Not null (must be a valid {@link Role} enum constant)</td></tr>
 *   <tr><td>salary</td>
 *       <td>Not null · ≥ 0 · ≤ 10,000,000 · max 2 decimal places</td></tr>
 *   <tr><td>status</td>
 *       <td>Defaults to {@code ACTIVE}; if supplied must be a valid enum constant</td></tr>
 *   <tr><td>joiningDate</td>
 *       <td>Defaults to today; if supplied must not be in the future</td></tr>
 * </table>
 */
public class CreateEmployeeRequest {

    /**
     * Full name of the employee.
     * <ul>
     *   <li>Must not be blank.</li>
     *   <li>2–100 characters.</li>
     *   <li>Letters (incl. accented), spaces, hyphens and apostrophes only.</li>
     * </ul>
     */
    @NotBlank(message = "name must not be blank")
    @Size(min = 2, max = 100,
          message = "name must be between 2 and 100 characters")
    @Pattern(regexp = "^[\\p{L} .'-]+$",
             message = "name must contain only letters, spaces, hyphens, apostrophes, or dots")
    private String name;

    /**
     * Corporate e-mail address.
     * <ul>
     *   <li>Must not be blank.</li>
     *   <li>Must be a well-formed RFC-5322 address.</li>
     *   <li>Max 255 characters (SMTP limit).</li>
     * </ul>
     */
    @NotBlank(message = "email must not be blank")
    @Email(message = "email must be a well-formed address (e.g. user@example.com)")
    @Size(max = 255, message = "email must not exceed 255 characters")
    private String email;

    /**
     * ID of the department this employee belongs to.
     * <ul>
     *   <li>Must not be blank.</li>
     *   <li>Max 50 characters.</li>
     * </ul>
     */
    @NotBlank(message = "departmentId must not be blank")
    @Size(max = 50, message = "departmentId must not exceed 50 characters")
    private String departmentId;

    /**
     * Job role.  Must be one of the {@link Role} enum constants.
     */
    @NotNull(message = "role must not be null — valid values: "
            + "ENGINEER, SENIOR_ENGINEER, MANAGER, SENIOR_MANAGER, ANALYST, HR, DIRECTOR")
    private Role role;

    /**
     * Monthly gross salary.
     * <ul>
     *   <li>Must not be null.</li>
     *   <li>Must be ≥ 0 (no negative salaries).</li>
     *   <li>Must be ≤ 10,000,000 (sanity cap).</li>
     *   <li>Max 2 decimal places (cents precision).</li>
     * </ul>
     */
    @NotNull(message = "salary must not be null")
    @DecimalMin(value = "0.00", inclusive = true,
                message = "salary must be 0.00 or greater")
    @DecimalMax(value = "10000000.00", inclusive = true,
                message = "salary must not exceed 10,000,000.00")
    @Digits(integer = 8, fraction = 2,
            message = "salary must have at most 8 integer digits and 2 decimal places")
    private BigDecimal salary;

    /**
     * Employment status.  Defaults to {@link EmployeeStatus#ACTIVE} when
     * omitted from the request body.
     */
    private EmployeeStatus status = EmployeeStatus.ACTIVE;

    /**
     * The date the employee joined the organisation.
     * Defaults to today when omitted; must not be in the future if supplied.
     */
    @PastOrPresent(message = "joiningDate must not be in the future")
    private LocalDate joiningDate = LocalDate.now();

    // ── Constructors ─────────────────────────────────────────────────────────

    /** No-arg constructor required by Jackson for deserialisation. */
    public CreateEmployeeRequest() {}

    public CreateEmployeeRequest(String name, String email, String departmentId,
                                  Role role, BigDecimal salary,
                                  EmployeeStatus status, LocalDate joiningDate) {
        this.name         = name;
        this.email        = email;
        this.departmentId = departmentId;
        this.role         = role;
        this.salary       = salary;
        this.status       = status != null ? status : EmployeeStatus.ACTIVE;
        this.joiningDate  = joiningDate != null ? joiningDate : LocalDate.now();
    }

    // ── Getters & setters ────────────────────────────────────────────────────

    public String getName()              { return name; }
    public void   setName(String name)   { this.name = name; }

    public String getEmail()               { return email; }
    public void   setEmail(String email)   { this.email = email; }

    public String getDepartmentId()                      { return departmentId; }
    public void   setDepartmentId(String departmentId)   { this.departmentId = departmentId; }

    public Role getRole()              { return role; }
    public void setRole(Role role)     { this.role = role; }

    public BigDecimal getSalary()                { return salary; }
    public void       setSalary(BigDecimal sal)  { this.salary = sal; }

    public EmployeeStatus getStatus()                      { return status; }
    public void           setStatus(EmployeeStatus status) { this.status = status; }

    public LocalDate getJoiningDate()                      { return joiningDate; }
    public void      setJoiningDate(LocalDate joiningDate) { this.joiningDate = joiningDate; }

    @Override
    public String toString() {
        return "CreateEmployeeRequest{name='%s', email='%s', departmentId='%s', role=%s, salary=%s, status=%s, joiningDate=%s}"
                .formatted(name, email, departmentId, role, salary, status, joiningDate);
    }
}
