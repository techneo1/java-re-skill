# Transaction Management - Manual Commit/Rollback

## Overview

Complete implementation of manual transaction management with explicit commit and rollback control for JDBC operations. This provides ACID guarantees and allows multiple database operations to be executed atomically.

---

## Components

### 1. TransactionManager

**Location**: `src/main/java/com/srikanth/javareskill/repository/jdbc/TransactionManager.java`

**Purpose**: Manages database transactions with explicit lifecycle control.

**Key Features**:
- Manual transaction boundaries (begin, commit, rollback)
- Savepoint support for partial rollback
- Configurable isolation levels
- Automatic resource cleanup
- Thread-safe (per instance)

**Usage**:
```java
TransactionManager tx = new TransactionManager();
try {
    tx.begin();
    
    // Multiple operations
    deptRepo.save(dept, tx.getConnection());
    empRepo.save(emp, tx.getConnection());
    
    tx.commit();  // All succeed
} catch (Exception e) {
    tx.rollback(); // or all fail
} finally {
    tx.close();
}
```

### 2. TransactionalJdbcEmployeeRepository

**Location**: `src/main/java/com/srikanth/javareskill/repository/jdbc/TransactionalJdbcEmployeeRepository.java`

**Purpose**: Transaction-aware repository that can participate in managed transactions.

**Key Features**:
- Overloaded methods accepting Connection parameter
- Works standalone (auto-commit) or within transactions
- Backward compatible with existing EmployeeRepository interface

**Usage**:
```java
TransactionalJdbcEmployeeRepository repo = new TransactionalJdbcEmployeeRepository();

// Standalone (auto-commit)
repo.save(employee);

// Within transaction
tx.begin();
repo.save(employee, tx.getConnection());
tx.commit();
```

---

## ACID Properties

### Atomicity
**All operations succeed or all fail together**

```java
tx.begin();
try {
    // These 3 operations are atomic
    deptRepo.save(dept, conn);
    empRepo.save(emp1, conn);
    empRepo.save(emp2, conn);
    
    tx.commit();  // All 3 committed
} catch (Exception e) {
    tx.rollback();  // All 3 rolled back
}
```

### Consistency
**Database constraints are enforced**

```java
tx.begin();
try {
    // Foreign key constraint enforced
    empRepo.save(employee, conn);  // Fails if department doesn't exist
    tx.commit();
} catch (SQLException e) {
    tx.rollback();  // Maintains database consistency
}
```

### Isolation
**Transactions don't interfere with each other**

```java
// Transaction 1
tx1.begin(Connection.TRANSACTION_READ_COMMITTED);
// Changes not visible to other transactions until commit
tx1.commit();

// Transaction 2 (different connection)
tx2.begin(Connection.TRANSACTION_SERIALIZABLE);
// Higher isolation level
tx2.commit();
```

### Durability
**Committed changes are permanent**

```java
tx.begin();
deptRepo.save(dept, conn);
tx.commit();  // Data is now permanently stored
// Survives system crashes, restarts
```

---

## Transaction Lifecycle

### 1. Begin Transaction
```java
TransactionManager tx = new TransactionManager();
tx.begin();  // Default isolation: READ_COMMITTED

// Or with custom isolation
tx.begin(Connection.TRANSACTION_SERIALIZABLE);
```

**What happens**:
- Connection obtained from pool
- Auto-commit disabled
- Isolation level set
- Transaction marked as active

### 2. Execute Operations
```java
Connection conn = tx.getConnection();

// All operations use same connection
deptRepo.save(dept, conn);
empRepo.save(emp, conn);
empRepo.update(another, conn);
```

### 3. Commit or Rollback
```java
// Success path
tx.commit();  // Makes changes permanent

// Error path
tx.rollback();  // Discards all changes
```

### 4. Cleanup
```java
tx.close();  // Always call in finally block
// Returns connection to pool
// Auto-rollback if not committed
```

---

## Savepoints

Savepoints allow partial rollback within a transaction.

### Basic Usage
```java
tx.begin();

deptRepo.save(dept, conn);
tx.setSavepoint("after_dept");  // Mark this point

try {
    empRepo.save(emp, conn);  // Might fail
} catch (Exception e) {
    tx.rollbackToSavepoint("after_dept");  // Keep dept, discard emp
}

tx.commit();  // Commit what survived
```

### Multiple Savepoints
```java
tx.begin();

operation1();
tx.setSavepoint("sp1");

operation2();
tx.setSavepoint("sp2");

operation3();
tx.setSavepoint("sp3");

// Rollback to any savepoint
tx.rollbackToSavepoint("sp1");  // Undo operations 2 and 3

tx.commit();
```

