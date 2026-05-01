package com.srikanth.javareskill.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Sealed class hierarchy representing the employment-type dimension of an
 * employee — {@link PermanentEmployee} or {@link ContractEmployee}.
 *
 * <h2>Java sealed-class features demonstrated</h2>
 * <ul>
 *   <li>{@code sealed … permits} restricts the set of subtypes to a closed,
 *       compiler-known list.</li>
 *   <li>Each permitted subtype is declared {@code final} (could also be
 *       {@code non-sealed} or {@code sealed} itself).</li>
 *   <li>Pattern matching with {@code switch} expressions (JDK 21+) can
 *       exhaustively match on all subtypes without a {@code default} arm,
 *       because the compiler knows the hierarchy is complete.</li>
 * </ul>
 *
 * <h2>Design rationale — composition over modification</h2>
 * <p>The existing {@link Employee} class is {@code final} and widely used.
 * Rather than modifying it, this hierarchy captures the <em>employment-type</em>
 * concern as a first-class domain concept.  An {@code EmployeeType} wraps the
 * common {@link Employee} reference and adds type-specific attributes
 * (e.g. benefits percentage, contract end date, hourly rate).</p>
 *
 * <h2>SOLID principles applied</h2>
 * <ul>
 *   <li><b>O – Open/Closed</b>: New employment types can be added by creating
 *       a new permitted subclass — existing code that uses exhaustive
 *       {@code switch} will get a compile error, guiding the developer.</li>
 *   <li><b>L – Liskov Substitution</b>: Both subtypes are fully substitutable
 *       for {@code EmployeeType} — callers that only need the common fields
 *       work transparently with either.</li>
 * </ul>
 */
public abstract sealed class EmployeeType permits PermanentEmployee, ContractEmployee {

    private final Employee employee;

    protected EmployeeType(Employee employee) {
        this.employee = Objects.requireNonNull(employee, "employee must not be null");
    }

    /** Returns the underlying employee entity. */
    public Employee getEmployee() {
        return employee;
    }

    // -------------------------------------------------------------------------
    // Convenience delegates — avoid .getEmployee().getXxx() chains
    // -------------------------------------------------------------------------

    public String getId()           { return employee.getId(); }
    public String getName()         { return employee.getName(); }
    public String getEmail()        { return employee.getEmail(); }
    public BigDecimal getSalary()   { return employee.getSalary(); }

    /**
     * Computes the annual compensation for this employee type.
     *
     * <p>Subtype-specific: permanent employees include benefits;
     * contract employees use hourly rate × contracted hours.</p>
     *
     * @return total annual compensation; never {@code null}
     */
    public BigDecimal annualCompensation() {
        // Base implementation: 12 × monthly salary
        return employee.getSalary().multiply(BigDecimal.valueOf(12));
    }

    /**
     * Returns a human-readable label for the employment type.
     *
     * @return e.g. "Permanent" or "Contract"
     */
    public abstract String typeName();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmployeeType that)) return false;
        return Objects.equals(employee.getId(), that.employee.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(employee.getId());
    }

    @Override
    public String toString() {
        return typeName() + "{employee=" + employee + "}";
    }
}

