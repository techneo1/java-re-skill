package com.srikanth.javareskill.exception;

/**
 * Thrown when an operation targets an employee ID that does not exist in the store.
 *
 * <p>Error code: {@link ErrorCode#EMPLOYEE_NOT_FOUND} ({@code HR-RES-001})</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * throw new EmployeeNotFoundException("E001");
 * }</pre>
 */
public class EmployeeNotFoundException extends ResourceNotFoundException {

    /** The employee ID that was not found. */
    private final String employeeId;

    /**
     * Constructs the exception with the missing employee ID.
     *
     * @param employeeId the ID that could not be resolved
     */
    public EmployeeNotFoundException(String employeeId) {
        super(ErrorCode.EMPLOYEE_NOT_FOUND,
                ErrorContext.of(ErrorCode.EMPLOYEE_NOT_FOUND,
                                "No employee found with ID: " + employeeId)
                        .with("employeeId", employeeId)
                        .build());
        this.employeeId = employeeId;
    }

    /**
     * Constructs the exception with the missing employee ID and an underlying cause.
     *
     * @param employeeId the ID that could not be resolved
     * @param cause      the underlying cause
     */
    public EmployeeNotFoundException(String employeeId, Throwable cause) {
        super(ErrorCode.EMPLOYEE_NOT_FOUND,
                ErrorContext.of(ErrorCode.EMPLOYEE_NOT_FOUND,
                                "No employee found with ID: " + employeeId)
                        .with("employeeId", employeeId)
                        .build(),
                cause);
        this.employeeId = employeeId;
    }

    /** Returns the employee ID that triggered this exception. */
    public String getEmployeeId() {
        return employeeId;
    }
}