---

## Isolation Levels

### READ_UNCOMMITTED (Level 0)
- Lowest isolation
- Dirty reads possible
- Highest concurrency

```java
tx.begin(Connection.TRANSACTION_READ_UNCOMMITTED);
```

### READ_COMMITTED (Level 1) - Default
- Prevents dirty reads
- Non-repeatable reads possible
- Good balance

```java
tx.begin();  // Default
// or
tx.begin(Connection.TRANSACTION_READ_COMMITTED);
```

### REPEATABLE_READ (Level 2)
- Prevents dirty and non-repeatable reads
- Phantom reads possible
- Lower concurrency

```java
tx.begin(Connection.TRANSACTION_REPEATABLE_READ);
```

### SERIALIZABLE (Level 3)
- Highest isolation
- No dirty, non-repeatable, or phantom reads
- Lowest concurrency

```java
tx.begin(Connection.TRANSACTION_SERIALIZABLE);
```

---

## Error Handling Patterns

### Pattern 1: Try-Catch-Finally
```java
TransactionManager tx = new TransactionManager();
try {
    tx.begin();
    
    // Operations
    deptRepo.save(dept, tx.getConnection());
    empRepo.save(emp, tx.getConnection());
    
    tx.commit();
} catch (Exception e) {
    tx.rollback();
    throw e;  // Re-throw or handle
} finally {
    tx.close();  // Always cleanup
}
```

### Pattern 2: Try-with-Resources
```java
try (TransactionManager tx = new TransactionManager()) {
    tx.begin();
    
    // Operations
    deptRepo.save(dept, tx.getConnection());
    
    tx.commit();
} catch (Exception e) {
    // Transaction auto-rolled back by close()
    throw e;
}
```

### Pattern 3: Savepoint Recovery
```java
tx.begin();

try {
    criticalOperation(tx.getConnection());
    tx.setSavepoint("safe");
    
    try {
        riskyOperation(tx.getConnection());
    } catch (Exception e) {
        tx.rollbackToSavepoint("safe");  // Partial recovery
    }
    
    tx.commit();
} catch (Exception e) {
    tx.rollback();
} finally {
    tx.close();
}
```

---

## Common Use Cases

### Use Case 1: Multi-Table Insert
```java
TransactionManager tx = new TransactionManager();
try {
    tx.begin();
    Connection conn = tx.getConnection();
    
    // Insert parent
    deptRepo.save(department, conn);
    
    // Insert children
    for (Employee emp : employees) {
        empRepo.save(emp, conn);
    }
    
    tx.commit();
} catch (Exception e) {
    tx.rollback();
} finally {
    tx.close();
}
```

### Use Case 2: Atomic Update
```java
tx.begin();
Connection conn = tx.getConnection();

// Read-modify-write atomically
Employee emp = empRepo.findById(id, conn);
emp.setSalary(emp.getSalary().multiply(BigDecimal.valueOf(1.10)));
empRepo.update(emp, conn);

tx.commit();
```

### Use Case 3: Batch Operations
```java
tx.begin();
Connection conn = tx.getConnection();

try (PreparedStatement pstmt = conn.prepareStatement(INSERT_SQL)) {
    for (Department dept : departments) {
        pstmt.setString(1, dept.getId());
        pstmt.setString(2, dept.getName());
        pstmt.setString(3, dept.getLocation());
        pstmt.addBatch();
    }
    pstmt.executeBatch();
}

tx.commit();
```

### Use Case 4: Conditional Commit
```java
tx.begin();
Connection conn = tx.getConnection();

int rowsAffected = performUpdate(conn);

if (rowsAffected > 0) {
    tx.commit();
} else {
    tx.rollback();
}
```

---

## Testing

### Test File
`src/test/java/com/srikanth/javareskill/repository/jdbc/TransactionManagementTest.java`

### Test Coverage
- ✅ Successful commit
- ✅ Explicit rollback
- ✅ Rollback on exception
- ✅ Auto-rollback on close
- ✅ Savepoints
- ✅ Atomic batch operations
- ✅ State management
- ✅ Isolation levels
- ✅ Multiple operations
- ✅ Foreign key constraints

### Running Tests
```bash
mvn test -Dtest=TransactionManagementTest
```

---

## Demo Application

### Demo File
`src/main/java/com/srikanth/javareskill/TransactionDemoApp.java`

### Demonstrations
1. **Successful Transaction** - Multiple operations with commit
2. **Rollback on Error** - Automatic rollback when exception occurs
3. **Savepoints** - Partial rollback using savepoints
4. **Atomicity** - Batch operations succeed or fail together

