package com.srikanth.javareskill.exception;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable, structured description of an error that has occurred.
 *
 * <p>{@code ErrorContext} acts as the canonical "error envelope" that
 * callers (API layers, loggers, monitoring hooks) can inspect instead of
 * parsing free-form exception messages.</p>
 *
 * <h2>Structure</h2>
 * <ul>
 *   <li>{@link #errorCode}  – the {@link ErrorCode} constant</li>
 *   <li>{@link #message}    – detailed, human-readable description</li>
 *   <li>{@link #timestamp}  – when the error was recorded</li>
 *   <li>{@link #details}    – optional key/value metadata (e.g. the bad field value)</li>
 * </ul>
 *
 * <h2>Building an instance</h2>
 * <pre>{@code
 * ErrorContext ctx = ErrorContext.of(ErrorCode.DUPLICATE_EMAIL, "Email already in use")
 *         .with("email", "alice@example.com")
 *         .with("existingEmployeeId", "E001");
 * }</pre>
 */
public final class ErrorContext {

    private final ErrorCode         errorCode;
    private final String            message;
    private final Instant           timestamp;
    private final Map<String, Object> details;

    private ErrorContext(Builder builder) {
        this.errorCode  = Objects.requireNonNull(builder.errorCode,  "errorCode");
        this.message    = Objects.requireNonNull(builder.message,    "message");
        this.timestamp  = builder.timestamp != null ? builder.timestamp : Instant.now();
        this.details    = Collections.unmodifiableMap(new LinkedHashMap<>(builder.details));
    }

    // ------------------------------------------------------------------
    // Factory
    // ------------------------------------------------------------------

    /**
     * Creates a new builder pre-populated with the given code and message.
     *
     * @param errorCode the {@link ErrorCode} that classifies this error
     * @param message   detailed, human-readable description
     * @return a {@link Builder} ready for optional detail entries
     */
    public static Builder of(ErrorCode errorCode, String message) {
        return new Builder(errorCode, message);
    }

    /**
     * Convenience factory – no additional details needed.
     *
     * @param errorCode the {@link ErrorCode}
     * @param message   human-readable description
     * @return a fully constructed {@code ErrorContext}
     */
    public static ErrorContext simple(ErrorCode errorCode, String message) {
        return new Builder(errorCode, message).build();
    }

    // ------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------

    /** Returns the {@link ErrorCode} that classifies this error. */
    public ErrorCode getErrorCode() { return errorCode; }

    /** Returns the human-readable error message. */
    public String getMessage()      { return message; }

    /** Returns the instant at which this context was created. */
    public Instant getTimestamp()   { return timestamp; }

    /**
     * Returns an unmodifiable view of the key/value details attached to this error.
     * The map preserves insertion order.
     */
    public Map<String, Object> getDetails() { return details; }

    // ------------------------------------------------------------------
    // Object overrides
    // ------------------------------------------------------------------

    @Override
    public String toString() {
        return "ErrorContext{"
                + "code=" + errorCode.getCode()
                + ", message='" + message + '\''
                + ", timestamp=" + timestamp
                + (details.isEmpty() ? "" : ", details=" + details)
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ErrorContext)) return false;
        ErrorContext that = (ErrorContext) o;
        return errorCode == that.errorCode
                && message.equals(that.message)
                && timestamp.equals(that.timestamp)
                && details.equals(that.details);
    }

    @Override
    public int hashCode() {
        return Objects.hash(errorCode, message, timestamp, details);
    }

    // ------------------------------------------------------------------
    // Builder
    // ------------------------------------------------------------------

    /**
     * Fluent builder for {@link ErrorContext}.
     */
    public static final class Builder {

        private final ErrorCode           errorCode;
        private final String              message;
        private       Instant             timestamp;
        private final Map<String, Object> details = new LinkedHashMap<>();

        private Builder(ErrorCode errorCode, String message) {
            this.errorCode = errorCode;
            this.message   = message;
        }

        /**
         * Attaches a key/value detail pair to the error context.
         *
         * @param key   the detail key (e.g. {@code "email"})
         * @param value the detail value; {@code null} values are stored as-is
         * @return this builder (fluent)
         */
        public Builder with(String key, Object value) {
            details.put(key, value);
            return this;
        }

        /**
         * Overrides the default timestamp ({@link Instant#now()}).
         * Useful in tests to pin a deterministic value.
         *
         * @param timestamp the desired timestamp
         * @return this builder (fluent)
         */
        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        /**
         * Constructs and returns the immutable {@link ErrorContext}.
         *
         * @return a new {@link ErrorContext}
         */
        public ErrorContext build() {
            return new ErrorContext(this);
        }
    }
}

