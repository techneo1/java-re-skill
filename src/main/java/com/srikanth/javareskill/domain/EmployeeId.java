package com.srikanth.javareskill.domain;

import java.util.Objects;

/**
 * Strongly-typed, immutable value object used as the identity key for an employee.
 *
 * <h2>Why a custom key instead of a plain String?</h2>
 * <ul>
 *   <li>Type-safety: prevents mixing an employee ID with any other String-based ID.</li>
 *   <li>Explicit contract: {@code equals} and {@code hashCode} are deliberately defined
 *       so the HashMap behaviour is predictable and testable.</li>
 * </ul>
 *
 * <h2>HashMap key contract</h2>
 * <ol>
 *   <li><b>equals consistency</b> – two {@code EmployeeId}s are equal iff their
 *       {@code value} strings are equal (case-insensitive by design).</li>
 *   <li><b>hashCode consistency</b> – objects that are {@code equal} must return the
 *       same hash code.</li>
 *   <li><b>Immutability</b> – once constructed the key must never change.</li>
 * </ol>
 */
public class EmployeeId {

    private final String value;

    /**
     * Creates an {@code EmployeeId} from the given string.
     *
     * @param value a non-null, non-blank employee identifier
     * @throws NullPointerException     if {@code value} is null
     * @throws IllegalArgumentException if {@code value} is blank
     */
    public EmployeeId(String value) {
        Objects.requireNonNull(value, "EmployeeId value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("EmployeeId value must not be blank");
        }
        // Normalise to uppercase so look-ups are case-insensitive
        this.value = value.toUpperCase();
    }

    /** Returns the normalised (upper-case) string representation of this ID. */
    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmployeeId)) return false;
        EmployeeId other = (EmployeeId) o;
        return value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "EmployeeId(" + value + ")";
    }
}

