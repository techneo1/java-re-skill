package com.srikanth.javareskill.dto.response;

/**
 * Generic response returned for successful resource creation (HTTP 201).
 *
 * <p>Carries the server-generated ID so the caller can reference the new
 * resource in subsequent requests — avoids the need to parse the
 * {@code Location} header.</p>
 *
 * @param id      the generated resource identifier
 * @param message a human-readable confirmation message
 */
public record CreatedResponse(String id, String message) {

    /**
     * Convenience factory for the common "Employee created" case.
     */
    public static CreatedResponse ofEmployee(String id) {
        return new CreatedResponse(id, "Employee created successfully");
    }

    /**
     * Convenience factory for the common "Department created" case.
     */
    public static CreatedResponse ofDepartment(String id) {
        return new CreatedResponse(id, "Department created successfully");
    }
}

