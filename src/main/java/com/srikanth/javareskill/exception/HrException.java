package com.srikanth.javareskill.exception;

/**
 * Root of the application's custom exception hierarchy.
 *
 * <pre>
 * HrException  (abstract, unchecked)
 * ├── ResourceNotFoundException  (abstract)
 * │   ├── EmployeeNotFoundException
 * │   └── DepartmentNotFoundException
 * └── BusinessRuleException  (abstract)
 *     ├── InvalidSalaryException
 *     └── DuplicateEmailException
 * </pre>
 *
 * <p>All exceptions are <em>unchecked</em> ({@link RuntimeException}) so callers
 * are not forced to declare {@code throws} clauses, while the hierarchy still
 * allows fine-grained {@code catch} blocks when needed.</p>
 *
 * <p>Every instance carries an {@link ErrorCode} that uniquely identifies the
 * failure scenario and an {@link ErrorContext} with full structured details.</p>
 */
public abstract class HrException extends RuntimeException {

    /** The machine-readable code that classifies this error. */
    private final ErrorCode    errorCode;

    /** Structured context with code, message, timestamp, and optional details. */
    private final ErrorContext errorContext;

    protected HrException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode    = errorCode;
        this.errorContext = ErrorContext.simple(errorCode, message);
    }

    protected HrException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode    = errorCode;
        this.errorContext = ErrorContext.simple(errorCode, message);
    }

    protected HrException(ErrorCode errorCode, ErrorContext errorContext) {
        super(errorContext.getMessage());
        this.errorCode    = errorCode;
        this.errorContext = errorContext;
    }

    protected HrException(ErrorCode errorCode, ErrorContext errorContext, Throwable cause) {
        super(errorContext.getMessage(), cause);
        this.errorCode    = errorCode;
        this.errorContext = errorContext;
    }

    /**
     * Returns the {@link ErrorCode} that classifies this exception.
     * The code is stable across releases and safe to include in API responses.
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * Returns the structured {@link ErrorContext} carrying the error code,
     * message, timestamp, and any key/value details attached by the thrower.
     */
    public ErrorContext getErrorContext() {
        return errorContext;
    }
}
