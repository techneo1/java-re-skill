package com.srikanth.javareskill.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /departments}.
 *
 * <p>The server auto-generates the department ID (UUID);
 * the caller supplies only name and location.</p>
 *
 * <h2>Validation rules</h2>
 * <table border="1">
 *   <tr><th>Field</th><th>Rule</th></tr>
 *   <tr><td>name</td>
 *       <td>Not blank · 2–100 chars · letters, digits, spaces, hyphens, ampersands only</td></tr>
 *   <tr><td>location</td>
 *       <td>Not blank · 2–100 chars · letters, digits, spaces, commas, hyphens only</td></tr>
 * </table>
 */
public class CreateDepartmentRequest {

    /**
     * Department name.
     * <ul>
     *   <li>Must not be blank.</li>
     *   <li>2–100 characters.</li>
     *   <li>Letters, digits, spaces, hyphens and ampersands only
     *       (e.g. "Engineering", "R&D", "Sales - APAC").</li>
     * </ul>
     */
    @NotBlank(message = "name must not be blank")
    @Size(min = 2, max = 100,
          message = "name must be between 2 and 100 characters")
    @Pattern(regexp = "^[\\p{L}\\d &.'-]+$",
             message = "name must contain only letters, digits, spaces, hyphens, ampersands, apostrophes, or dots")
    private String name;

    /**
     * Office or city location.
     * <ul>
     *   <li>Must not be blank.</li>
     *   <li>2–100 characters.</li>
     *   <li>Letters, digits, spaces, commas and hyphens only
     *       (e.g. "New York", "London, UK", "San Francisco - HQ").</li>
     * </ul>
     */
    @NotBlank(message = "location must not be blank")
    @Size(min = 2, max = 100,
          message = "location must be between 2 and 100 characters")
    @Pattern(regexp = "^[\\p{L}\\d ,.-]+$",
             message = "location must contain only letters, digits, spaces, commas, dots, or hyphens")
    private String location;

    /** No-arg constructor required by Jackson. */
    public CreateDepartmentRequest() {}

    public CreateDepartmentRequest(String name, String location) {
        this.name     = name;
        this.location = location;
    }

    public String getName()                { return name; }
    public void   setName(String name)     { this.name = name; }

    public String getLocation()                  { return location; }
    public void   setLocation(String location)   { this.location = location; }

    @Override
    public String toString() {
        return "CreateDepartmentRequest{name='%s', location='%s'}".formatted(name, location);
    }
}
