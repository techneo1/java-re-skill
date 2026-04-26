package com.srikanth.javareskill.store;

/**
 * Thrown when an operation targets an employee ID that does not exist in the store.
 */
public class EmployeeNotFoundException extends RuntimeException {

    public EmployeeNotFoundException(String id) {
        super("No employee found with ID: " + id);
    }
}

