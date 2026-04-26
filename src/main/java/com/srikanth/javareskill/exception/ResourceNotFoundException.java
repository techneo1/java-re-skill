package com.srikanth.javareskill.exception;

/**
 * Intermediate category for exceptions that signal a requested resource could
 * not be found (e.g. an employee or department that does not exist in the store).
 */
public abstract class ResourceNotFoundException extends HrException {

    protected ResourceNotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    protected ResourceNotFoundException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    protected ResourceNotFoundException(ErrorCode errorCode, ErrorContext errorContext) {
        super(errorCode, errorContext);
    }

    protected ResourceNotFoundException(ErrorCode errorCode, ErrorContext errorContext, Throwable cause) {
        super(errorCode, errorContext, cause);
    }
}
