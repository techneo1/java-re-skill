package com.srikanth.javareskill.exception;

/**
 * Thrown when an operation targets a department ID that does not exist in the store.
 *
 * <p>Error code: {@link ErrorCode#DEPARTMENT_NOT_FOUND} ({@code HR-RES-002})</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * throw new DepartmentNotFoundException("DEPT-99");
 * }</pre>
 */
public final class DepartmentNotFoundException extends ResourceNotFoundException {

    /** The department ID that was not found. */
    private final String departmentId;

    /**
     * Constructs the exception with the missing department ID.
     *
     * @param departmentId the ID that could not be resolved
     */
    public DepartmentNotFoundException(String departmentId) {
        super(ErrorCode.DEPARTMENT_NOT_FOUND,
                ErrorContext.of(ErrorCode.DEPARTMENT_NOT_FOUND,
                                "No department found with ID: " + departmentId)
                        .with("departmentId", departmentId)
                        .build());
        this.departmentId = departmentId;
    }

    /**
     * Constructs the exception with the missing department ID and an underlying cause.
     *
     * @param departmentId the ID that could not be resolved
     * @param cause        the underlying cause
     */
    public DepartmentNotFoundException(String departmentId, Throwable cause) {
        super(ErrorCode.DEPARTMENT_NOT_FOUND,
                ErrorContext.of(ErrorCode.DEPARTMENT_NOT_FOUND,
                                "No department found with ID: " + departmentId)
                        .with("departmentId", departmentId)
                        .build(),
                cause);
        this.departmentId = departmentId;
    }

    /** Returns the department ID that triggered this exception. */
    public String getDepartmentId() {
        return departmentId;
    }
}
