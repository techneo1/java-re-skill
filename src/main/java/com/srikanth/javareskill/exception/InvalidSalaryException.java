package com.srikanth.javareskill.exception;

import java.math.BigDecimal;

/**
 * Thrown when a salary value violates business rules (e.g. negative, null, or
 * exceeds a configured maximum).
 *
 * <p>Error code: {@link ErrorCode#INVALID_SALARY} ({@code HR-BIZ-002})</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * throw new InvalidSalaryException(new BigDecimal("-500"), "Salary must not be negative");
 * }</pre>
 */
public final class InvalidSalaryException extends BusinessRuleException {

    /** The salary value that caused the violation (may be {@code null} if unknown). */
    private final BigDecimal invalidValue;

    /**
     * Constructs the exception with the offending salary and a reason message.
     *
     * @param invalidValue the salary value that failed validation
     * @param reason       human-readable description of why the value is invalid
     */
    public InvalidSalaryException(BigDecimal invalidValue, String reason) {
        super(ErrorCode.INVALID_SALARY,
                ErrorContext.of(ErrorCode.INVALID_SALARY,
                                "Invalid salary " + invalidValue + ": " + reason)
                        .with("invalidValue", invalidValue)
                        .with("reason", reason)
                        .build());
        this.invalidValue = invalidValue;
    }

    /**
     * Constructs the exception with the offending salary, a reason message, and
     * an underlying cause.
     *
     * @param invalidValue the salary value that failed validation
     * @param reason       human-readable description of why the value is invalid
     * @param cause        the underlying cause
     */
    public InvalidSalaryException(BigDecimal invalidValue, String reason, Throwable cause) {
        super(ErrorCode.INVALID_SALARY,
                ErrorContext.of(ErrorCode.INVALID_SALARY,
                                "Invalid salary " + invalidValue + ": " + reason)
                        .with("invalidValue", invalidValue)
                        .with("reason", reason)
                        .build(),
                cause);
        this.invalidValue = invalidValue;
    }

    /** Returns the salary value that triggered this exception (may be {@code null}). */
    public BigDecimal getInvalidValue() {
        return invalidValue;
    }
}
