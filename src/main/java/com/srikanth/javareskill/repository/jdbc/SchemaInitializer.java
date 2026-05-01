package com.srikanth.javareskill.repository.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Utility class for initializing the database schema.
 *
 * <p>Creates the necessary tables for the HR domain model:
 * {@code departments} and {@code employees}.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * ConnectionManager.initializeDefault();
 * SchemaInitializer.createTables();
 * // ... use DAOs
 * ConnectionManager.shutdown();
 * }</pre>
 */
public final class SchemaInitializer {

    private SchemaInitializer() {}

    /**
     * Creates all required database tables if they don't already exist.
     *
     * <p>Safe to call multiple times — uses {@code CREATE TABLE IF NOT EXISTS}.</p>
     *
     * @throws SQLException if table creation fails
     */
    public static void createTables() throws SQLException {
        try (Connection conn = ConnectionManager.getConnection();
             Statement stmt = conn.createStatement()) {

            // Department table
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS departments (
                    id          VARCHAR(50)  PRIMARY KEY,
                    name        VARCHAR(255) NOT NULL,
                    location    VARCHAR(255) NOT NULL
                )
            """);

            // Employee table
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS employees (
                    id              VARCHAR(50)     PRIMARY KEY,
                    name            VARCHAR(255)    NOT NULL,
                    email           VARCHAR(255)    NOT NULL UNIQUE,
                    department_id   VARCHAR(50)     NOT NULL,
                    role            VARCHAR(50)     NOT NULL,
                    salary          DECIMAL(15, 2)  NOT NULL,
                    status          VARCHAR(20)     NOT NULL,
                    joining_date    DATE            NOT NULL,
                    FOREIGN KEY (department_id) REFERENCES departments(id)
                )
            """);

            // Indexes for common queries
            stmt.executeUpdate("""
                CREATE INDEX IF NOT EXISTS idx_emp_dept 
                ON employees(department_id)
            """);

            stmt.executeUpdate("""
                CREATE INDEX IF NOT EXISTS idx_emp_status 
                ON employees(status)
            """);

            stmt.executeUpdate("""
                CREATE INDEX IF NOT EXISTS idx_emp_role 
                ON employees(role)
            """);

            stmt.executeUpdate("""
                CREATE INDEX IF NOT EXISTS idx_dept_location 
                ON departments(location)
            """);
        }
    }

    /**
     * Drops all tables (useful for testing).
     *
     * @throws SQLException if drop fails
     */
    public static void dropTables() throws SQLException {
        try (Connection conn = ConnectionManager.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("DROP TABLE IF EXISTS employees");
            stmt.executeUpdate("DROP TABLE IF EXISTS departments");
        }
    }
}

