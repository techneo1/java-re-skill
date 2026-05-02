package com.srikanth.javareskill.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code POST /departments}.
 *
 * <p>The server auto-generates the department ID (UUID);
 * the caller supplies only name and location.</p>
 */
public class CreateDepartmentRequest {

    @NotBlank(message = "name must not be blank")
    private String name;

    @NotBlank(message = "location must not be blank")
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