### Running Demo
```bash
mvn compile exec:java -Dexec.mainClass="com.srikanth.javareskill.TransactionDemoApp"
```

---

## Best Practices

### ✅ DO

1. **Always use try-finally**
   ```java
   TransactionManager tx = new TransactionManager();
   try {
       tx.begin();
       // operations
       tx.commit();
   } finally {
       tx.close();  // Always cleanup
   }
   ```

2. **Rollback on any exception**
   ```java
   try {
       tx.begin();
       operations();
       tx.commit();
   } catch (Exception e) {
       tx.rollback();  // Don't skip this!
       throw e;
   }
   ```

3. **Keep transactions short**
   ```java
   // GOOD: Short transaction
   tx.begin();
   quickDatabaseOp();
   tx.commit();
   ```

4. **Use savepoints for complex operations**
   ```java
   tx.begin();
   criticalOp();
   tx.setSavepoint("safe");
   try {
       riskyOp();
   } catch (Exception e) {
       tx.rollbackToSavepoint("safe");
   }
   tx.commit();
   ```

### ❌ DON'T

1. **Don't forget to close**
   ```java
   // BAD: Connection leak
   tx.begin();
   operations();
   tx.commit();
   // Missing tx.close()!
   ```

2. **Don't hold transactions too long**
   ```java
   // BAD: Long-running transaction
   tx.begin();
   performExpensiveCalculation();  // Keep out of transaction!
   quickDatabaseOp();
   tx.commit();
   ```

3. **Don't ignore exceptions**
   ```java
   // BAD: Swallowing exception
   try {
       tx.begin();
       operations();
       tx.commit();
   } catch (Exception e) {
       // Missing rollback!
   }
   ```

4. **Don't reuse TransactionManager**
   ```java
   // BAD: Reusing after commit
   tx.begin();
   tx.commit();
   tx.begin();  // Don't reuse, create new instance
   ```

---

## Performance Considerations

### Connection Pooling
- TransactionManager uses HikariCP pool
- Connections returned to pool after close()
- No need to manually manage connections

### Transaction Duration
```java
// Minimize time in transaction
tx.begin();

// BAD: External API call in transaction
String result = callExternalAPI();  // Move outside!

// GOOD: Only database operations
performDatabaseUpdate();

tx.commit();
```

### Batch Operations
```java
// Use batch for multiple similar operations
tx.begin();
try (PreparedStatement pstmt = conn.prepareStatement(SQL)) {
    for (Item item : items) {
        // Set parameters
        pstmt.addBatch();
    }
    pstmt.executeBatch();  // Single round-trip
}
tx.commit();
```

### Isolation Level Selection
- READ_COMMITTED: Good default (balance of consistency and concurrency)
- SERIALIZABLE: Use only when necessary (lower concurrency)
- READ_UNCOMMITTED: Avoid (dirty reads)

---

## Troubleshooting

### Problem: Transaction not rolling back
**Solution**: Ensure rollback() called in catch block
```java
try {
    tx.begin();
    operations();
    tx.commit();
} catch (Exception e) {
    tx.rollback();  // Add this!
    throw e;
} finally {
    tx.close();
}
```

### Problem: Connection leaks
**Solution**: Always call close() in finally
```java
TransactionManager tx = new TransactionManager();
try {
    // operations
} finally {
    tx.close();  // Always!
}
```

### Problem: IllegalStateException: No active transaction
**Solution**: Call begin() before other operations
```java
tx.begin();  // Must call first!
tx.commit();
```

### Problem: Deadlocks
**Solution**: Use consistent lock ordering and shorter transactions
```java
// Always lock in same order: Dept → Employee
tx.begin();
updateDepartment();
updateEmployee();
tx.commit();
```

---

## Summary

### Files Created
1. **TransactionManager.java** - Transaction lifecycle management
2. **TransactionalJdbcEmployeeRepository.java** - Transaction-aware repository
3. **TransactionDemoApp.java** - Comprehensive demonstrations
4. **TransactionManagementTest.java** - 13 unit tests

### Features
✅ Manual commit/rollback control  
✅ Savepoint support  
✅ Configurable isolation levels  
✅ Automatic resource cleanup  
✅ ACID guarantees  
✅ Error handling patterns  
✅ Comprehensive tests  
✅ Demo application  

### Status
**✅ PRODUCTION READY**

Transaction management is fully implemented, tested, and documented. It provides enterprise-grade transaction control for JDBC operations with ACID guarantees.

