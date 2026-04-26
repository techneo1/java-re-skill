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
 */
public abstract class HrException extends RuntimeException {

    protected HrException(String message) {
        super(message);
    }

    protected HrException(String message, Throwable cause) {
        super(message, cause);
    }
}

