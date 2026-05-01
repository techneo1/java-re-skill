package com.srikanth.javareskill.repository.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Manages database transactions with explicit commit/rollback control.
 *
 * <p>This class provides programmatic transaction boundaries, allowing multiple
 * database operations to be executed atomically within a single transaction.</p>
 *
 * <h2>Transaction Lifecycle</h2>
 * <pre>{@code
 * TransactionManager txManager = new TransactionManager();
 * try {
 *     txManager.begin();
 *     
 *     // Multiple operations within transaction
 *     deptRepo.save(dept);
 *     empRepo.save(emp);
 *     
 *     txManager.commit();  // All succeed or all fail
 * } catch (Exception e) {
 *     txManager.rollback(); // Undo all changes
 *     throw e;
 * } finally {
 *     txManager.close();    // Release connection
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Each {@code TransactionManager} instance is bound to a single {@link Connection}
 * and is <b>not thread-safe</b>. Each thread should use its own instance.</p>
 *
 * <h2>ACID Properties</h2>
 * <ul>
 *   <li><b>Atomicity</b>: All operations succeed or all fail together</li>
 *   <li><b>Consistency</b>: Database constraints are enforced</li>
 *   <li><b>Isolation</b>: Configurable isolation level (default: READ_COMMITTED)</li>
 *   <li><b>Durability</b>: Committed changes are persisted</li>
 * </ul>
 */
public class TransactionManager implements AutoCloseable {

    private Connection connection;
    private boolean transactionActive = false;
    private Integer originalIsolationLevel;

    /**
     * Begins a new transaction.
     *
     * <p>Obtains a connection from the pool, disables auto-commit, and sets
     * the transaction isolation level to READ_COMMITTED.</p>
     *
     * @throws SQLException if the transaction cannot be started
     * @throws IllegalStateException if a transaction is already active
     */
    public void begin() throws SQLException {
        if (transactionActive) {
            throw new IllegalStateException("Transaction already active");
        }

        connection = ConnectionManager.getConnection();
        originalIsolationLevel = connection.getTransactionIsolation();
        
        connection.setAutoCommit(false);
        connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        
        transactionActive = true;
    }

    /**
     * Begins a new transaction with the specified isolation level.
     *
     * @param isolationLevel one of {@link Connection#TRANSACTION_READ_UNCOMMITTED},
     *                       {@link Connection#TRANSACTION_READ_COMMITTED},
     *                       {@link Connection#TRANSACTION_REPEATABLE_READ}, or
     *                       {@link Connection#TRANSACTION_SERIALIZABLE}
     * @throws SQLException if the transaction cannot be started
     * @throws IllegalStateException if a transaction is already active
     */
    public void begin(int isolationLevel) throws SQLException {
        if (transactionActive) {
            throw new IllegalStateException("Transaction already active");
        }

        connection = ConnectionManager.getConnection();
        originalIsolationLevel = connection.getTransactionIsolation();
        
        connection.setAutoCommit(false);
        connection.setTransactionIsolation(isolationLevel);
        
        transactionActive = true;
    }

    /**
     * Commits the current transaction.
     *
     * <p>Makes all changes since {@link #begin()} permanent and visible to
     * other transactions.</p>
     *
     * @throws SQLException if the commit fails
     * @throws IllegalStateException if no transaction is active
     */
    public void commit() throws SQLException {
        ensureTransactionActive();
        
        try {
            connection.commit();
        } finally {
            cleanupTransaction();
        }
    }

    /**
     * Rolls back the current transaction.
     *
     * <p>Discards all changes made since {@link #begin()}, restoring the
     * database to its state before the transaction started.</p>
     *
     * @throws SQLException if the rollback fails
     * @throws IllegalStateException if no transaction is active
     */
    public void rollback() throws SQLException {
        ensureTransactionActive();
        
        try {
            connection.rollback();
        } finally {
            cleanupTransaction();
        }
    }

    /**
     * Creates a savepoint with the given name.
     *
     * <p>Savepoints allow partial rollback within a transaction — you can
     * roll back to a savepoint without rolling back the entire transaction.</p>
     *
     * <pre>{@code
     * txManager.begin();
     * 
     * deptRepo.save(dept);
     * txManager.setSavepoint("after_dept");
     * 
     * try {
     *     empRepo.save(emp);  // Might fail
     * } catch (Exception e) {
     *     txManager.rollbackToSavepoint("after_dept");  // Keep dept, discard emp
     * }
     * 
     * txManager.commit();
     * }</pre>
     *
     * @param savepointName unique name for the savepoint
     * @throws SQLException if the savepoint cannot be created
     * @throws IllegalStateException if no transaction is active
     */
    public void setSavepoint(String savepointName) throws SQLException {
        ensureTransactionActive();
        connection.setSavepoint(savepointName);
    }

    /**
     * Rolls back to the specified savepoint.
     *
     * <p>Undoes all changes made after the savepoint was created, but keeps
     * changes made before it.</p>
     *
     * @param savepointName the savepoint to roll back to
     * @throws SQLException if the rollback fails
     * @throws IllegalStateException if no transaction is active
     */
    public void rollbackToSavepoint(String savepointName) throws SQLException {
        ensureTransactionActive();
        
        var savepoint = connection.setSavepoint(savepointName);
        connection.rollback(savepoint);
        connection.releaseSavepoint(savepoint);
    }

    /**
     * Returns the connection associated with this transaction.
     *
     * <p>This connection should be used by repository methods to participate
     * in the transaction.</p>
     *
     * @return the active connection
     * @throws IllegalStateException if no transaction is active
     */
    public Connection getConnection() {
        if (!transactionActive || connection == null) {
            throw new IllegalStateException("No active transaction");
        }
        return connection;
    }

    /**
     * Returns {@code true} if a transaction is currently active.
     *
     * @return {@code true} if a transaction has been started and not yet
     *         committed or rolled back
     */
    public boolean isTransactionActive() {
        return transactionActive;
    }

    /**
     * Closes this transaction manager and releases the database connection.
     *
     * <p>If a transaction is still active, it is automatically rolled back
     * before the connection is closed.</p>
     *
     * <p>This method is idempotent — calling it multiple times has no effect
     * after the first call.</p>
     */
    @Override
    public void close() {
        if (connection != null) {
            try {
                if (transactionActive) {
                    // Auto-rollback if transaction not explicitly committed
                    connection.rollback();
                }
            } catch (SQLException e) {
                // Log but don't throw — we're cleaning up
                System.err.println("Failed to rollback transaction during cleanup: " + e.getMessage());
            } finally {
                cleanupTransaction();
            }
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void ensureTransactionActive() {
        if (!transactionActive || connection == null) {
            throw new IllegalStateException("No active transaction");
        }
    }

    private void cleanupTransaction() {
        transactionActive = false;
        
        if (connection != null) {
            try {
                // Restore original auto-commit and isolation level
                connection.setAutoCommit(true);
                if (originalIsolationLevel != null) {
                    connection.setTransactionIsolation(originalIsolationLevel);
                }
            } catch (SQLException e) {
                // Log but don't throw
                System.err.println("Failed to restore connection state: " + e.getMessage());
            } finally {
                try {
                    connection.close();  // Return to pool
                } catch (SQLException e) {
                    System.err.println("Failed to close connection: " + e.getMessage());
                }
                connection = null;
                originalIsolationLevel = null;
            }
        }
    }
}

