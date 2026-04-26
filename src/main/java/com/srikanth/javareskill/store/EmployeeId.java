package com.srikanth.javareskill.store;

import java.util.Objects;

/**
 * Strongly-typed, immutable value object used as a key in the employee store's
 * {@link java.util.HashMap}.
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
 *       {@code value} strings are equal (case-insensitive by design, so {@code "e001"}
 *       and {@code "E001"} resolve to the same bucket entry).</li>
 *   <li><b>hashCode consistency</b> – objects that are {@code equal} must return the
 *       same hash code.  Objects that are NOT equal may share a hash code (collision)
 *       but this only affects performance, never correctness.</li>
 *   <li><b>Immutability</b> – once constructed the key must never change, otherwise a
 *       stored entry becomes unreachable.</li>
 * </ol>
 */
public final class EmployeeId {

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

    // -------------------------------------------------------------------------
    // equals & hashCode — MUST be consistent for correct HashMap behaviour
    // -------------------------------------------------------------------------

    /**
     * Two {@code EmployeeId}s are equal when their normalised values are equal.
     *
     * <p>This satisfies the equals contract:
     * <ul>
     *   <li>Reflexive  – {@code x.equals(x)} is always {@code true}</li>
     *   <li>Symmetric  – {@code x.equals(y) == y.equals(x)}</li>
     *   <li>Transitive – if {@code x.equals(y)} and {@code y.equals(z)}, then {@code x.equals(z)}</li>
     *   <li>Consistent – repeated calls return the same result (object is immutable)</li>
     *   <li>Non-null   – {@code x.equals(null)} is always {@code false}</li>
     * </ul>
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmployeeId)) return false;
        EmployeeId other = (EmployeeId) o;
        return value.equals(other.value);
    }

    /**
     * Hash code derived solely from {@code value}, consistent with {@link #equals}.
     *
     * <p>Invariant: {@code a.equals(b) ⟹ a.hashCode() == b.hashCode()}</p>
     */
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "EmployeeId(" + value + ")";
    }
}

