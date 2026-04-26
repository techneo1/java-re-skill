package com.srikanth.javareskill.exception;

/**
 * Intermediate category for exceptions that signal a violated business rule
 * (e.g. an invalid salary value or a duplicate e-mail address).
 */
public abstract class BusinessRuleException extends HrException {

    protected BusinessRuleException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    protected BusinessRuleException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    protected BusinessRuleException(ErrorCode errorCode, ErrorContext errorContext) {
        super(errorCode, errorContext);
    }

    protected BusinessRuleException(ErrorCode errorCode, ErrorContext errorContext, Throwable cause) {
        super(errorCode, errorContext, cause);
    }
}
