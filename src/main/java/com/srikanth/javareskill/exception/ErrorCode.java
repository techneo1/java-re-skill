package com.srikanth.javareskill.exception;

/**
 * Enumeration of all application-level error codes.
 *
 * <p>Each constant carries a machine-readable {@link #code} string (suitable
 * for API responses, logs, and monitoring dashboards) and a
 * {@link #description} that conveys the semantics in plain English.</p>
 *
 * <h2>Naming convention</h2>
 * <pre>
 * HR-{CATEGORY}-{sequence}
 *
 * Categories:
 *   RES  – resource / not-found errors
 *   BIZ  – business-rule violations
 *   CFG  – configuration errors
 *   VAL  – generic input-validation errors
 * </pre>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * throw new EmployeeNotFoundException("E001");
 * // → exception.getErrorCode() == ErrorCode.EMPLOYEE_NOT_FOUND
 * // → exception.getErrorCode().getCode() == "HR-RES-001"
 * }</pre>
 */
public enum ErrorCode {

    // ------------------------------------------------------------------
    // RES – Resource / not-found errors
    // ------------------------------------------------------------------

    /** A requested employee record could not be located. */
    EMPLOYEE_NOT_FOUND(
            "HR-RES-001",
            "The requested employee does not exist"),

    /** A referenced department could not be located. */
    DEPARTMENT_NOT_FOUND(
            "HR-RES-002",
            "The referenced department does not exist"),

    // ------------------------------------------------------------------
    // BIZ – Business-rule violations
    // ------------------------------------------------------------------

    /** An e-mail address is already registered to another employee. */
    DUPLICATE_EMAIL(
            "HR-BIZ-001",
            "The supplied e-mail address is already in use"),

    /** A salary value is negative, below the minimum, or above the maximum. */
    INVALID_SALARY(
            "HR-BIZ-002",
            "The supplied salary value is outside the permitted range"),

    // ------------------------------------------------------------------
    // CFG – Configuration errors
    // ------------------------------------------------------------------

    /** Application configuration could not be loaded or parsed. */
    CONFIGURATION_ERROR(
            "HR-CFG-001",
            "Application configuration is missing or malformed"),

    // ------------------------------------------------------------------
    // VAL – Generic input-validation errors
    // ------------------------------------------------------------------

    /** A required argument was null or blank. */
    INVALID_ARGUMENT(
            "HR-VAL-001",
            "A required argument is null, blank, or otherwise invalid"),

    /** A salary argument was null. */
    NULL_SALARY(
            "HR-VAL-002",
            "Salary must not be null"),

    /** The supplied min/max salary range is logically impossible. */
    INVALID_SALARY_RANGE(
            "HR-VAL-003",
            "Minimum salary must not exceed maximum salary");

    // ------------------------------------------------------------------
    // Fields & constructor
    // ------------------------------------------------------------------

    /** Short, machine-readable identifier (e.g. {@code "HR-RES-001"}). */
    private final String code;

    /** Human-readable description of what the error code represents. */
    private final String description;

    ErrorCode(String code, String description) {
        this.code        = code;
        this.description = description;
    }

    /**
     * Returns the short machine-readable identifier (e.g. {@code "HR-RES-001"}).
     * This value is stable across releases and safe to include in API responses.
     */
    public String getCode() {
        return code;
    }

    /**
     * Returns the human-readable description of this error code.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns a formatted string combining both the code and the description,
     * e.g. {@code "HR-RES-001: The requested employee does not exist"}.
     */
    @Override
    public String toString() {
        return code + ": " + description;
    }
}

