package com.srikanth.javareskill.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Uniform error envelope returned for all non-2xx responses.
 *
 * <p>Using a consistent error shape lets API clients (and tests) parse errors
 * without branching on status codes.  Follows RFC 7807 "Problem Details"
 * conventions loosely.</p>
 *
 * @param timestamp  when the error occurred (server time, ISO-8601)
 * @param status     HTTP status code (e.g. 404, 400)
 * @param error      short HTTP reason phrase (e.g. "Not Found")
 * @param message    human-readable description of the specific error
 * @param path       the request URI that triggered the error
 * @param fieldErrors per-field validation errors; empty for non-400 responses
 */
public record ApiErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, List<String>> fieldErrors
) {
    /**
     * Convenience factory for a single-message error (no field errors).
     */
    public static ApiErrorResponse of(int status, String error, String message, String path) {
        return new ApiErrorResponse(LocalDateTime.now(), status, error, message, path, Map.of());
    }

    /**
     * Convenience factory for a validation-failure response (HTTP 400)
     * that carries per-field error messages.
     *
     * @param fieldErrors map of field name → list of violation messages
     */
    public static ApiErrorResponse validationError(
            String path, Map<String, List<String>> fieldErrors) {
        return new ApiErrorResponse(
                LocalDateTime.now(), 400, "Bad Request",
                "Validation failed — see fieldErrors for details",
                path, fieldErrors);
    }
}

