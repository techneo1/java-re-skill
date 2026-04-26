package com.srikanth.javareskill.exception;

/**
 * Thrown when an attempt is made to register an e-mail address that is already
 * associated with another employee in the store.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * throw new DuplicateEmailException("alice@example.com", "E001");
 * }</pre>
 */
public final class DuplicateEmailException extends BusinessRuleException {

    /** The e-mail address that was found to be a duplicate. */
    private final String email;

    /** The ID of the existing employee who already owns this e-mail address. */
    private final String existingEmployeeId;

    /**
     * Constructs the exception with the duplicate e-mail and the ID of the
     * employee who already owns it.
     *
     * @param email              the duplicate e-mail address
     * @param existingEmployeeId the ID of the existing employee
     */
    public DuplicateEmailException(String email, String existingEmployeeId) {
        super("Email '" + email + "' is already registered to employee ID: " + existingEmployeeId);
        this.email = email;
        this.existingEmployeeId = existingEmployeeId;
    }

    /**
     * Constructs the exception with the duplicate e-mail, the existing employee ID,
     * and an underlying cause.
     *
     * @param email              the duplicate e-mail address
     * @param existingEmployeeId the ID of the existing employee
     * @param cause              the underlying cause
     */
    public DuplicateEmailException(String email, String existingEmployeeId, Throwable cause) {
        super("Email '" + email + "' is already registered to employee ID: " + existingEmployeeId, cause);
        this.email = email;
        this.existingEmployeeId = existingEmployeeId;
    }

    /** Returns the duplicate e-mail address. */
    public String getEmail() {
        return email;
    }

    /** Returns the ID of the employee who already owns the duplicate e-mail address. */
    public String getExistingEmployeeId() {
        return existingEmployeeId;
    }
}

