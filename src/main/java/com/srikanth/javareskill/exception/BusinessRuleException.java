package com.srikanth.javareskill.exception;

/**
 * Intermediate category for exceptions that signal a violated business rule
 * (e.g. an invalid salary value or a duplicate e-mail address).
 */
public abstract class BusinessRuleException extends HrException {

    protected BusinessRuleException(String message) {
        super(message);
    }

    protected BusinessRuleException(String message, Throwable cause) {
        super(message, cause);
    }
}

