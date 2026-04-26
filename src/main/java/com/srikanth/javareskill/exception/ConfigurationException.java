package com.srikanth.javareskill.exception;

/**
 * Thrown when application configuration cannot be loaded or parsed.
 * Sits in the {@link HrException} hierarchy so a single top-level handler
 * can catch all application exceptions.
 *
 * <p>Error code: {@link ErrorCode#CONFIGURATION_ERROR} ({@code HR-CFG-001})</p>
 *
 * <pre>
 * HrException
 * └── ConfigurationException
 * </pre>
 */
public final class ConfigurationException extends HrException {

    /** The config key or file path that caused the problem (may be {@code null}). */
    private final String context;

    /**
     * Constructs the exception with a plain message.
     *
     * @param message human-readable description
     */
    public ConfigurationException(String message) {
        super(ErrorCode.CONFIGURATION_ERROR,
                ErrorContext.of(ErrorCode.CONFIGURATION_ERROR, message)
                        .build());
        this.context = null;
    }

    /**
     * Constructs the exception with a context identifier and a message.
     *
     * @param context  the config key or file path that triggered the error
     * @param message  human-readable description
     */
    public ConfigurationException(String context, String message) {
        super(ErrorCode.CONFIGURATION_ERROR,
                ErrorContext.of(ErrorCode.CONFIGURATION_ERROR, "[" + context + "] " + message)
                        .with("configContext", context)
                        .build());
        this.context = context;
    }

    /**
     * Constructs the exception with a context identifier, a message, and an underlying cause.
     *
     * @param context  the config key or file path that triggered the error
     * @param message  human-readable description
     * @param cause    the underlying cause (e.g. {@link java.io.IOException})
     */
    public ConfigurationException(String context, String message, Throwable cause) {
        super(ErrorCode.CONFIGURATION_ERROR,
                ErrorContext.of(ErrorCode.CONFIGURATION_ERROR, "[" + context + "] " + message)
                        .with("configContext", context)
                        .build(),
                cause);
        this.context = context;
    }

    /** Returns the config key or file path that triggered this exception, or {@code null}. */
    public String getContext() {
        return context;
    }
}
