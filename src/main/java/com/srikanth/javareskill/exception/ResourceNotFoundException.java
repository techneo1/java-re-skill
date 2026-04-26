package com.srikanth.javareskill.exception;

/**
 * Intermediate category for exceptions that signal a requested resource could
 * not be found (e.g. an employee or department that does not exist in the store).
 */
public abstract class ResourceNotFoundException extends HrException {

    protected ResourceNotFoundException(String message) {
        super(message);
    }

    protected ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

