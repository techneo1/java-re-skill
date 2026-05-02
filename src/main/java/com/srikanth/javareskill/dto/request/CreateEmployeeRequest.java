package com.srikanth.javareskill.dto.request;

import com.srikanth.javareskill.domain.enums.EmployeeStatus;
import com.srikanth.javareskill.domain.enums.Role;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request body for {@code POST /employees}.
 *
 * <p>Bean Validation (JSR-380) annotations are placed here so that
 * the controller can simply annotate the parameter with {@code @Valid}
 * and Spring will reject malformed payloads with HTTP 400 before any
 * service logic runs.</p>
 *
 * <h2>Design notes</h2>
 * <ul>
 *   <li>No {@code id} field — the server generates the ID (UUID).</li>
 *   <li>{@code status} defaults to {@code ACTIVE} when absent in the JSON;
 *       callers can override it explicitly.</li>
 *   <li>{@code joiningDate} defaults to today when absent.</li>
 * </ul>
 */
public class CreateEmployeeRequest {

    @NotBlank(message = "name must not be blank")
    private String name;

    @NotBlank(message = "email must not be blank")
    @Email(message = "email must be a well-formed address")
    private String email;

    @NotBlank(message = "departmentId must not be blank")
    private String departmentId;

    @NotNull(message = "role must not be null")
    private Role role;

    @NotNull(message = "salary must not be null")
    @DecimalMin(value = "0.00", inclusive = true, message = "salary must not be negative")
    private BigDecimal salary;

    /** Defaults to {@link EmployeeStatus#ACTIVE} when omitted from the request. */
    private EmployeeStatus status = EmployeeStatus.ACTIVE;

    /** Defaults to today when omitted from the request. */
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
        this.status       = status;
        this.joiningDate  = joiningDate;
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

