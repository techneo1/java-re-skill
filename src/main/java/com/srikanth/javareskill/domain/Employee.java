package com.srikanth.javareskill.domain;

import com.srikanth.javareskill.domain.enums.EmployeeStatus;
import com.srikanth.javareskill.domain.enums.Role;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Represents an employee in the organisation.
 *
 * <p>Immutable after construction; use {@link Builder} to create instances.</p>
 */
public final class Employee {

    private final String id;
    private final String name;
    private final String email;
    private final String departmentId;
    private final Role role;
    private final BigDecimal salary;
    private final EmployeeStatus status;
    private final LocalDate joiningDate;

    private Employee(Builder builder) {
        this.id           = Objects.requireNonNull(builder.id,           "id must not be null");
        this.name         = Objects.requireNonNull(builder.name,         "name must not be null");
        this.email        = Objects.requireNonNull(builder.email,        "email must not be null");
        this.departmentId = Objects.requireNonNull(builder.departmentId, "departmentId must not be null");
        this.role         = Objects.requireNonNull(builder.role,         "role must not be null");
        this.salary       = Objects.requireNonNull(builder.salary,       "salary must not be null");
        this.status       = Objects.requireNonNull(builder.status,       "status must not be null");
        this.joiningDate  = Objects.requireNonNull(builder.joiningDate,  "joiningDate must not be null");

        if (salary.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("salary must not be negative");
        }
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public String getId()             { return id; }
    public String getName()           { return name; }
    public String getEmail()          { return email; }
    public String getDepartmentId()   { return departmentId; }
    public Role getRole()             { return role; }
    public BigDecimal getSalary()     { return salary; }
    public EmployeeStatus getStatus() { return status; }
    public LocalDate getJoiningDate() { return joiningDate; }

    // -------------------------------------------------------------------------
    // equals / hashCode / toString
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Employee)) return false;
        Employee e = (Employee) o;
        return Objects.equals(id, e.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Employee{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", departmentId='" + departmentId + '\'' +
                ", role=" + role +
                ", salary=" + salary +
                ", status=" + status +
                ", joiningDate=" + joiningDate +
                '}';
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String id;
        private String name;
        private String email;
        private String departmentId;
        private Role role;
        private BigDecimal salary;
        private EmployeeStatus status;
        private LocalDate joiningDate;

        private Builder() {}

        public Builder id(String id)                     { this.id = id; return this; }
        public Builder name(String name)                 { this.name = name; return this; }
        public Builder email(String email)               { this.email = email; return this; }
        public Builder departmentId(String departmentId) { this.departmentId = departmentId; return this; }
        public Builder role(Role role)                   { this.role = role; return this; }
        public Builder salary(BigDecimal salary)         { this.salary = salary; return this; }
        public Builder status(EmployeeStatus status)     { this.status = status; return this; }
        public Builder joiningDate(LocalDate joiningDate){ this.joiningDate = joiningDate; return this; }

        public Employee build() {
            return new Employee(this);
        }
    }
}

