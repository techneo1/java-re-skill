package com.srikanth.javareskill.dto.request;

import com.srikanth.javareskill.domain.enums.EmployeeStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for {@code PUT /employees/{id}/status}.
 *
 * <p>A focused single-field request that follows the
 * <em>Command Query Separation</em> principle: updating status is a
 * distinct operation from a full employee update.</p>
 */
public class UpdateEmployeeStatusRequest {

    @NotNull(message = "status must not be null")
    private EmployeeStatus status;

    /** No-arg constructor required by Jackson. */
    public UpdateEmployeeStatusRequest() {}

    public UpdateEmployeeStatusRequest(EmployeeStatus status) {
        this.status = status;
    }

    public EmployeeStatus getStatus()                      { return status; }
    public void           setStatus(EmployeeStatus status) { this.status = status; }

    @Override
    public String toString() {
        return "UpdateEmployeeStatusRequest{status=" + status + "}";
    }
}

