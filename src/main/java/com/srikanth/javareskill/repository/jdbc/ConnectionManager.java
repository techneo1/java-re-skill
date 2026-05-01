package com.srikanth.javareskill.repository.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Centralized database connection manager using HikariCP connection pooling.
 *
 * <p>Manages a single {@link HikariDataSource} instance throughout the application
 * lifecycle, providing efficient connection pooling and resource management.</p>
 *
 * <h2>Design pattern: Singleton (with explicit lifecycle)</h2>
 * <ul>
 *   <li>{@link #initialize(String, String, String)} must be called once at startup</li>
 *   <li>{@link #getConnection()} returns pooled connections</li>
 *   <li>{@link #shutdown()} releases all resources on application exit</li>
 * </ul>
 *
 * <h2>Default configuration</h2>
 * <pre>
 * JDBC URL:  jdbc:h2:mem:hrdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL
 * Username:  sa
 * Password:  (empty)
 * Pool size: 10 connections
 * </pre>
 *
 * <h2>SOLID principles applied</h2>
 * <ul>
 *   <li><b>S – Single Responsibility</b>: This class is responsible solely for
 *       managing database connections and pooling — no business logic.</li>
 *   <li><b>D – Dependency Inversion</b>: DAO classes depend on standard
 *       {@link Connection} abstraction, not on this manager directly.</li>
 * </ul>
 */
public final class ConnectionManager {

    private static HikariDataSource dataSource;

    // Prevent instantiation
    private ConnectionManager() {}

    /**
     * Initializes the connection pool with the given JDBC configuration.
     *
     * <p>This method must be called exactly once before any call to
     * {@link #getConnection()}.  Subsequent calls throw {@link IllegalStateException}.</p>
     *
     * @param jdbcUrl  JDBC URL (e.g. {@code jdbc:h2:mem:testdb})
     * @param username database username; may be {@code null} if not required
     * @param password database password; may be {@code null} if not required
     * @throws IllegalStateException if already initialized
     */
    public static synchronized void initialize(String jdbcUrl, String username, String password) {
        if (dataSource != null) {
            throw new IllegalStateException("ConnectionManager already initialized");
        }

        Objects.requireNonNull(jdbcUrl, "jdbcUrl must not be null");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);

        // Connection pool tuning
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30_000);  // 30 seconds
        config.setIdleTimeout(600_000);       // 10 minutes
        config.setMaxLifetime(1_800_000);     // 30 minutes

        // Enable automatic connection testing
        config.setConnectionTestQuery("SELECT 1");

        dataSource = new HikariDataSource(config);
    }

    /**
     * Convenience overload that initializes an in-memory H2 database with default credentials.
     *
     * <p>Equivalent to calling:</p>
     * <pre>{@code
     * initialize("jdbc:h2:mem:hrdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL", "sa", "")
     * }</pre>
     */
    public static void initializeDefault() {
        initialize("jdbc:h2:mem:hrdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL", "sa", "");
    }

    /**
     * Obtains a connection from the pool.
     *
     * <p><b>Important:</b> Callers must close the connection in a
     * {@code try-with-resources} block to return it to the pool.</p>
     *
     * @return a pooled database connection
     * @throws SQLException if the connection cannot be obtained
     * @throws IllegalStateException if {@link #initialize} has not been called
     */
    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new IllegalStateException("ConnectionManager not initialized. Call initialize() first.");
        }
        return dataSource.getConnection();
    }

    /**
     * Closes the connection pool and releases all database resources.
     *
     * <p>After calling this method, further calls to {@link #getConnection()}
     * will throw {@link IllegalStateException} until {@link #initialize} is
     * called again.</p>
     */
    public static synchronized void shutdown() {
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }
    }

    /**
     * Returns {@code true} if the connection pool has been initialized and is active.
     *
     * @return {@code true} if ready to provide connections
     */
    public static boolean isInitialized() {
        return dataSource != null && !dataSource.isClosed();
    }
}

